package com.yl.clothing.controller;

import com.yl.clothing.common.ApiResponse;
import com.yl.clothing.common.PageResult;
import com.yl.clothing.dto.*;
import com.yl.clothing.service.ImageService;
import com.yl.clothing.service.ProductService;
import com.yl.clothing.vo.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
public class ProductController {
    private final ProductService service;
    private final ImageService images;

    public ProductController(ProductService service, ImageService images) {
        this.service = service;
        this.images = images;
    }

    @GetMapping("/api/products")
    public ApiResponse<PageResult<ProductVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.list(keyword, categoryId, channelId, onSale, page, pageSize));
    }

    @GetMapping("/api/products/{id}")
    public ApiResponse<ProductDetailVO> find(@PathVariable long id) {
        return ApiResponse.success(service.find(id));
    }

    @PostMapping("/api/products")
    @ResponseStatus(CREATED)
    public ApiResponse<ProductDetailVO> create(@Valid @RequestBody ProductCreateRequest value) {
        return ApiResponse.success(service.create(value));
    }

    @PatchMapping("/api/products/{id}")
    public ApiResponse<ProductDetailVO> update(@PathVariable long id, @Valid @RequestBody ProductPatchRequest value) {
        return ApiResponse.success(service.update(id, value));
    }

    @DeleteMapping("/api/products/{id}")
    @ResponseStatus(NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }

    @PostMapping("/api/products/{id}/colors")
    @ResponseStatus(CREATED)
    public ApiResponse<ColorVO> color(@PathVariable long id, @Valid @RequestBody ColorCreateRequest value) {
        return ApiResponse.success(service.createColor(id, value));
    }

    @PatchMapping("/api/product-colors/{id}")
    public ApiResponse<ColorVO> updateColor(@PathVariable long id, @Valid @RequestBody ColorPatchRequest value) {
        return ApiResponse.success(service.updateColor(id, value));
    }

    @DeleteMapping("/api/product-colors/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteColor(@PathVariable long id) {
        service.deleteColor(id);
    }

    @GetMapping("/api/products/{id}/listings")
    public ApiResponse<PageResult<ListingVO>> listings(
            @PathVariable long id,
            @RequestParam(required = false) Long colorId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(service.listings(id, colorId, channelId, size, onSale, page, pageSize));
    }

    @PatchMapping("/api/listings/{id}")
    public ApiResponse<ListingVO> listing(@PathVariable long id, @Valid @RequestBody ListingPatchRequest value) {
        return ApiResponse.success(service.updateListings(List.of(id), value.price(), value.onSale()).getFirst());
    }

    @PatchMapping("/api/listings")
    public ApiResponse<List<ListingVO>> batch(@Valid @RequestBody ListingBatchRequest value) {
        return ApiResponse.success(service.updateListings(value.ids(), value.price(), value.onSale()));
    }

    @PostMapping("/api/product-colors/{id}/images")
    @ResponseStatus(CREATED)
    public ApiResponse<List<ImageVO>> upload(@PathVariable long id, @RequestParam("files") List<MultipartFile> files) throws IOException {
        return ApiResponse.success(images.upload(id, files));
    }

    @DeleteMapping("/api/images/{id}")
    @ResponseStatus(NO_CONTENT)
    public void deleteImage(@PathVariable long id) {
        images.delete(id);
    }

    @PutMapping("/api/product-colors/{id}/images/order")
    public ApiResponse<List<ImageVO>> sort(@PathVariable long id, @Valid @RequestBody ImageOrderRequest value) {
        return ApiResponse.success(images.sort(id, value.ids()));
    }
}
