# MySQL8 Public Key Retrieval 报错

- 日期：2026-08-01
- 状态：已解决

## 问题现象

后端启动正常，但访问数据库接口时返回 `code:50000 系统内部错误`，日志报：

```text
Caused by: java.sql.SQLNonTransientConnectionException: Public Key Retrieval is not allowed
```

## 原因

MySQL 8 默认使用 `caching_sha2_password` 认证插件，JDBC 非 SSL 连接时默认不允许自动获取服务端公钥。

## 解决

JDBC URL 增加参数：

```yaml
url: jdbc:mysql://localhost:3306/openapi?useSSL=false&allowPublicKeyRetrieval=true
```

## 关联

- [[MySQL 笔记]]
