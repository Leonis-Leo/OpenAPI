package com.openapi.backend.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("audit_log")
public class AuditLog { @TableId(type=IdType.ASSIGN_ID) private Long id; private Long userId; private String action; private String resource; private String ip; private Integer statusCode; private Integer success; private String detail; private LocalDateTime createTime; }
