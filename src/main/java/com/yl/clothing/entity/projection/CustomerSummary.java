package com.yl.clothing.entity.projection;


import com.yl.clothing.entity.Customer;

/** 类型化 SQL 查询结果，不直接作为接口响应。 */
public class CustomerSummary extends Customer {
    private Long channelCount;
    private Long orderCount;

    public Long getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(Long channelCount) {
        this.channelCount = channelCount;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }
}
