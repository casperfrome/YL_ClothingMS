package com.yl.clothing.converter;

import com.yl.clothing.entity.*;
import com.yl.clothing.entity.projection.*;
import com.yl.clothing.vo.*;
import java.util.List;

/** 显式转换，避免向 API 泄漏数据库字段。 */
public final class OrderConverter {
    private OrderConverter() {}

    public static OrderVO order(OrderDetails row) {
        return new OrderVO(
                row.getId(),
                row.getOrderNo(),
                row.getUserId(),
                row.getListingId(),
                row.getChannelId(),
                row.getProductCode(),
                row.getProductName(),
                row.getColorName(),
                row.getHex1(),
                row.getHex2(),
                row.getSize(),
                row.getChannelName(),
                row.getUnitPrice(),
                row.getQuantity(),
                row.getTotalPrice(),
                row.getStatus(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getPhone(),
                row.getNickname());
    }
}
