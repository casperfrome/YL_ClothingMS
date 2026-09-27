package com.yl.clothing.converter;

import com.yl.clothing.entity.*;
import com.yl.clothing.entity.projection.*;
import com.yl.clothing.vo.*;
import java.util.List;

/** 显式转换，避免向 API 泄漏数据库字段。 */
public final class UserConverter {
    private UserConverter() {}

    public static UserVO user(CustomerSummary row) {
        return new UserVO(
                row.getId(),
                row.getPhone(),
                row.getNickname(),
                row.getGender(),
                row.getBirthday(),
                row.getCreatedAt(),
                row.getChannelCount(),
                row.getOrderCount());
    }

    public static UserChannelVO channel(UserChannelSummary row) {
        return new UserChannelVO(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getOrderCount());
    }
}
