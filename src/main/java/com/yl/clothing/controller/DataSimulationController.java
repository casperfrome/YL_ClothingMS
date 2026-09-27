package com.yl.clothing.controller;

import com.yl.clothing.common.ApiResponse;
import com.yl.clothing.dto.SimulatorToggleRequest;
import com.yl.clothing.dto.SimulatorRetentionRequest;
import com.yl.clothing.service.DataSimulatorService;
import com.yl.clothing.vo.DataSimulatorStatusVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data-simulator")
public class DataSimulationController {
    private final DataSimulatorService simulator;

    public DataSimulationController(DataSimulatorService simulator) {
        this.simulator = simulator;
    }

    @GetMapping
    public ApiResponse<DataSimulatorStatusVO> status() {
        return ApiResponse.success(simulator.status());
    }

    @PutMapping
    public ApiResponse<DataSimulatorStatusVO> toggle(@Valid @RequestBody SimulatorToggleRequest value) {
        return ApiResponse.success(simulator.setEnabled(value.enabled()));
    }

    @PatchMapping
    public ApiResponse<DataSimulatorStatusVO> retain(@Valid @RequestBody SimulatorRetentionRequest value) {
        return ApiResponse.success(simulator.setRetainData(value.retainData()));
    }
}
