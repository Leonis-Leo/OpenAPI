package com.openapi.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内通知实体：订阅申请、审批结果等系统通知。
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收人用户 ID */
    private Long userId;

    /** 类型：SUBSCRIBE_APPLY / SUBSCRIBE_APPROVED / SUBSCRIBE_REJECTED */
    private String type;

    private String title;

    private String content;

    /** 关联业务 ID（订阅记录 ID） */
    private Long bizId;

    /** 前端跳转路由，如 /subscribes */
    private String link;

    /** 0 未读，1 已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDelete;
}
