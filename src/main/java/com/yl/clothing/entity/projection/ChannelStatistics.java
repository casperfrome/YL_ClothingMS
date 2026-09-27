package com.yl.clothing.entity.projection;


import com.yl.clothing.entity.Channel;

/** 类型化 SQL 查询结果，不直接作为接口响应。 */
public class ChannelStatistics extends Channel {
    private Long userCount;
    private Long orderCount;
    private Long productCount;
    private Long onSaleCount;

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }

    public Long getOnSaleCount() {
        return onSaleCount;
    }

    public void setOnSaleCount(Long onSaleCount) {
        this.onSaleCount = onSaleCount;
    }
}
