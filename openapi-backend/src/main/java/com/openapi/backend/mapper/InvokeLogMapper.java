package com.openapi.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.openapi.backend.entity.InvokeLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InvokeLogMapper extends BaseMapper<InvokeLog> {

    @Select("SELECT COUNT(*) FROM invoke_log")
    Long countAll();

    @Select("SELECT COUNT(*) FROM invoke_log WHERE success = 1")
    Long countSuccess();

    @Select("SELECT DATE(create_time) AS day, COUNT(*) AS total, SUM(success) AS ok " +
            "FROM invoke_log WHERE create_time >= #{since} " +
            "GROUP BY DATE(create_time) ORDER BY day")
    List<Map<String, Object>> dailyStats(@Param("since") LocalDateTime since);
}
