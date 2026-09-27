package com.yl.clothing.converter;

import com.yl.clothing.entity.*;
import com.yl.clothing.entity.projection.*;
import com.yl.clothing.vo.*;
import java.util.List;

/** 显式转换，避免向 API 泄漏数据库字段。 */
public final class ChannelConverter {
    private ChannelConverter() {}

    public static ChannelVO channel(ChannelStatistics row) {
        return new ChannelVO(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getUserCount(),
                row.getOrderCount(),
                row.getProductCount(),
                row.getOnSaleCount());
    }
}
