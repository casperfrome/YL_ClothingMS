package com.yl.clothing.controller;

import com.yl.clothing.common.ApiResponse;
import com.yl.clothing.service.ChannelService;
import com.yl.clothing.vo.ChannelVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChannelController {
    private final ChannelService service;

    public ChannelController(ChannelService service) {
        this.service = service;
    }

    @GetMapping("/api/channels")
    public ApiResponse<List<ChannelVO>> all() {
        return ApiResponse.success(service.all());
    }
}
