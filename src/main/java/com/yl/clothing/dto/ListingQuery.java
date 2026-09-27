package com.yl.clothing.dto;

/** 已校验的数据库查询条件；limit/offset 不作为 HTTP 参数暴露。 */
public record ListingQuery(long productId, Long colorId, Long channelId, String variantSize, Boolean onSale, int limit, int offset) {
}
