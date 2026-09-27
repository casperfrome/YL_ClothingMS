package com.yl.clothing.entity.projection;


import com.yl.clothing.entity.Category;

/** 类型化 SQL 查询结果，不直接作为接口响应。 */
public class CategorySummary extends Category {
    private Long productCount;

    public Long getProductCount() {
        return productCount;
    }

    public void setProductCount(Long productCount) {
        this.productCount = productCount;
    }
}
