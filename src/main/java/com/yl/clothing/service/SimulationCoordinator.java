package com.yl.clothing.service;

import com.yl.clothing.exception.BusinessException;
import com.yl.clothing.vo.DataSimulatorStatusVO;
import com.yl.clothing.vo.SimulatorStatusVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/** One monitor owns both modes, their transactions, and all cleanup. */
@Service
@DependsOn("flywayInitializer")
public class SimulationCoordinator {
    private static final Logger log = LoggerFactory.getLogger(SimulationCoordinator.class);
    public static final int ORDER_INTERVAL_MS = 500;
    public static final int USER_INTERVAL_MS = 15_000;
    private static final long ORDER_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(ORDER_INTERVAL_MS);
    private static final long USER_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(USER_INTERVAL_MS);
    private enum Mode { STOPPED, ORDERS_ONLY, FULL_DATA }

    private final SimulationBatchService batches;
    private final Clock clock;
    private final LongSupplier nanoTime;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> task;
    private Mode mode = Mode.STOPPED;
    private Long runId;
    private boolean orderRetainData;
    private boolean dataRetainData;
    private boolean closed;
    private long generation;
    private long nextOrderAt;
    private long nextUserAt;
    private long generatedCount;
    private long generatedUserCount;
    private long updatedCount;
    private long tickCount;
    private String state = "STOPPED";
    private String error;
    private String blockedReason;
    private String lastTickAt;

