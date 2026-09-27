package com.yl.clothing.entity.projection;



/** 类型化 SQL 查询结果，不直接作为接口响应。 */
public class MutableOrder {
    private Long id;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
