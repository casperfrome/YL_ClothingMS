package com.yl.clothing.service;

import com.yl.clothing.common.Checks;
import com.yl.clothing.common.PageResult;
import com.yl.clothing.converter.OrderConverter;
import com.yl.clothing.dto.OrderQuery;
import com.yl.clothing.enums.OrderStatus;
import com.yl.clothing.exception.BusinessException;
import com.yl.clothing.mapper.OrderMapper;
import com.yl.clothing.vo.OrderVO;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderMapper mapper;
    private final UserService users;

    public OrderService(OrderMapper mapper, UserService users) {
        this.mapper = mapper;
        this.users = users;
    }

    public PageResult<OrderVO> list(long userId, Long channelId, String status, int page, int pageSize) {
        Checks.pagination(page, pageSize);
        users.find(userId);
        if (status != null) {
            try {
                OrderStatus.valueOf(status);
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(400, "订单状态无效");
            }
        }
        if (channelId != null && users.channels(userId).stream().noneMatch(channel -> channel.id().equals(channelId))) {
            throw new BusinessException(400, "用户尚未开通该渠道");
        }
        var query = new OrderQuery(userId, channelId, status, pageSize, (page - 1) * pageSize);
        return new PageResult<>(mapper.list(query).stream().map(OrderConverter::order).toList(),
                mapper.count(query), page, pageSize);
    }

    public OrderVO find(long id) {
        return OrderConverter.order(Checks.require(mapper.find(id), "订单（可能已被模拟器清空）"));
    }
}
