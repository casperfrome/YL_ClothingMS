package com.yl.clothing.service;

import com.yl.clothing.common.Checks;
import com.yl.clothing.common.PageResult;
import com.yl.clothing.converter.ProductConverter;
import com.yl.clothing.dto.*;
import com.yl.clothing.entity.Product;
import com.yl.clothing.entity.ProductColor;
import com.yl.clothing.exception.BusinessException;
import com.yl.clothing.mapper.ProductMapper;
import com.yl.clothing.vo.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductMapper mapper;
    private final CategoryService categories;

    public ProductService(ProductMapper mapper, CategoryService categories) {
        this.mapper = mapper;
        this.categories = categories;
    }

    public PageResult<ProductVO> list(String keyword, Long categoryId, Long channelId, Boolean onSale, int page, int pageSize) {
        Checks.pagination(page, pageSize);
        var query = new ProductQuery(keyword, categoryId, channelId, onSale, pageSize, (page - 1) * pageSize);
        return new PageResult<>(mapper.list(query).stream().map(ProductConverter::product).toList(),
                mapper.count(query), page, pageSize);
    }

    public ProductDetailVO find(long id) {
        var product = Checks.require(mapper.find(id), "商品");
        var colors = mapper.colors(id).stream()
                .map(color -> ProductConverter.colorDetail(color,
                        mapper.images(color.getId()).stream().map(ProductConverter::image).toList()))
                .toList();
        return ProductConverter.detail(product, colors);
    }

    @Transactional
    public ProductDetailVO create(ProductCreateRequest value) {
        categories.find(value.categoryId());
        Product product = new Product();
        product.setCode(value.code().strip());
        product.setName(value.name().strip());
        product.setCategoryId(value.categoryId());
        product.setDescription(value.description() == null ? "" : value.description());
        mapper.insert(product);
        return find(product.getId());
    }

    @Transactional
    public ProductDetailVO update(long id, ProductPatchRequest value) {
        find(id);
        if (value.categoryId() != null) {
            categories.find(value.categoryId());
        }
        if (value.name() == null && value.code() == null && value.description() == null && value.categoryId() == null) {
            throw new BusinessException(400, "请提供需要修改的字段");
        }
        mapper.update(id, value);
        return find(id);
    }

    @Transactional
    public void delete(long id) {
        find(id);
        mapper.delete(id);
    }

    public ColorVO color(long id) {
        return ProductConverter.color(Checks.require(mapper.color(id), "配色"));
    }

    @Transactional
    public ColorVO createColor(long productId, ColorCreateRequest value) {
        find(productId);
        ProductColor color = new ProductColor();
        color.setProductId(productId);
        color.setName(value.name().strip());
        color.setHex1(value.hex1());
        color.setHex2(value.hex2());
        mapper.insertColor(color);
        mapper.createSkus(color.getId());
        mapper.createListings(color.getId(), value.initialPrice());
        return color(color.getId());
    }

    @Transactional
    public ColorVO updateColor(long id, ColorPatchRequest value) {
        color(id);
        mapper.updateColor(id, value);
        return color(id);
    }

    @Transactional
    public void deleteColor(long id) {
        color(id);
        mapper.deleteColor(id);
    }

    public PageResult<ListingVO> listings(long productId, Long colorId, Long channelId, String size,
                                         Boolean onSale, int page, int pageSize) {
        Checks.pagination(page, pageSize);
        Checks.require(mapper.find(productId), "商品");
        if (size != null && !Set.of("S", "M", "L", "XL", "XXL").contains(size)) {
            throw new BusinessException(400, "尺码只能为 S、M、L、XL、XXL");
        }
        var query = new ListingQuery(productId, colorId, channelId, size, onSale, pageSize, (page - 1) * pageSize);
        return new PageResult<>(mapper.listings(query).stream().map(ProductConverter::listing).toList(),
                mapper.listingCount(query), page, pageSize);
    }

    @Transactional
    public List<ListingVO> updateListings(List<Long> ids, BigDecimal price, Boolean onSale) {
        if (price == null && onSale == null) {
            throw new BusinessException(400, "请提供价格或销售状态");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new BusinessException(400, "销售记录不能重复");
        }
        ids.forEach(id -> Checks.require(mapper.listing(id), "销售记录"));
        int updated = mapper.updateListings(ids, price, onSale);
        if (updated != ids.size()) {
            throw new BusinessException(409, "商品状态已变化，请刷新后重试；本次修改已回滚");
        }
        return ids.stream().map(mapper::listing).map(ProductConverter::listing).toList();
    }
}
