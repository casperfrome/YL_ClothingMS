package com.yl.clothing.service;

import com.yl.clothing.vo.SimulatorStatusVO;
import org.springframework.stereotype.Service;

@Service
public class OrderSimulatorService {
    private final SimulationCoordinator coordinator;

    public OrderSimulatorService(SimulationCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public SimulatorStatusVO status() {
        return coordinator.orderStatus();
    }

    public SimulatorStatusVO setEnabled(boolean enabled) {
        return coordinator.setOrderEnabled(enabled);
    }

    public SimulatorStatusVO setRetainData(boolean retainData) {
        return coordinator.setOrderRetainData(retainData);
    }
}
