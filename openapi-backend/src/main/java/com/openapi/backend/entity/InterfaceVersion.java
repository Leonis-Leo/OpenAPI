package com.openapi.backend.entity;

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
 * 接口发布版本快照：发布/在线更新时生成，支持变更 diff 与一键回滚。
 */
@Data
@TableName("interface_version")
public class InterfaceVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long interfaceId;

    /** 版本号，从 1 递增 */
    private Integer versionNo;

    private String name;

    private String description;

    private String method;

    private String url;

    private String requestParams;

    private String responseExample;

    /** 接口状态快照：0 下线 1 上线 */
    private Integer status;

    private String changeNote;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer isDelete;
}
