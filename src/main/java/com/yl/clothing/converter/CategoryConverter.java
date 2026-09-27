package com.yl.clothing.converter;

import com.yl.clothing.entity.*;
import com.yl.clothing.entity.projection.*;
import com.yl.clothing.vo.*;
import java.util.List;

/** 显式转换，避免向 API 泄漏数据库字段。 */
public final class CategoryConverter {
    private CategoryConverter() {}

    public static CategoryVO category(Category row) {
        return new CategoryVO(
                row.getId(),
                row.getName());
    }

    public static CategorySummaryVO summary(CategorySummary row) {
        return new CategorySummaryVO(
                row.getId(),
                row.getName(),
                row.getProductCount());
    }
}
