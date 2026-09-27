package com.yl.clothing.dto;

/** 已校验的数据库查询条件；limit/offset 不作为 HTTP 参数暴露。 */
public record ProductQuery(String keyword, Long categoryId, Long channelId, Boolean onSale, int limit, int offset) {
}
