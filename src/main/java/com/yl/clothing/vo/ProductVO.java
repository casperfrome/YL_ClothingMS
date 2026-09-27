package com.yl.clothing.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductVO(
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
        String thumbnail) {
}
