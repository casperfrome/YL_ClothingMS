package com.yl.clothing.mapper;

import com.yl.clothing.dto.UserQuery;
import com.yl.clothing.dto.UserUpdateRequest;
import com.yl.clothing.entity.Customer;
import com.yl.clothing.entity.projection.CustomerSummary;
import com.yl.clothing.entity.projection.UserChannelSummary;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    List<CustomerSummary> list(@Param("q") UserQuery query);
    long count(String phone);
    CustomerSummary find(long id);
    List<UserChannelSummary> channels(long id);
    void update(@Param("id") long id, @Param("v") UserUpdateRequest value);
    List<Long> channelIds();
    boolean phoneExists(String phone);
    void insertSimulated(Customer customer);
    void addChannel(@Param("userId") long userId, @Param("channelId") long channelId);
    void clearSimulationChannels(long runId);
    void clearSimulationUsers(long runId);
}
