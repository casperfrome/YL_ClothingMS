package com.yl.clothing.dto;

/** 已校验的数据库查询条件；limit/offset 不作为 HTTP 参数暴露。 */
public record OrderQuery(long userId, Long channelId, String status, int limit, int offset) {
}
