package com.yl.clothing.service;

import com.yl.clothing.vo.DataSimulatorStatusVO;
import org.springframework.stereotype.Service;

@Service
public class DataSimulatorService {
    private final SimulationCoordinator coordinator;

    public DataSimulatorService(SimulationCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    public DataSimulatorStatusVO status() {
        return coordinator.dataStatus();
    }

    public DataSimulatorStatusVO setEnabled(boolean enabled) {
        return coordinator.setDataEnabled(enabled);
    }

    public DataSimulatorStatusVO setRetainData(boolean retainData) {
        return coordinator.setDataRetainData(retainData);
    }
}
