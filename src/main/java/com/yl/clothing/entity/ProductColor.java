package com.yl.clothing.entity;



/** 数据库持久化实体。 */
public class ProductColor {
    private Long id;
    private Long productId;
    private String name;
    private String hex1;
    private String hex2;
    private Boolean deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
