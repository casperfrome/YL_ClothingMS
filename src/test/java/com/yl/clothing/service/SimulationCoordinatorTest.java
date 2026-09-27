package com.yl.clothing.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulationCoordinatorTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

    private static class Fixture implements AutoCloseable {
        final SimulationBatchService batches = mock(SimulationBatchService.class);
        final AtomicLong nanos = new AtomicLong();
        final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        final SimulationCoordinator simulator = new SimulationCoordinator(batches, CLOCK, nanos::get, scheduler);

        Fixture() {
            when(batches.initialize()).thenReturn(new SimulationBatchService.Preferences(false, false));
            AtomicLong runs = new AtomicLong();
            when(batches.switchRun(nullable(Long.class), nullable(String.class), anyBoolean()))
                    .thenAnswer(call -> call.getArgument(1) == null ? null : runs.incrementAndGet());
            when(batches.generate(anyLong())).thenReturn(new SimulationBatchService.Result(2, 1, false));
            when(batches.generateData(anyLong(), anyBoolean())).thenAnswer(call ->
                    new SimulationBatchService.DataResult(new SimulationBatchService.Result(2, 1, false),
                            call.getArgument(1, Boolean.class) ? 1 : 0));
        }

        void at(long millis) { nanos.set(TimeUnit.MILLISECONDS.toNanos(millis)); simulator.tick(); }
        @Override public void close() { simulator.shutdown(); }
    }

    @Test void firstTicksHaveFixedBoundariesAndMissedSlotsAreSkipped() {
        try (var f = new Fixture()) {
            f.simulator.initialize();
            f.simulator.setDataEnabled(true);
            f.at(0);
            f.at(499);
            verify(f.batches, never()).generateData(anyLong(), anyBoolean());
            f.at(500);
            assertThat(f.simulator.dataStatus().generatedCount()).isEqualTo(2);
            assertThat(f.simulator.dataStatus().generatedUserCount()).isZero();
            f.at(14_999);
            assertThat(f.simulator.dataStatus().generatedUserCount()).isZero();
            f.at(15_000);
            assertThat(f.simulator.dataStatus().generatedUserCount()).isEqualTo(1);
            f.at(15_000);
            f.at(15_499);
            verify(f.batches, times(3)).generateData(anyLong(), anyBoolean());
            f.at(15_500);
            f.at(90_000);
            assertThat(f.simulator.dataStatus().generatedCount()).isEqualTo(10);
            assertThat(f.simulator.dataStatus().generatedUserCount()).isEqualTo(2);
            assertThat(f.simulator.dataStatus().intervalMs()).isEqualTo(500);
            assertThat(f.simulator.dataStatus().userIntervalMs()).isEqualTo(15_000);
            verify(f.batches, times(2)).generateData(anyLong(), eq(true));
            verify(f.batches, times(3)).generateData(anyLong(), eq(false));
        }
    }

    @Test void aSlowBatchDoesNotCauseAnImmediateCatchUpBurst() {
        try (var f = new Fixture()) {
            when(f.batches.generate(anyLong())).thenAnswer(call -> {
                f.nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(2_500));
                return new SimulationBatchService.Result(1, 0, false);
            });
            f.simulator.setOrderEnabled(true);
            f.at(500);
            f.simulator.tick();
            verify(f.batches, times(1)).generate(anyLong());
            assertThat(f.simulator.orderStatus().generatedCount()).isEqualTo(1);
            f.at(3_500);
            verify(f.batches, times(2)).generate(anyLong());
        }
    }

    @Test void failedBatchIsReportedAndTheNextFixedSlotRecovers() {
        try (var f = new Fixture()) {
            when(f.batches.generate(anyLong()))
                    .thenThrow(new DataAccessResourceFailureException("injected database failure"))
                    .thenReturn(new SimulationBatchService.Result(2, 1, false));
            f.simulator.setOrderEnabled(true);
            f.at(500);
            assertThat(f.simulator.orderStatus().state()).isEqualTo("ERROR");
            assertThat(f.simulator.orderStatus().generatedCount()).isZero();
            assertThat(f.simulator.orderStatus().error()).isNotBlank();
            f.at(999);
            verify(f.batches, times(1)).generate(anyLong());
            f.at(1_000);
            assertThat(f.simulator.orderStatus().state()).isEqualTo("RUNNING");
            assertThat(f.simulator.orderStatus().generatedCount()).isEqualTo(2);
            assertThat(f.simulator.orderStatus().updatedCount()).isEqualTo(1);
            assertThat(f.simulator.orderStatus().lastTickAt()).endsWith("+08:00");
            assertThat(f.simulator.orderStatus().error()).isNull();
        }
    }

    @Test void waitingForProductsStillCountsUsersAndAdvancesExistingOrders() {
        try (var f = new Fixture()) {
            when(f.batches.generateData(anyLong(), eq(true))).thenReturn(
                    new SimulationBatchService.DataResult(new SimulationBatchService.Result(0, 3, true), 2));
            f.simulator.setDataEnabled(true);
            f.at(15_000);
            assertThat(f.simulator.dataStatus().state()).isEqualTo("WAITING");
            assertThat(f.simulator.dataStatus().generatedCount()).isZero();
            assertThat(f.simulator.dataStatus().generatedUserCount()).isEqualTo(2);
            assertThat(f.simulator.dataStatus().updatedCount()).isEqualTo(3);
            f.at(15_500);
            assertThat(f.simulator.dataStatus().state()).isEqualTo("RUNNING");
            assertThat(f.simulator.dataStatus().generatedCount()).isEqualTo(2);
        }
    }

    @Test void duplicateRequestsDoNotClearCountsOrRestartTheFirstTickDeadline() {
        try (var f = new Fixture()) {
            f.simulator.initialize();
            f.simulator.setOrderEnabled(false);
            f.simulator.setOrderEnabled(true);
            f.nanos.set(TimeUnit.MILLISECONDS.toNanos(400));
            f.simulator.setOrderEnabled(true);
            f.simulator.setDataEnabled(false);
            f.at(500);
            assertThat(f.simulator.orderStatus().generatedCount()).isEqualTo(2);
            f.simulator.setOrderEnabled(true);
            assertThat(f.simulator.orderStatus().generatedCount()).isEqualTo(2);
            verify(f.batches, times(1)).switchRun(nullable(Long.class), nullable(String.class), anyBoolean());
            f.simulator.setDataEnabled(true);
            assertThat(f.simulator.orderStatus().enabled()).isFalse();
            assertThat(f.simulator.dataStatus().enabled()).isTrue();
            assertThat(f.simulator.dataStatus().generatedCount()).isZero();
            f.at(999);
            verify(f.batches, times(1)).generate(anyLong());
            verify(f.batches, never()).generateData(anyLong(), anyBoolean());
            f.at(1_000);
            assertThat(f.simulator.dataStatus().generatedCount()).isEqualTo(2);
            verify(f.batches, times(2)).switchRun(nullable(Long.class), nullable(String.class), anyBoolean());
        }
    }

    @Test void failedCleanupPreservesTheActiveModeAndItsCounts() {
        try (var f = new Fixture()) {
            f.simulator.setDataEnabled(true);
            f.at(15_000);
            when(f.batches.switchRun(nullable(Long.class), nullable(String.class), anyBoolean()))
                    .thenThrow(new DataAccessResourceFailureException("injected clear failure"));
            assertThatThrownBy(() -> f.simulator.setOrderEnabled(true)).isInstanceOf(DataAccessResourceFailureException.class);
            assertThatThrownBy(() -> f.simulator.setDataEnabled(false)).isInstanceOf(DataAccessResourceFailureException.class);
            assertThat(f.simulator.dataStatus().enabled()).isTrue();
            assertThat(f.simulator.orderStatus().enabled()).isFalse();
            assertThat(f.simulator.dataStatus().generatedUserCount()).isEqualTo(1);
            assertThat(f.simulator.dataStatus().generatedCount()).isEqualTo(2);
            doReturn(2L).when(f.batches).switchRun(nullable(Long.class), nullable(String.class), anyBoolean());
        }
    }

    @Test void preferencesLoadIndependentlyAndRunningChangesPreserveCountsAndDeadlines() {
        try (var f = new Fixture()) {
            when(f.batches.initialize()).thenReturn(new SimulationBatchService.Preferences(true, false));
            f.simulator.initialize();
            assertThat(f.simulator.orderStatus().retainData()).isTrue();
            assertThat(f.simulator.dataStatus().retainData()).isFalse();
            assertThat(f.simulator.orderStatus().enabled()).isFalse();
            f.simulator.setOrderEnabled(true);
            f.at(500);
            f.nanos.set(TimeUnit.MILLISECONDS.toNanos(900));
            f.simulator.setOrderRetainData(false);
            assertThat(f.simulator.orderStatus().generatedCount()).isEqualTo(2);
            assertThat(f.simulator.orderStatus().tickCount()).isEqualTo(1);
            f.simulator.setDataRetainData(true);
            assertThat(f.simulator.orderStatus().retainData()).isFalse();
            assertThat(f.simulator.dataStatus().retainData()).isTrue();
            verify(f.batches).setRetainData("ORDERS_ONLY", false, 1L);
            verify(f.batches).setRetainData("FULL_DATA", true, null);
            f.at(1_000);
            assertThat(f.simulator.orderStatus().generatedCount()).isEqualTo(4);
            verify(f.scheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), eq(500L), eq(500L), eq(TimeUnit.MILLISECONDS));
            verify(f.batches, times(1)).switchRun(nullable(Long.class), nullable(String.class), anyBoolean());
        }
    }

    @Test void failedRetentionSaveDoesNotPublishTheNewPreferenceOrResetRunningState() {
        try (var f = new Fixture()) {
            f.simulator.setDataEnabled(true);
            f.at(15_000);
            doThrow(new DataAccessResourceFailureException("injected setting save failure"))
                    .when(f.batches).setRetainData("FULL_DATA", true, 1L);
            assertThatThrownBy(() -> f.simulator.setDataRetainData(true))
                    .isInstanceOf(DataAccessResourceFailureException.class);
            assertThat(f.simulator.dataStatus().retainData()).isFalse();
            assertThat(f.simulator.dataStatus().enabled()).isTrue();
            assertThat(f.simulator.dataStatus().generatedCount()).isEqualTo(2);
            assertThat(f.simulator.dataStatus().generatedUserCount()).isEqualTo(1);
            f.at(15_500);
            assertThat(f.simulator.dataStatus().generatedCount()).isEqualTo(4);
        }
    }

    @Test void retentionSaveFinishesBeforeAConcurrentStopUsesTheRun() throws Exception {
        try (var f = new Fixture(); var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch saveStarted = new CountDownLatch(1), releaseSave = new CountDownLatch(1);
            doAnswer(call -> {
                saveStarted.countDown();
                assertThat(releaseSave.await(3, TimeUnit.SECONDS)).isTrue();
                return null;
            }).when(f.batches).setRetainData("FULL_DATA", true, 1L);
            f.simulator.setDataEnabled(true);
            clearInvocations(f.batches);
            Future<?> save = pool.submit(() -> f.simulator.setDataRetainData(true));
            assertThat(saveStarted.await(3, TimeUnit.SECONDS)).isTrue();
            Future<?> stop = pool.submit(() -> f.simulator.setDataEnabled(false));
            try {
                verify(f.batches, never()).switchRun(nullable(Long.class), nullable(String.class), anyBoolean());
            } finally { releaseSave.countDown(); }
            save.get(3, TimeUnit.SECONDS);
            stop.get(3, TimeUnit.SECONDS);
            var ordered = inOrder(f.batches);
            ordered.verify(f.batches).setRetainData("FULL_DATA", true, 1L);
            ordered.verify(f.batches).switchRun(eq(1L), isNull(), anyBoolean());
            assertThat(f.simulator.dataStatus().retainData()).isTrue();
            assertThat(f.simulator.dataStatus().enabled()).isFalse();
        }
    }

    @Test void concurrentEnablesRemainExclusiveAndRepeatedEnablesAreIdempotent() throws Exception {
        try (var f = new Fixture(); var pool = Executors.newFixedThreadPool(6)) {
            var calls = new ArrayList<Future<?>>();
            for (int i = 0; i < 12; i++) calls.add(pool.submit(() -> f.simulator.setDataEnabled(true)));
            for (var call : calls) call.get(3, TimeUnit.SECONDS);
            verify(f.batches, times(1)).switchRun(nullable(Long.class), nullable(String.class), anyBoolean());
            calls.clear();
            for (int i = 0; i < 24; i++) {
                final boolean data = i % 2 == 0;
                calls.add(pool.submit(() -> {
                    if (data) f.simulator.setDataEnabled(true); else f.simulator.setOrderEnabled(true);
                }));
            }
            for (var call : calls) call.get(3, TimeUnit.SECONDS);
            assertThat(f.simulator.dataStatus().enabled() ^ f.simulator.orderStatus().enabled()).isTrue();
        }
    }

    @Test void aCallbackQueuedBeforeAModeSwitchCannotGenerateForTheNewMode() {
        try (var f = new Fixture()) {
            f.simulator.setOrderEnabled(true);
            f.simulator.setDataEnabled(true);
            var callbacks = org.mockito.ArgumentCaptor.forClass(Runnable.class);
            verify(f.scheduler, times(2)).scheduleAtFixedRate(callbacks.capture(), eq(500L), eq(500L), eq(TimeUnit.MILLISECONDS));
            f.nanos.set(TimeUnit.MILLISECONDS.toNanos(500));
            callbacks.getAllValues().getFirst().run();
            verify(f.batches, never()).generate(anyLong());
            verify(f.batches, never()).generateData(anyLong(), anyBoolean());
            callbacks.getAllValues().getLast().run();
            verify(f.batches, times(1)).generateData(anyLong(), eq(false));
            f.simulator.setDataEnabled(false);
            f.nanos.set(TimeUnit.MILLISECONDS.toNanos(1_000));
            callbacks.getAllValues().getLast().run();
            verify(f.batches, times(1)).generateData(anyLong(), anyBoolean());
        }
    }

    @Test void stopWaitsForInFlightGenerationAndShutdownBlocksFurtherWrites() throws Exception {
        try (var f = new Fixture(); var pool = Executors.newFixedThreadPool(2)) {
            CountDownLatch generationStarted = new CountDownLatch(1), releaseGeneration = new CountDownLatch(1);
            when(f.batches.generate(anyLong())).thenAnswer(call -> {
                generationStarted.countDown();
                assertThat(releaseGeneration.await(3, TimeUnit.SECONDS)).isTrue();
                return new SimulationBatchService.Result(2, 0, false);
            });
            f.simulator.setOrderEnabled(true);
            clearInvocations(f.batches);
            Future<?> generation = pool.submit(() -> f.at(500));
            assertThat(generationStarted.await(3, TimeUnit.SECONDS)).isTrue();
            Future<?> stop = pool.submit(() -> f.simulator.setOrderEnabled(false));
            try { verify(f.batches, never()).switchRun(nullable(Long.class), nullable(String.class), anyBoolean()); } finally { releaseGeneration.countDown(); }
            generation.get(3, TimeUnit.SECONDS);
            stop.get(3, TimeUnit.SECONDS);
            var order = inOrder(f.batches);
            order.verify(f.batches).generate(anyLong());
            order.verify(f.batches).switchRun(eq(1L), isNull(), anyBoolean());
            f.at(60_000);
            verify(f.batches, times(1)).generate(anyLong());
            f.simulator.shutdown();
            assertThatThrownBy(() -> f.simulator.setDataEnabled(true)).hasMessage("服务正在关闭");
            f.at(120_000);
            verify(f.batches, times(1)).generate(anyLong());
        }
    }
}
