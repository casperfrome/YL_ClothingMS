package com.yl.clothing.entity.projection;


import com.yl.clothing.entity.SalesOrder;

/** 类型化 SQL 查询结果，不直接作为接口响应。 */
public class OrderDetails extends SalesOrder {
    private String phone;
    private String nickname;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
