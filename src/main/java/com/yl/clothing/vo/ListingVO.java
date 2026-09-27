package com.yl.clothing.vo;

import java.math.BigDecimal;

public record ListingVO(
        Long id,
        Long skuId,
        Long channelId,
        BigDecimal price,
        Boolean onSale,
        String channelName,
        String size,
        Long colorId,
        String colorName,
        String hex1,
        String hex2,
        Long productId) {
}
