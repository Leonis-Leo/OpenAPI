package com.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 接口调用日志（由 MQ 消费者异步写入）。
 */
@Data
@TableName("invoke_log")
public class InvokeLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long interfaceId;

    private Long appId;

    private Long userId;

    private String ip;

    private String method;

    private String path;

    private String requestParams;

    /** 请求头（JSON，脱敏后） */
    private String requestHeaders;

    private String responseBody;

    private Integer statusCode;

    /** 是否成功：0 失败 1 成功 */
    private Integer success;

    /** 调用耗时（毫秒） */
    private Long costMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
