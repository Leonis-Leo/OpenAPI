package com.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 接口信息实体。
 */
@Data
@TableName("interface_info")
public class InterfaceInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String description;

    private String method;

    private String url;

    private String requestParams;

    private String responseExample;

    /** 上游服务地址（配置后 /api/** 请求代理到上游） */
    private String upstream;

    /** 上游调用超时（毫秒） */
    private Integer timeoutMs;

    /** 失败重试次数 */
    private Integer retryCount;

    /** 所属分组 */
    private Long groupId;

    /** 分组名称（非表字段，联表展示） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String groupName;

    /** 标签列表（非表字段，联表展示） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<Map<String, Object>> tags;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
