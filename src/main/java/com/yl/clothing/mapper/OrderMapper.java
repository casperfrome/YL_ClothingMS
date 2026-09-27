package com.yl.clothing.mapper;

import com.yl.clothing.dto.OrderQuery;
import com.yl.clothing.entity.SalesOrder;
import com.yl.clothing.entity.UserChannel;
import com.yl.clothing.entity.projection.*;
import java.util.List;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper {
    List<OrderDetails> list(@Param("q") OrderQuery query);
    long count(@Param("q") OrderQuery query);
    OrderDetails find(long id);
    default OrderCandidate candidate(long runId) {
        UserChannel relation = candidateUserChannel(runId);
        return relation == null ? null : candidateForChannel(relation.getUserId(), relation.getChannelId());
    }
    UserChannel candidateUserChannel(long runId);
    OrderCandidate candidateForChannel(@Param("userId") long userId, @Param("channelId") long channelId);
    void insert(SalesOrder order);
    List<MutableOrder> dueOrders(@Param("runId") long runId, @Param("status") String status, @Param("cutoff") LocalDateTime cutoff,
                                @Param("limit") int limit);
    int updateStatus(@Param("id") long id, @Param("from") String from, @Param("to") String to,
                     @Param("changedAt") LocalDateTime changedAt);
    void clearRun(long runId);
    long total();
}
