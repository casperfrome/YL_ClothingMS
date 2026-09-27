package com.yl.clothing.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailVO(
        Long id,
        String code,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        Long colorCount,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Long onSaleCount,
        Long listingCount,
        String thumbnail,
        List<ColorDetailVO> colors) {
}
