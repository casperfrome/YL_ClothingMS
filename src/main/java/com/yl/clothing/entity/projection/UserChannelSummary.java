package com.yl.clothing.entity.projection;


import com.yl.clothing.entity.Channel;

/** 类型化 SQL 查询结果，不直接作为接口响应。 */
public class UserChannelSummary extends Channel {
    private Long orderCount;

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }
}
