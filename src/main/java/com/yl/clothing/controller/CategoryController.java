package com.yl.clothing.controller;

import com.yl.clothing.common.ApiResponse;
import com.yl.clothing.dto.CategoryRequest;
import com.yl.clothing.service.CategoryService;
import com.yl.clothing.vo.CategoryVO;
import com.yl.clothing.vo.CategorySummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CategorySummaryVO>> all() {
        return ApiResponse.success(service.all());
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryRequest body) {
        return ApiResponse.success(service.create(body.name()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<CategoryVO> rename(@PathVariable long id, @Valid @RequestBody CategoryRequest body) {
        return ApiResponse.success(service.rename(id, body.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
