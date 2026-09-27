package com.yl.clothing.entity.projection;


import com.yl.clothing.entity.Listing;

/** 类型化 SQL 查询结果，不直接作为接口响应。 */
public class ListingDetails extends Listing {
    private String channelName;
    private String size;
    private Long colorId;
    private String colorName;
    private String hex1;
    private String hex2;
    private Long productId;

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Long getColorId() {
        return colorId;
    }

    public void setColorId(Long colorId) {
        this.colorId = colorId;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public String getHex1() {
        return hex1;
    }

    public void setHex1(String hex1) {
        this.hex1 = hex1;
    }

    public String getHex2() {
        return hex2;
    }

    public void setHex2(String hex2) {
        this.hex2 = hex2;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
