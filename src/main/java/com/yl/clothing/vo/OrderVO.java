package com.yl.clothing.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderVO(
        Long id,
        String orderNo,
        Long userId,
        Long listingId,
        Long channelId,
        String productCode,
        String productName,
        String colorName,
        String hex1,
        String hex2,
        String size,
        String channelName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String phone,
        String nickname) {
}
