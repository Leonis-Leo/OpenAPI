package com.openapi.domain.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 调用统计聚合 Mapper：明细日志落库的同时按天/累计计数，统计与日志删除解耦。
 */
public interface InvokeStatsMapper {

    @Insert("INSERT INTO invoke_stats_daily (stat_date, app_id, interface_id, total, success, fail, total_cost_ms, create_time, update_time) " +
            "VALUES (#{statDate}, #{appId}, #{interfaceId}, 1, #{success}, #{fail}, #{costMs}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE total = total + 1, success = success + #{success}, fail = fail + #{fail}, " +
            "total_cost_ms = total_cost_ms + #{costMs}, update_time = NOW()")
    int upsertDaily(@Param("statDate") LocalDate statDate,
                    @Param("appId") Long appId,
                    @Param("interfaceId") Long interfaceId,
                    @Param("success") int success,
                    @Param("fail") int fail,
                    @Param("costMs") long costMs);

    @Insert("INSERT INTO invoke_stats_counter (app_id, interface_id, total, success, fail, total_cost_ms, update_time) " +
            "VALUES (#{appId}, #{interfaceId}, 1, #{success}, #{fail}, #{costMs}, NOW()) " +
            "ON DUPLICATE KEY UPDATE total = total + 1, success = success + #{success}, fail = fail + #{fail}, " +
            "total_cost_ms = total_cost_ms + #{costMs}, update_time = NOW()")
    int upsertCounter(@Param("appId") Long appId,
                      @Param("interfaceId") Long interfaceId,
                      @Param("success") int success,
                      @Param("fail") int fail,
                      @Param("costMs") long costMs);

    @Select("SELECT COALESCE(SUM(total), 0) AS total, COALESCE(SUM(success), 0) AS success FROM invoke_stats_counter")
    Map<String, Object> overview();

    @Select("SELECT stat_date AS day, SUM(total) AS total, SUM(success) AS ok " +
            "FROM invoke_stats_daily WHERE stat_date >= #{since} GROUP BY stat_date ORDER BY day")
    List<Map<String, Object>> daily(@Param("since") LocalDate since);

    @Select("SELECT interface_id AS interfaceId, SUM(total) AS total, SUM(success) AS ok " +
            "FROM invoke_stats_counter GROUP BY interface_id ORDER BY total DESC LIMIT #{limit}")
    List<Map<String, Object>> topInterfaces(@Param("limit") int limit);

    @Select("SELECT app_id AS appId, SUM(total) AS total, SUM(success) AS ok " +
            "FROM invoke_stats_counter GROUP BY app_id ORDER BY total DESC LIMIT #{limit}")
    List<Map<String, Object>> topApps(@Param("limit") int limit);

    @Select("<script>" +
            "SELECT " +
            "<choose>" +
            "  <when test='dimension == \"app\"'>app_id AS appId</when>" +
            "  <when test='dimension == \"interface\"'>interface_id AS interfaceId</when>" +
            "  <otherwise>stat_date AS day</otherwise>" +
            "</choose>, " +
            "SUM(total) AS total, SUM(success) AS success, SUM(fail) AS fail, SUM(total_cost_ms) AS totalCostMs " +
            "FROM invoke_stats_daily " +
            "<where>" +
            "  <if test='startDate != null'>AND stat_date &gt;= #{startDate}</if>" +
            "  <if test='endDate != null'>AND stat_date &lt;= #{endDate}</if>" +
            "  <if test='appId != null'>AND app_id = #{appId}</if>" +
            "  <if test='interfaceId != null'>AND interface_id = #{interfaceId}</if>" +
            "</where> " +
            "<choose>" +
            "  <when test='dimension == \"app\"'>GROUP BY app_id ORDER BY total DESC</when>" +
            "  <when test='dimension == \"interface\"'>GROUP BY interface_id ORDER BY total DESC</when>" +
            "  <otherwise>GROUP BY stat_date ORDER BY stat_date DESC</otherwise>" +
            "</choose>" +
            "</script>")
    IPage<Map<String, Object>> pageDaily(Page<?> page,
                                         @Param("dimension") String dimension,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("appId") Long appId,
                                         @Param("interfaceId") Long interfaceId);
}
