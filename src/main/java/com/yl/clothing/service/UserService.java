package com.yl.clothing.service;

import com.yl.clothing.common.Checks;
import com.yl.clothing.common.PageResult;
import com.yl.clothing.converter.UserConverter;
import com.yl.clothing.dto.UserQuery;
import com.yl.clothing.dto.UserUpdateRequest;
import com.yl.clothing.mapper.UserMapper;
import com.yl.clothing.vo.UserVO;
import com.yl.clothing.vo.UserChannelVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserMapper mapper;

    public UserService(UserMapper mapper) {
        this.mapper = mapper;
    }

    public PageResult<UserVO> list(String phone, int page, int pageSize) {
        Checks.pagination(page, pageSize);
        var query = new UserQuery(phone, pageSize, (page - 1) * pageSize);
        return new PageResult<>(mapper.list(query).stream().map(UserConverter::user).toList(),
                mapper.count(phone), page, pageSize);
    }

    public UserVO find(long id) {
        return UserConverter.user(Checks.require(mapper.find(id), "用户"));
    }

    public List<UserChannelVO> channels(long id) {
        find(id);
        return mapper.channels(id).stream().map(UserConverter::channel).toList();
    }

    @Transactional
    public UserVO update(long id, UserUpdateRequest value) {
        find(id);
        mapper.update(id, value);
        return find(id);
    }
}