    @Autowired
    public SimulationCoordinator(SimulationBatchService batches) {
        this(batches, Clock.system(ZoneId.of("Asia/Shanghai")), System::nanoTime,
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "yl-data-simulator");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    public SimulationCoordinator(SimulationBatchService batches, Clock clock, LongSupplier nanoTime,
                                 ScheduledExecutorService executor) {
        this.batches = batches;
        this.clock = clock;
        this.nanoTime = nanoTime;
        this.executor = executor;
    }

    @PostConstruct
    public synchronized void initialize() {
        var preferences = batches.initialize();
        orderRetainData = preferences.orderRetainData();
        dataRetainData = preferences.dataRetainData();
        generation++;
    }

    public synchronized SimulatorStatusVO orderStatus() {
        boolean active = mode == Mode.ORDERS_ONLY;
        return new SimulatorStatusVO(active, orderRetainData, active ? state : "STOPPED",
                active ? generatedCount : 0, active ? updatedCount : 0, active ? tickCount : 0,
                ORDER_INTERVAL_MS, active ? lastTickAt : null,
                active ? blockedReason : null, active ? error : null);
    }

    public synchronized DataSimulatorStatusVO dataStatus() {
        boolean active = mode == Mode.FULL_DATA;
        return new DataSimulatorStatusVO(active, dataRetainData, active ? state : "STOPPED",
                active ? generatedCount : 0, active ? updatedCount : 0, active ? tickCount : 0,
                ORDER_INTERVAL_MS, active ? lastTickAt : null,
                active ? blockedReason : null, active ? error : null,
                active ? generatedUserCount : 0, USER_INTERVAL_MS);
    }

    public synchronized SimulatorStatusVO setOrderEnabled(boolean enabled) {
        switchMode(Mode.ORDERS_ONLY, enabled);
        return orderStatus();
    }

    public synchronized DataSimulatorStatusVO setDataEnabled(boolean enabled) {
        switchMode(Mode.FULL_DATA, enabled);
        return dataStatus();
    }

    public synchronized SimulatorStatusVO setOrderRetainData(boolean retainData) {
        setRetention(Mode.ORDERS_ONLY, retainData);
        return orderStatus();
    }

    public synchronized DataSimulatorStatusVO setDataRetainData(boolean retainData) {
        setRetention(Mode.FULL_DATA, retainData);
        return dataStatus();
    }

    private void setRetention(Mode requested, boolean retainData) {
        if (closed) throw new BusinessException(409, "服务正在关闭");
        if (retention(requested) == retainData) return;
        batches.setRetainData(requested.name(), retainData, mode == requested ? runId : null);
        if (requested == Mode.ORDERS_ONLY) orderRetainData = retainData;
        else dataRetainData = retainData;
    }

    private boolean retention(Mode requested) {
        return requested == Mode.ORDERS_ONLY ? orderRetainData : dataRetainData;
    }

    private void switchMode(Mode requested, boolean enabled) {
        if (closed) throw new BusinessException(409, "服务正在关闭");
        if ((mode == requested) == enabled) return;
        // Finish and start runs in one transaction before changing task/state.
        Long nextRunId = batches.switchRun(runId, enabled ? requested.name() : null, enabled && retention(requested));
        generation++;
        if (task != null) {
            task.cancel(false);
            task = null;
        }
        mode = enabled ? requested : Mode.STOPPED;
        runId = nextRunId;
        generatedCount = generatedUserCount = updatedCount = tickCount = 0;
        lastTickAt = error = blockedReason = null;
        state = enabled ? "RUNNING" : "STOPPED";
        if (enabled) {
            long now = nanoTime.getAsLong();
            nextOrderAt = now + ORDER_INTERVAL_NANOS;
            nextUserAt = now + USER_INTERVAL_NANOS;
            long scheduledGeneration = generation;
            task = executor.scheduleAtFixedRate(() -> tick(scheduledGeneration), ORDER_INTERVAL_MS, ORDER_INTERVAL_MS,
                    TimeUnit.MILLISECONDS);
        }
    }

    synchronized void tick() {
        tick(generation);
    }

    private synchronized void tick(long scheduledGeneration) {
        if (mode == Mode.STOPPED || closed || generation != scheduledGeneration) return;
        long now = nanoTime.getAsLong();
        if (now - nextOrderAt < 0) return;
        // Skip elapsed slots: a slow/failed batch must not cause a catch-up burst.
        nextOrderAt = nextSlot(nextOrderAt, now, ORDER_INTERVAL_NANOS);
        boolean createUsers = mode == Mode.FULL_DATA && now - nextUserAt >= 0;
        if (createUsers) nextUserAt = nextSlot(nextUserAt, now, USER_INTERVAL_NANOS);
        try {
            SimulationBatchService.Result result;
            int users = 0;
            if (mode == Mode.FULL_DATA) {
                var data = batches.generateData(runId, createUsers);
                result = data.orders();
                users = data.usersCreated();
            } else {
                result = batches.generate(runId);
            }
            generatedCount += result.created();
            generatedUserCount += users;
            updatedCount += result.updated();
            tickCount++;
            blockedReason = result.blocked() ? "暂无可生成订单的商品或用户组合" : null;
            state = result.blocked() ? "WAITING" : "RUNNING";
            error = null;
            lastTickAt = OffsetDateTime.now(clock).withOffsetSameInstant(java.time.ZoneOffset.ofHours(8)).toString();
        } catch (Exception exception) {
            state = "ERROR";
            blockedReason = null;
            error = "模拟数据生成失败，正在重试，请检查数据库连接";
            log.error("模拟数据批次失败", exception);
        } finally {
            long finished = nanoTime.getAsLong();
            if (finished - nextOrderAt >= 0) nextOrderAt = nextSlot(nextOrderAt, finished, ORDER_INTERVAL_NANOS);
            if (mode == Mode.FULL_DATA && finished - nextUserAt >= 0) {
                nextUserAt = nextSlot(nextUserAt, finished, USER_INTERVAL_NANOS);
            }
        }
    }

    private static long nextSlot(long due, long now, long interval) {
        return due + ((now - due) / interval + 1) * interval;
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (closed) return;
        closed = true;
        generation++;
        mode = Mode.STOPPED;
        if (task != null) task.cancel(false);
        executor.shutdown();
        try {
            batches.shutdown(runId);
            runId = null;
            state = "STOPPED";
        } catch (Exception exception) {
            log.error("退出清理失败，下次启动将再次清理", exception);
        }
    }
}
