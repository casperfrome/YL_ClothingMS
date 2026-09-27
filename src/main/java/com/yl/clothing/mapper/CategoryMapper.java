package com.yl.clothing.mapper;

import com.yl.clothing.entity.Category;
import com.yl.clothing.entity.projection.CategorySummary;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CategoryMapper {
    List<CategorySummary> all();
    Category find(long id);
    void insert(Category category);
    int rename(@Param("id") long id, @Param("name") String name);
    int references(long id);
    void unlinkDeletedProducts(long id);
    int delete(long id);
}
