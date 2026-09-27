package com.yl.clothing.mapper;

import com.yl.clothing.entity.projection.ChannelStatistics;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChannelMapper {
    List<ChannelStatistics> all();
}
