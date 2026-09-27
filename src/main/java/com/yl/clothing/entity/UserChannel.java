package com.yl.clothing.entity;



/** 数据库持久化实体。 */
public class UserChannel {
    private Long userId;
    private Long channelId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }
}
