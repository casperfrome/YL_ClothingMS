package com.yl.clothing.service;

import com.yl.clothing.common.Checks;
import com.yl.clothing.converter.CategoryConverter;
import com.yl.clothing.entity.Category;
import com.yl.clothing.exception.BusinessException;
import com.yl.clothing.mapper.CategoryMapper;
import com.yl.clothing.vo.CategoryVO;
import com.yl.clothing.vo.CategorySummaryVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryMapper mapper;

    public CategoryService(CategoryMapper mapper) {
        this.mapper = mapper;
    }

    public List<CategorySummaryVO> all() {
        return mapper.all().stream().map(CategoryConverter::summary).toList();
    }

    public CategoryVO find(long id) {
        return CategoryConverter.category(Checks.require(mapper.find(id), "品类"));
    }

    @Transactional
    public CategoryVO create(String name) {
        Category category = new Category();
        category.setName(name.strip());
        mapper.insert(category);
        return find(category.getId());
    }

    @Transactional
    public CategoryVO rename(long id, String name) {
        find(id);
        mapper.rename(id, name.strip());
        return find(id);
    }

    @Transactional
    public void delete(long id) {
        find(id);
        if (mapper.references(id) > 0) {
            throw new BusinessException(409, "该品类仍被商品使用，请先调整商品分类");
        }
        mapper.unlinkDeletedProducts(id);
        mapper.delete(id);
    }
}
