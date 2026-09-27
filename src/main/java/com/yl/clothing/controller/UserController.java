package com.yl.clothing.controller;

import com.yl.clothing.common.ApiResponse;
import com.yl.clothing.common.PageResult;
import com.yl.clothing.dto.UserUpdateRequest;
import com.yl.clothing.service.UserService;
import com.yl.clothing.vo.UserVO;
import com.yl.clothing.vo.UserChannelVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<UserVO>> list(@RequestParam(required = false) String phone,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.list(phone, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> find(@PathVariable long id) {
        return ApiResponse.success(service.find(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<UserVO> update(@PathVariable long id, @Valid @RequestBody UserUpdateRequest value) {
        return ApiResponse.success(service.update(id, value));
    }

    @GetMapping("/{id}/channels")
    public ApiResponse<List<UserChannelVO>> channels(@PathVariable long id) {
        return ApiResponse.success(service.channels(id));
    }
}
