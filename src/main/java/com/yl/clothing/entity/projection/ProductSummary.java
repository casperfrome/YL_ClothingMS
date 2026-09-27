package com.yl.clothing.entity.projection;

import java.math.BigDecimal;
import com.yl.clothing.entity.Product;

/** 类型化 SQL 查询结果，不直接作为接口响应。 */
public class ProductSummary extends Product {
    private String categoryName;
    private Long colorCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Long onSaleCount;
    private Long listingCount;
    private String thumbnail;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getColorCount() {
        return colorCount;
    }

    public void setColorCount(Long colorCount) {
        this.colorCount = colorCount;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Long getOnSaleCount() {
        return onSaleCount;
    }

    public void setOnSaleCount(Long onSaleCount) {
        this.onSaleCount = onSaleCount;
    }

    public Long getListingCount() {
        return listingCount;
    }

    public void setListingCount(Long listingCount) {
        this.listingCount = listingCount;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
}
