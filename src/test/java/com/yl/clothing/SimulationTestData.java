package com.yl.clothing;

import com.yl.clothing.service.SimulationCoordinator;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Retained runs deliberately survive lifecycle hooks, so test fixtures need an explicit reset. */
public final class SimulationTestData {
    private SimulationTestData() {}

    public static void reset(JdbcTemplate db, SimulationCoordinator coordinator) {
        assertThat(db.queryForObject("SELECT DATABASE()", String.class)).isEqualToIgnoringCase("YL_260924_test");
        coordinator.setOrderEnabled(false);
        coordinator.setDataEnabled(false);
        db.update("DELETE FROM sales_order");
        db.update("DELETE uc FROM user_channel uc JOIN customer c ON c.id=uc.user_id WHERE c.simulated=1");
        db.update("DELETE FROM customer WHERE simulated=1");
        db.update("DELETE FROM simulation_run");
        coordinator.setOrderRetainData(false);
        coordinator.setDataRetainData(false);
        // Some persistence tests write through a separate coordinator or the batch service.
        db.update("UPDATE simulation_settings SET retain_data=0");
    }
}
