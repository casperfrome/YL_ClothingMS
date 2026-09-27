package com.yl.clothing.mapper;

import com.yl.clothing.dto.*;
import com.yl.clothing.entity.*;
import com.yl.clothing.entity.projection.*;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductMapper {
    List<ProductSummary> list(@Param("q") ProductQuery query);
    long count(@Param("q") ProductQuery query);
    ProductSummary find(long id);
    void insert(Product product);
    int update(@Param("id") long id, @Param("v") ProductPatchRequest value);
    int delete(long id);
    List<ProductColor> colors(long productId);
    ProductColor color(long id);
    void insertColor(ProductColor color);
    int updateColor(@Param("id") long id, @Param("v") ColorPatchRequest value);
    int deleteColor(long id);
    void createSkus(long colorId);
    void createListings(@Param("colorId") long colorId, @Param("price") BigDecimal price);
    List<ListingDetails> listings(@Param("q") ListingQuery query);
    long listingCount(@Param("q") ListingQuery query);
    ListingDetails listing(long id);
    int updateListings(@Param("ids") List<Long> ids, @Param("price") BigDecimal price, @Param("onSale") Boolean onSale);
    List<ProductImage> images(long colorId);
    ProductImage image(long id);
    void insertImage(ProductImage image);
    int deleteImage(long id);
    void sortImage(@Param("id") long id, @Param("sortOrder") int sortOrder);
    Long lockColor(long id);
}
