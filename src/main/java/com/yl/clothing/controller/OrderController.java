package com.yl.clothing.controller;

import com.yl.clothing.common.ApiResponse;
import com.yl.clothing.common.PageResult;
import com.yl.clothing.service.OrderService;
import com.yl.clothing.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/api/users/{id}/orders")
    public ApiResponse<PageResult<OrderVO>> list(@PathVariable long id,
                                               @RequestParam(required = false) Long channelId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.list(id, channelId, status, page, pageSize));
    }

    @GetMapping("/api/orders/{id}")
    public ApiResponse<OrderVO> find(@PathVariable long id) {
        return ApiResponse.success(service.find(id));
    }
}
