package com.yl.clothing.controller;

import com.yl.clothing.common.ApiResponse;
import com.yl.clothing.dto.SimulatorToggleRequest;
import com.yl.clothing.dto.SimulatorRetentionRequest;
import com.yl.clothing.service.OrderSimulatorService;
import com.yl.clothing.vo.SimulatorStatusVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-simulator")
public class SimulationController {
    private final OrderSimulatorService simulator;

    public SimulationController(OrderSimulatorService simulator) {
        this.simulator = simulator;
    }

    @GetMapping
    public ApiResponse<SimulatorStatusVO> status() {
        return ApiResponse.success(simulator.status());
    }

    @PutMapping
    public ApiResponse<SimulatorStatusVO> toggle(@Valid @RequestBody SimulatorToggleRequest value) {
        return ApiResponse.success(simulator.setEnabled(value.enabled()));
    }

    @PatchMapping
    public ApiResponse<SimulatorStatusVO> retain(@Valid @RequestBody SimulatorRetentionRequest value) {
        return ApiResponse.success(simulator.setRetainData(value.retainData()));
    }
}
