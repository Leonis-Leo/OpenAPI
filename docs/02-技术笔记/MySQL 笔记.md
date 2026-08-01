# MySQL 笔记

## 本机环境

- 版本：MySQL 8.0.36
- 连接：`localhost:3306`，账号 `root`，密码 `123456`
- 项目数据库：`openapi`

## 项目表结构

| 表 | 说明 |
| --- | --- |
| `user` | 平台用户 |
| `app` | 应用（一对 AccessKey / SecretKey） |
| `interface_info` | 对外发布的接口信息 |

初始化脚本：`db/init.sql`（Docker 与手动均可执行）

## 本机踩坑

MySQL 8 默认 `caching_sha2_password` 认证，JDBC 直连需在 URL 加 `allowPublicKeyRetrieval=true`，否则报 `Public Key Retrieval is not allowed`。详见 [[MySQL8-PublicKeyRetrieval]]。

## 关键知识点

- 索引优化（`EXPLAIN` 查看执行计划）
- 事务隔离级别与 MVCC
- 逻辑删除 vs 物理删除
