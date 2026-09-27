package com.yl.clothing.service;

import com.yl.clothing.SimulationTestData;
import com.yl.clothing.mapper.OrderMapper;
import com.yl.clothing.mapper.SimulationMapper;
import com.yl.clothing.mapper.UserMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SimulationPersistenceTest {
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;
    @Autowired OrderMapper orders;
    @Autowired UserMapper users;
    @Autowired SimulationMapper simulations;
    private long runId;
    @Autowired SimulationBatchService batches;
    @Autowired SimulationCoordinator coordinator;
    @Autowired PlatformTransactionManager transactions;
    @Autowired org.mybatis.spring.SqlSessionTemplate sqlSession;

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-09-26T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneId.of("Asia/Shanghai"); }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant, zone); }
        @Override public Instant instant() { return instant; }
        void advance(long millis) { instant = instant.plusMillis(millis); }
    }

    private static class MinimumRandom extends Random {
        @Override public int nextInt(int bound) { return 0; }
        @Override public int nextInt(int origin, int bound) { return origin; }
        @Override public double nextDouble() { return 0; }
    }

    @BeforeEach void prepare() {
        assertTestDatabase();
        SimulationTestData.reset(db, coordinator);
        runId = batches.switchRun(null, "FULL_DATA", false);
    }

    @AfterEach void cleanup() {
        assertTestDatabase();
        SimulationTestData.reset(db, coordinator);
    }

    private void assertTestDatabase() {
        assertThat(db.queryForObject("SELECT DATABASE()", String.class)).isEqualToIgnoringCase("YL_260924_test");
    }

    private long count(String table) { return db.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }

    private SimulationBatchService transactionalBatch(UserMapper mapper, Clock clock, Random random) {
        return transactionalBatch(orders, mapper, simulations, clock, random);
    }

    private SimulationBatchService transactionalBatch(OrderMapper orderMapper, UserMapper userMapper,
                                                       SimulationMapper simulationMapper, Clock clock, Random random) {
        ProxyFactory proxy = new ProxyFactory(new SimulationBatchService(orderMapper, userMapper, simulationMapper, clock, random));
        proxy.setProxyTargetClass(true);
        TransactionInterceptor advice = new TransactionInterceptor();
        advice.setTransactionManager(transactions);
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        proxy.addAdvice(advice);
        return (SimulationBatchService) proxy.getProxy();
    }

    private long insertOrder(String status, LocalDateTime changedAt) {
        String number = "TEST-" + UUID.randomUUID();
        db.update("""
                INSERT INTO sales_order(order_no,user_id,listing_id,channel_id,product_code,product_name,
                    color_name,hex1,size,channel_name,unit_price,quantity,total_price,status,created_at,updated_at,simulation_run_id)
                VALUES(?,1,1,1,'YL-2601','状态测试','云白','#FFFFFF','S','抖音',12.30,2,24.60,?,?,?,?)
                """, number, status, changedAt, changedAt, runId);
        return db.queryForObject("SELECT id FROM sales_order WHERE order_no=?", Long.class, number);
    }

    private String orderStatus(long id) { return db.queryForObject("SELECT status FROM sales_order WHERE id=?", String.class, id); }

    @ParameterizedTest
    @CsvSource({"PENDING_PAYMENT,PENDING_SHIPMENT,10000", "PENDING_SHIPMENT,SHIPPED,30000", "SHIPPED,COMPLETED,90000"})
    @Transactional
    void transitionOccursAtTheExactStateAgeBoundary(String initial, String expected, long delay) {
        MutableClock clock = new MutableClock();
        SimulationBatchService service = new SimulationBatchService(orders, users, simulations, clock, new MinimumRandom());
        long id = insertOrder(initial, LocalDateTime.now(clock));
        db.update("UPDATE listing SET on_sale=0");
        clock.advance(delay - 1);
        assertThat(service.generate(runId).updated()).isZero();
        assertThat(orderStatus(id)).isEqualTo(initial);
        clock.advance(1);
        var result = service.generate(runId);
        assertThat(result.created()).isZero();
        assertThat(result.blocked()).isTrue();
        assertThat(result.updated()).isEqualTo(1);
        assertThat(orderStatus(id)).isEqualTo(expected);
        assertThat(db.queryForObject("SELECT updated_at FROM sales_order WHERE id=?", LocalDateTime.class, id))
                .isEqualTo(LocalDateTime.now(clock));
    }

    @Test @Transactional
    void anOverdueOrderOnlyAdvancesOneStageAndTerminalStatesRemainStable() {
        MutableClock clock = new MutableClock();
        var service = new SimulationBatchService(orders, users, simulations, clock, new MinimumRandom());
        LocalDateTime old = LocalDateTime.now(clock).minusHours(1);
        long pending = insertOrder("PENDING_PAYMENT", old);
        var terminal = new LinkedHashMap<Long, String>();
        for (String state : List.of("COMPLETED", "CANCELLED", "REFUNDED")) terminal.put(insertOrder(state, old), state);
        db.update("UPDATE listing SET on_sale=0");
        assertThat(service.generate(runId).updated()).isEqualTo(1);
        assertThat(orderStatus(pending)).isEqualTo("PENDING_SHIPMENT");
        assertThat(service.generate(runId).updated()).isZero();
        clock.advance(29_999);
        assertThat(service.generate(runId).updated()).isZero();
        clock.advance(1);
        assertThat(service.generate(runId).updated()).isEqualTo(1);
        assertThat(orderStatus(pending)).isEqualTo("SHIPPED");
        clock.advance(90_000);
        assertThat(service.generate(runId).updated()).isEqualTo(1);
        assertThat(orderStatus(pending)).isEqualTo("COMPLETED");
        clock.advance(1_000_000);
        assertThat(service.generate(runId).updated()).isZero();
        terminal.forEach((id, state) -> assertThat(orderStatus(id)).isEqualTo(state));
    }

    @Test @Transactional
    void transitionsAreLimitedToOneHundredPerStageWithOldestFirst() {
        MutableClock clock = new MutableClock();
        var service = new SimulationBatchService(orders, users, simulations, clock, new MinimumRandom());
        var remaining = new LinkedHashMap<Long, String>();
        for (String state : List.of("PENDING_PAYMENT", "PENDING_SHIPMENT", "SHIPPED")) {
            for (int i = 0; i < 101; i++) {
                long id = insertOrder(state, LocalDateTime.now(clock).minusSeconds(1_000 - i));
                if (i == 100) remaining.put(id, state);
            }
        }
        db.update("UPDATE listing SET on_sale=0");
        assertThat(service.generate(runId).updated()).isEqualTo(300);
        remaining.forEach((id, state) -> assertThat(orderStatus(id)).isEqualTo(state));
        assertThat(service.generate(runId).updated()).isEqualTo(3);
        assertThat(service.generate(runId).updated()).isZero();
    }

    @Test @Transactional
    void usersAreGeneratedWhenListingsAreUnavailableAndOrdersResume() {
        List<Long> saleable = db.queryForList("SELECT id FROM listing WHERE on_sale=1", Long.class);
        db.update("UPDATE listing SET on_sale=0");
        long customers = count("customer");
        var result = batches.generateData(runId, true);
        assertThat(result.usersCreated()).isBetween(1, 2);
        assertThat(result.orders().created()).isZero();
        assertThat(result.orders().blocked()).isTrue();
        assertThat(count("customer")).isEqualTo(customers + result.usersCreated());
        db.update("UPDATE listing SET on_sale=1 WHERE id=?", saleable.getFirst());
        // Fixture SQL bypasses MyBatis; discard this test transaction's cached unavailable candidate.
        sqlSession.clearCache();
        assertThat(batches.generateData(runId, false).orders().created()).isBetween(1, 3);
    }

    @Test void generatedUsersHaveUniqueValidProfilesAndChannelsAndSurviveEditsUntilCleanup() throws Exception {
        List<Map<String, Object>> originals = db.queryForList("SELECT * FROM customer ORDER BY id");
        List<Map<String, Object>> originalChannels = db.queryForList("SELECT * FROM user_channel ORDER BY user_id,channel_id");
        int total = 0;
        for (int i = 0; i < 12; i++) {
            var result = batches.generateData(runId, true);
            assertThat(result.usersCreated()).isBetween(1, 2);
            total += result.usersCreated();
        }
        assertThat(count("customer")).isEqualTo(originals.size() + total);
        assertThat(db.queryForObject("SELECT COUNT(DISTINCT phone) FROM customer", Long.class)).isEqualTo(count("customer"));
        var generated = db.queryForList("SELECT id,phone,nickname,gender,birthday FROM customer WHERE simulated=1");
        assertThat(generated).hasSize(total);
        for (var user : generated) {
            assertThat(user.get("phone").toString()).matches("[0-9]{11}");
            assertThat(user.get("nickname").toString()).isNotBlank().hasSizeLessThanOrEqualTo(40);
            assertThat(user.get("gender").toString()).isIn("UNKNOWN", "MALE", "FEMALE");
            assertThat(user.get("birthday")).isNotNull();
            assertThat(db.queryForObject("SELECT COUNT(*) FROM user_channel WHERE user_id=?", Integer.class, user.get("id")))
                    .isBetween(1, 4);
        }
        var edited = generated.getFirst();
        mvc.perform(patch("/api/users/" + edited.get("id")).contentType("application/json").content(
                "{\"phone\":\"" + edited.get("phone") + "\",\"nickname\":\"编辑过的模拟用户\",\"gender\":\"UNKNOWN\",\"birthday\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.simulated").doesNotExist());
        batches.switchRun(runId, null, false);
        assertThat(count("sales_order")).isZero();
        assertThat(db.queryForList("SELECT * FROM customer ORDER BY id")).isEqualTo(originals);
        assertThat(db.queryForList("SELECT * FROM user_channel ORDER BY user_id,channel_id")).isEqualTo(originalChannels);
        mvc.perform(get("/api/users/" + edited.get("id"))).andExpect(status().isNotFound());
    }

    @Test void failureInChannelInsertionRollsBackTheNewCustomerAndOrders() {
        long customers = count("customer"), channels = count("user_channel");
        UserMapper failing = mock(UserMapper.class, delegatesTo(users));
        doThrow(new DataAccessResourceFailureException("injected channel insert failure")).when(failing).addChannel(anyLong(), anyLong());
        var service = transactionalBatch(failing, Clock.system(ZoneId.of("Asia/Shanghai")), new Random(74));
        assertThatThrownBy(() -> service.generateData(runId, true)).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(count("customer")).isEqualTo(customers);
        assertThat(count("user_channel")).isEqualTo(channels);
        assertThat(count("sales_order")).isZero();
    }

    @Test void failureDeletingSimulatedUsersRollsBackOrdersAndChannelDeletion() {
        batches.generateData(runId, true);
        long customers = count("customer"), channels = count("user_channel"), orderCount = count("sales_order");
        UserMapper failing = mock(UserMapper.class, delegatesTo(users));
        doThrow(new DataAccessResourceFailureException("injected user delete failure")).when(failing).clearSimulationUsers(runId);
        var service = transactionalBatch(failing, Clock.system(ZoneId.of("Asia/Shanghai")), new Random(74));
        assertThatThrownBy(() -> service.switchRun(runId, null, false)).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(count("customer")).isEqualTo(customers);
        assertThat(count("user_channel")).isEqualTo(channels);
        assertThat(count("sales_order")).isEqualTo(orderCount).isPositive();
        assertThat(db.queryForObject("SELECT ended_at FROM simulation_run WHERE id=?", LocalDateTime.class, runId)).isNull();
    }

    @Test void retainedDataRunSurvivesALaterOrderRunThatReferencesItsUsers() {
        var deterministic = transactionalBatch(users, Clock.systemUTC(), new MinimumRandom());
        deterministic.generateData(runId, true);
        long retainedRun = runId;
        long retainedUser = db.queryForObject("SELECT id FROM customer WHERE simulation_run_id=?", Long.class, retainedRun);
        List<Map<String, Object>> retainedOrders = db.queryForList("SELECT * FROM sales_order WHERE simulation_run_id=?", retainedRun);
        List<Map<String, Object>> retainedChannels = db.queryForList("SELECT * FROM user_channel WHERE user_id=?", retainedUser);
        batches.setRetainData("FULL_DATA", true, retainedRun);
        long temporaryRun = batches.switchRun(retainedRun, "ORDERS_ONLY", false);
        OrderMapper targetedOrders = mock(OrderMapper.class, delegatesTo(orders));
        doReturn(orders.candidateForChannel(retainedUser, 1L)).when(targetedOrders).candidate(temporaryRun);
        var targeted = transactionalBatch(targetedOrders, users, simulations, Clock.systemUTC(), new MinimumRandom());
        assertThat(targeted.generate(temporaryRun).created()).isEqualTo(1);
        assertThat(db.queryForObject("SELECT user_id FROM sales_order WHERE simulation_run_id=?", Long.class, temporaryRun))
                .isEqualTo(retainedUser);
        batches.switchRun(temporaryRun, null, false);
        assertThat(db.queryForList("SELECT * FROM sales_order WHERE simulation_run_id=?", retainedRun)).isEqualTo(retainedOrders);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order WHERE simulation_run_id=?", Long.class, temporaryRun)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM customer WHERE id=?", Long.class, retainedUser)).isEqualTo(1);
        assertThat(db.queryForList("SELECT * FROM user_channel WHERE user_id=?", retainedUser)).isEqualTo(retainedChannels);
        batches.setRetainData("FULL_DATA", false, null);
        batches.initialize();
        assertThat(db.queryForList("SELECT * FROM sales_order WHERE simulation_run_id=?", retainedRun)).isEqualTo(retainedOrders);
        assertThat(db.queryForObject("SELECT retain_data FROM simulation_run WHERE id=?", Boolean.class, retainedRun)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"FULL_DATA", "ORDERS_ONLY"})
    void changingTheActivePreferenceUsesTheLastSavedValueAtStop(String mode) {
        runId = batches.switchRun(runId, mode, false);
        batches.generateData(runId, "FULL_DATA".equals(mode));
        long ordersBefore = count("sales_order"), usersBefore = count("customer");
        batches.setRetainData(mode, true, runId);
        assertThat(count("sales_order")).isEqualTo(ordersBefore);
        assertThat(count("customer")).isEqualTo(usersBefore);
        batches.setRetainData(mode, false, runId);
        batches.switchRun(runId, null, false);
        assertThat(count("sales_order")).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM customer WHERE simulated=1", Long.class)).isZero();
        runId = batches.switchRun(null, mode, false);
        batches.generateData(runId, "FULL_DATA".equals(mode));
        batches.setRetainData(mode, true, runId);
        ordersBefore = count("sales_order");
        batches.switchRun(runId, null, false);
        assertThat(count("sales_order")).isEqualTo(ordersBefore).isPositive();
        batches.setRetainData(mode, false, null);
        assertThat(db.queryForObject("SELECT retain_data FROM simulation_run WHERE id=?", Boolean.class, runId)).isTrue();
    }

    @Test void retentionSaveFailureRollsBackBothTheSettingAndRunFlag() {
        SimulationMapper failing = mock(SimulationMapper.class, delegatesTo(simulations));
        doThrow(new DataAccessResourceFailureException("injected run setting failure"))
                .when(failing).updateRunRetainData(runId, true);
        var service = transactionalBatch(orders, users, failing, Clock.systemUTC(), new Random(74));
        assertThatThrownBy(() -> service.setRetainData("FULL_DATA", true, runId))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(db.queryForObject("SELECT retain_data FROM simulation_settings WHERE mode='FULL_DATA'", Boolean.class)).isFalse();
        assertThat(db.queryForObject("SELECT retain_data FROM simulation_run WHERE id=?", Boolean.class, runId)).isFalse();
    }

    @Test void failedNextRunCreationRollsBackCleanupOfThePreviousRun() {
        batches.generateData(runId, true);
        long orderCount = count("sales_order"), customerCount = count("customer"), channelCount = count("user_channel");
        SimulationMapper failing = mock(SimulationMapper.class, delegatesTo(simulations));
        doThrow(new DataAccessResourceFailureException("injected run creation failure")).when(failing).insertRun(any());
        var service = transactionalBatch(orders, users, failing, Clock.systemUTC(), new Random(74));
        assertThatThrownBy(() -> service.switchRun(runId, "ORDERS_ONLY", false))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(count("sales_order")).isEqualTo(orderCount);
        assertThat(count("customer")).isEqualTo(customerCount);
        assertThat(count("user_channel")).isEqualTo(channelCount);
        assertThat(db.queryForObject("SELECT ended_at FROM simulation_run WHERE id=?", LocalDateTime.class, runId)).isNull();
    }

    @ParameterizedTest
    @CsvSource({"FULL_DATA", "ORDERS_ONLY"})
    void eitherModeAdvancesRetainedOrdersAndStoppingPausesThem(String mode) {
        MutableClock clock = new MutableClock();
        long retainedOrder = insertOrder("PENDING_PAYMENT", LocalDateTime.now(clock).minusMinutes(10));
        long terminalOrder = insertOrder("COMPLETED", LocalDateTime.now(clock).minusMinutes(10));
        batches.setRetainData("FULL_DATA", true, runId);
        batches.switchRun(runId, null, false);
        var deterministic = transactionalBatch(users, clock, new MinimumRandom());
        AtomicLong nanos = new AtomicLong();
        var local = new SimulationCoordinator(deterministic, clock, nanos::get, mock(ScheduledExecutorService.class));
        try {
            local.initialize();
            local.setDataRetainData(false);
            if ("FULL_DATA".equals(mode)) local.setDataEnabled(true); else local.setOrderEnabled(true);
            nanos.set(TimeUnit.MILLISECONDS.toNanos(500));
            local.tick();
            assertThat(orderStatus(retainedOrder)).isEqualTo("PENDING_SHIPMENT");
            assertThat(orderStatus(terminalOrder)).isEqualTo("COMPLETED");
            if ("FULL_DATA".equals(mode)) {
                assertThat(local.dataStatus().generatedCount()).isEqualTo(1);
                assertThat(local.dataStatus().updatedCount()).isEqualTo(1);
                local.setDataEnabled(false);
            } else {
                assertThat(local.orderStatus().generatedCount()).isEqualTo(1);
                assertThat(local.orderStatus().updatedCount()).isEqualTo(1);
                local.setOrderEnabled(false);
            }
            clock.advance(120_000);
            nanos.set(TimeUnit.MILLISECONDS.toNanos(120_000));
            local.tick();
            assertThat(orderStatus(retainedOrder)).isEqualTo("PENDING_SHIPMENT");
            assertThat(orderStatus(terminalOrder)).isEqualTo("COMPLETED");
        } finally { local.shutdown(); }
    }

    @ParameterizedTest
    @CsvSource({"FULL_DATA,true", "FULL_DATA,false", "ORDERS_ONLY,true", "ORDERS_ONLY,false"})
    void gracefulShutdownAndRestartHonorTheSavedPreference(String mode, boolean retain) {
        batches.setRetainData(mode, retain, null);
        MutableClock clock = new MutableClock();
        AtomicLong nanos = new AtomicLong();
        var local = new SimulationCoordinator(batches, clock, nanos::get, mock(ScheduledExecutorService.class));
        long originalUsers = count("customer");
        try {
            local.initialize();
            if ("FULL_DATA".equals(mode)) local.setDataEnabled(true); else local.setOrderEnabled(true);
            nanos.set(TimeUnit.MILLISECONDS.toNanos(15_000));
            local.tick();
            assertThat(count("sales_order")).isPositive();
        } finally { local.shutdown(); }
        long remainingOrders = count("sales_order"), remainingUsers = count("customer");
        if (retain) {
            assertThat(remainingOrders).isPositive();
            if ("FULL_DATA".equals(mode)) assertThat(remainingUsers).isBetween(originalUsers + 1, originalUsers + 2);
            else assertThat(remainingUsers).isEqualTo(originalUsers);
        } else {
            assertThat(remainingOrders).isZero();
            assertThat(remainingUsers).isEqualTo(originalUsers);
        }
        var restarted = new SimulationCoordinator(batches, clock, nanos::get, mock(ScheduledExecutorService.class));
        try {
            restarted.initialize();
            assertThat(restarted.orderStatus().enabled()).isFalse();
            assertThat(restarted.dataStatus().enabled()).isFalse();
            assertThat("FULL_DATA".equals(mode) ? restarted.dataStatus().retainData() : restarted.orderStatus().retainData())
                    .isEqualTo(retain);
            assertThat(count("sales_order")).isEqualTo(remainingOrders);
            assertThat(count("customer")).isEqualTo(remainingUsers);
        } finally { restarted.shutdown(); }
    }

    @Test void startupRecoversCrashedRunsByTheirSavedFlagsAndLoadsIndependentPreferences() {
        batches.generateData(runId, true);
        batches.setRetainData("FULL_DATA", true, runId);
        long retainedRun = runId;
        long discardedRun = batches.switchRun(null, "ORDERS_ONLY", false);
        batches.generate(discardedRun);
        batches.setRetainData("ORDERS_ONLY", false, discardedRun);
        var restarted = new SimulationCoordinator(batches, Clock.systemUTC(), () -> 0L, mock(ScheduledExecutorService.class));
        try {
            restarted.initialize();
            assertThat(restarted.orderStatus().retainData()).isFalse();
            assertThat(restarted.dataStatus().retainData()).isTrue();
            assertThat(restarted.orderStatus().enabled()).isFalse();
            assertThat(restarted.dataStatus().enabled()).isFalse();
            assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order WHERE simulation_run_id=?", Long.class, discardedRun)).isZero();
            assertThat(db.queryForObject("SELECT COUNT(*) FROM simulation_run WHERE id=?", Long.class, discardedRun)).isZero();
            assertThat(db.queryForObject("SELECT COUNT(*) FROM sales_order WHERE simulation_run_id=?", Long.class, retainedRun)).isPositive();
            assertThat(db.queryForObject("SELECT COUNT(*) FROM customer WHERE simulation_run_id=?", Long.class, retainedRun)).isPositive();
            assertThat(db.queryForObject("SELECT ended_at FROM simulation_run WHERE id=?", LocalDateTime.class, retainedRun)).isNotNull();
            restarted.initialize();
            assertThat(count("simulation_run")).isEqualTo(1);
        } finally { restarted.shutdown(); }
    }

    @Test void simulatorPutEndpointsSwitchModesAndInactiveFalseIsANoOp() throws Exception {
        mvc.perform(put("/api/order-simulator").contentType("application/json").content("{\"enabled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(true));
        mvc.perform(put("/api/data-simulator").contentType("application/json").content("{\"enabled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(true));
        mvc.perform(get("/api/order-simulator")).andExpect(jsonPath("$.data.enabled").value(false));
        mvc.perform(put("/api/order-simulator").contentType("application/json").content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/data-simulator")).andExpect(jsonPath("$.data.enabled").value(true));
        mvc.perform(put("/api/order-simulator").contentType("application/json").content("{\"enabled\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(true));
        mvc.perform(get("/api/data-simulator")).andExpect(jsonPath("$.data.enabled").value(false));
    }
}
