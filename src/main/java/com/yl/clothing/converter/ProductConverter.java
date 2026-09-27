package com.yl.clothing.converter;

import com.yl.clothing.entity.*;
import com.yl.clothing.entity.projection.*;
import com.yl.clothing.vo.*;
import java.util.List;

/** 显式转换，避免向 API 泄漏数据库字段。 */
public final class ProductConverter {
    private ProductConverter() {}

    public static ProductVO product(ProductSummary row) {
        return new ProductVO(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getDescription(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getCreatedAt(),
                row.getColorCount(),
                row.getMinPrice(),
                row.getMaxPrice(),
                row.getOnSaleCount(),
                row.getListingCount(),
                row.getThumbnail());
    }

    public static ProductDetailVO detail(ProductSummary row, List<ColorDetailVO> colors) {
        return new ProductDetailVO(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getDescription(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getCreatedAt(),
                row.getColorCount(),
                row.getMinPrice(),
                row.getMaxPrice(),
                row.getOnSaleCount(),
                row.getListingCount(),
                row.getThumbnail(),
                colors);
    }

    public static ColorVO color(ProductColor row) {
        return new ColorVO(
                row.getId(),
                row.getProductId(),
                row.getName(),
                row.getHex1(),
                row.getHex2());
    }

    public static ColorDetailVO colorDetail(ProductColor row, List<ImageVO> images) {
        return new ColorDetailVO(
                row.getId(),
                row.getProductId(),
                row.getName(),
                row.getHex1(),
                row.getHex2(),
                images);
    }

    public static ImageVO image(ProductImage row) {
        return new ImageVO(
                row.getId(),
                row.getColorId(),
                row.getUrl(),
                row.getSortOrder());
    }

    public static ListingVO listing(ListingDetails row) {
        return new ListingVO(
                row.getId(),
                row.getSkuId(),
                row.getChannelId(),
                row.getPrice(),
                row.getOnSale(),
                row.getChannelName(),
                row.getSize(),
                row.getColorId(),
                row.getColorName(),
                row.getHex1(),
                row.getHex2(),
                row.getProductId());
    }
}
