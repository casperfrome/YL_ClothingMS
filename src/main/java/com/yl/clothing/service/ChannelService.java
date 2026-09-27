package com.yl.clothing.service;

import com.yl.clothing.converter.ChannelConverter;
import com.yl.clothing.mapper.ChannelMapper;
import com.yl.clothing.vo.ChannelVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChannelService {
    private final ChannelMapper mapper;

    public ChannelService(ChannelMapper mapper) {
        this.mapper = mapper;
    }

    public List<ChannelVO> all() {
        return mapper.all().stream().map(ChannelConverter::channel).toList();
    }
}
