package com.yl.clothing.vo;



public record ChannelVO(
        Long id,
        String code,
        String name,
        Long userCount,
        Long orderCount,
        Long productCount,
        Long onSaleCount) {
}
