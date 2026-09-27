package com.yl.clothing.common;

import com.yl.clothing.exception.BusinessException;

public final class Checks {
    private Checks() {}

    public static void pagination(int page, int size) {
        if (page < 1 || page > 1000000 || size < 1 || size > 100) {
            throw new BusinessException(400, "分页参数超出范围");
        }
    }

    public static <T> T require(T value, String name) {
        if (value == null) {
            throw new BusinessException(404, name + "不存在");
        }
        return value;
    }
}
