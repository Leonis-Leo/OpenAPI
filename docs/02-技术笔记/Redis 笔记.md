# Redis 笔记

## 本机环境

- 版本：Redis 5.0.14.1 for Windows（tporadowski 原生版）
- 安装目录：`E:\redis`
- 地址：`127.0.0.1:6379`，无密码
- 启动方式：后台进程（管理员可用 `redis-server --service-install` 装成系统服务）

## 常用命令

```text
ping                # 返回 PONG
set key value       # 写入
get key             # 读取
keys *              # 列出所有 key
ttl key             # 查看过期时间
```

## 在项目中的用途

1. **nonce 防重放**：`openapi:nonce:{accessKey}:{nonce}` 键 5 分钟过期，防止请求重放
2. 后续规划：Redis + Lua 令牌桶限流、接口调用统计缓存

## 关键知识点

- Redis 单线程模型、IO 多路复用
- 为什么 Lua 脚本保证原子性（限流场景）
- 缓存穿透 / 击穿 / 雪崩及解决方案

关联：[[签名鉴权原理]]、[[常见面试题]]
