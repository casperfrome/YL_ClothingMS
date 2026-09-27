package com.yl.clothing.service;

import com.yl.clothing.entity.Customer;
import com.yl.clothing.entity.SalesOrder;
import com.yl.clothing.entity.SimulationRun;
import com.yl.clothing.enums.OrderStatus;
import com.yl.clothing.mapper.OrderMapper;
import com.yl.clothing.mapper.SimulationMapper;
import com.yl.clothing.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationBatchService {
    public record Result(int created, int updated, boolean blocked) {}
    public record DataResult(Result orders, int usersCreated) {}
    public record Preferences(boolean orderRetainData, boolean dataRetainData) {}
    private static final int MAX_TRANSITIONS_PER_STAGE = 100;
    private static final int PHONE_ATTEMPTS = 20;
    private static final String[] NICKNAMES = {
            "小禾", "星野", "知夏", "晴空", "阿梨", "木棉", "云朵", "小鹿", "向晚", "青柠", "雨桐", "小屿"};
    private final OrderMapper mapper;
    private final UserMapper users;
    private final SimulationMapper runs;
    private final Clock clock;
    private final RandomGenerator random;

    @Autowired
    public SimulationBatchService(OrderMapper mapper, UserMapper users, SimulationMapper runs) {
        this(mapper, users, runs, Clock.system(ZoneId.of("Asia/Shanghai")), RandomGenerator.getDefault());
    }

    public SimulationBatchService(OrderMapper mapper, UserMapper users, SimulationMapper runs, Clock clock, RandomGenerator random) {
        this.mapper = mapper;
        this.users = users;
        this.runs = runs;
        this.clock = clock;
        this.random = random;
    }

    @Transactional
    public Preferences initialize() {
        clearRuns(runs.disposableRunIds());
        runs.endRetainedRuns(LocalDateTime.now(clock));
        return new Preferences(preference("ORDERS_ONLY"), preference("FULL_DATA"));
    }

    @Transactional
    public Long switchRun(Long currentRunId, String nextMode, boolean nextRetainData) {
        if (nextMode != null) validateMode(nextMode);
        finishRun(currentRunId);
        if (nextMode == null) return null;
        SimulationRun run = new SimulationRun();
        run.setMode(nextMode);
        run.setRetainData(nextRetainData);
        run.setStartedAt(LocalDateTime.now(clock));
        runs.insertRun(run);
        return run.getId();
    }

    @Transactional
    public void setRetainData(String mode, boolean retainData, Long activeRunId) {
        validateMode(mode);
        if (runs.updateSetting(mode, retainData) != 1) throw new IllegalStateException("模拟器保留设置不存在");
        if (activeRunId != null && runs.updateRunRetainData(activeRunId, retainData) != 1) {
            throw new IllegalStateException("当前模拟轮次不存在或已结束");
        }
    }

    @Transactional
    public void shutdown(Long activeRunId) {
        finishRun(activeRunId);
    }

    private boolean preference(String mode) {
        Boolean value = runs.retainData(mode);
        if (value == null) throw new IllegalStateException("模拟器保留设置不存在");
        return value;
    }

    private static void validateMode(String mode) {
        if (!"ORDERS_ONLY".equals(mode) && !"FULL_DATA".equals(mode)) {
            throw new IllegalArgumentException("未知模拟器模式");
        }
    }

    private void finishRun(Long runId) {
        if (runId == null) return;
        Boolean retained = runs.runRetainData(runId);
        if (retained == null) throw new IllegalStateException("当前模拟轮次不存在");
        if (retained) runs.endRun(runId, LocalDateTime.now(clock));
        else clearRuns(List.of(runId));
    }

    private void clearRuns(List<Long> runIds) {
        // Delete all discarded orders before their users, including legacy residual data.
        for (long runId : runIds) mapper.clearRun(runId);
        for (long runId : runIds) users.clearSimulationChannels(runId);
        for (long runId : runIds) users.clearSimulationUsers(runId);
        for (long runId : runIds) runs.deleteRun(runId);
    }

    @Transactional
    public Result generate(long runId) {
        return generateOrders(runId, LocalDateTime.now(clock));
    }

    @Transactional
    public DataResult generateData(long runId, boolean createUsers) {
        LocalDateTime now = LocalDateTime.now(clock);
        int createdUsers = createUsers ? generateUsers(runId, now) : 0;
        return new DataResult(generateOrders(runId, now), createdUsers);
    }

    private Result generateOrders(long runId, LocalDateTime now) {
        // Process older stages first so no order advances twice within this transaction.
        int updated = advance(runId, OrderStatus.SHIPPED, now.minusSeconds(90), now)
                + advance(runId, OrderStatus.PENDING_SHIPMENT, now.minusSeconds(30), now)
                + advance(runId, OrderStatus.PENDING_PAYMENT, now.minusSeconds(10), now);
        int requested = random.nextInt(1, 4);
        int created = 0;
        for (int i = 0; i < requested; i++) {
            var candidate = mapper.candidate(runId);
            if (candidate == null) break;
            int quantity = random.nextInt(1, 4);
            SalesOrder order = new SalesOrder();
            order.setSimulationRunId(runId);
            order.setOrderNo("YL" + UUID.randomUUID().toString().replace("-", ""));
            order.setListingId(candidate.getListingId());
            order.setUserId(candidate.getUserId());
            order.setChannelId(candidate.getChannelId());
            order.setChannelName(candidate.getChannelName());
            order.setProductCode(candidate.getProductCode());
            order.setProductName(candidate.getProductName());
            order.setColorName(candidate.getColorName());
            order.setHex1(candidate.getHex1());
            order.setHex2(candidate.getHex2());
            order.setSize(candidate.getSize());
            order.setUnitPrice(candidate.getUnitPrice());
            order.setQuantity(quantity);
            order.setTotalPrice(candidate.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            order.setStatus(OrderStatus.PENDING_PAYMENT.name());
            order.setCreatedAt(now);
            order.setUpdatedAt(now);
            mapper.insert(order);
            created++;
        }
        return new Result(created, updated, created == 0);
    }

    private int advance(long runId, OrderStatus from, LocalDateTime cutoff, LocalDateTime now) {
        int updated = 0;
        for (var order : mapper.dueOrders(runId, from.name(), cutoff, MAX_TRANSITIONS_PER_STAGE)) {
            updated += mapper.updateStatus(order.getId(), from.name(), from.next(random).name(), now);
        }
        return updated;
    }

    private int generateUsers(long runId, LocalDateTime now) {
        List<Long> channels = users.channelIds();
        if (channels.isEmpty()) return 0;
        int requested = random.nextInt(1, 3);
        for (int i = 0; i < requested; i++) {
            Customer customer = insertCustomer(runId, now);
            var available = new ArrayList<>(channels);
            int count = random.nextInt(1, Math.min(4, available.size()) + 1);
            for (int j = 0; j < count; j++) {
                long channelId = available.remove(random.nextInt(available.size()));
                users.addChannel(customer.getId(), channelId);
            }
        }
        return requested;
    }

    private Customer insertCustomer(long runId, LocalDateTime now) {
        for (int attempt = 0; attempt < PHONE_ATTEMPTS; attempt++) {
            String phone = "199" + String.format(Locale.ROOT, "%08d", random.nextInt(100_000_000));
            if (users.phoneExists(phone)) continue;
            Customer customer = new Customer();
            customer.setSimulationRunId(runId);
            customer.setPhone(phone);
            customer.setNickname(NICKNAMES[random.nextInt(NICKNAMES.length)] + random.nextInt(1000));
            int gender = random.nextInt(100);
            customer.setGender(gender < 45 ? "FEMALE" : gender < 85 ? "MALE" : "UNKNOWN");
            customer.setBirthday(LocalDate.now(clock).minusYears(18 + random.nextInt(43))
                    .minusDays(random.nextInt(365)));
            customer.setCreatedAt(now);
            try {
                users.insertSimulated(customer);
                return customer;
            } catch (DuplicateKeyException collision) {
                // A concurrent profile edit may claim the number after the existence check.
                if (attempt == PHONE_ATTEMPTS - 1) throw collision;
            }
        }
        throw new IllegalStateException("无法分配唯一模拟手机号，将在后续批次重试");
    }
}
