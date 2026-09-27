package com.yl.clothing.mapper;

import com.yl.clothing.entity.SimulationRun;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SimulationMapper {
    Boolean retainData(String mode);
    int updateSetting(@Param("mode") String mode, @Param("retainData") boolean retainData);
    void insertRun(SimulationRun run);
    Boolean runRetainData(long id);
    int updateRunRetainData(@Param("id") long id, @Param("retainData") boolean retainData);
    List<Long> disposableRunIds();
    void endRun(@Param("id") long id, @Param("endedAt") LocalDateTime endedAt);
    void endRetainedRuns(LocalDateTime endedAt);
    void deleteRun(long id);
}
