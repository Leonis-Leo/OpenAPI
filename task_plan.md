# Task Plan: OpenAPI 开放平台 —— 阶段化推进路线

## Goal

将 OpenAPI 开放平台从"功能完备的 MVP"推进到工程化扎实、可观测、可水平扩展的生产级 API 开放平台。

## Next Step

完成 Phase 3 剩余项：前端全功能回归测试、JMeter 压测（需先定 QPS/P95 目标）、数据库定时备份。

## Current Phase

Phase 3

## Phases

### Phase 1: MVP 基础能力

- [x] Maven 多模块骨架（common / sdk / backend / gateway）
- [x] 签名鉴权全链路（HMAC-SHA256、时间戳防过期、nonce 防重放，网关+后端双层校验）
- [x] 用户注册/登录（JWT + Cookie）
- [x] 应用管理（AccessKey/SecretKey 自动生成）
- [x] 接口管理 + 在线调试
- [x] 演示接口与 OpenAPI 3.0 文档（springdoc/swagger）
- [x] Docker Compose（MySQL + Redis）+ db/init.sql 演示数据
- [x] CI：GitHub Actions + Apifox 接口用例全绿
- **Status:** complete

### Phase 2: 核心能力强化

- [x] 网关限流（Redis + Lua 令牌桶，接口维度 / 应用维度）
- [x] MQ 异步调用统计（RabbitMQ + invoke_log + ECharts）
- [x] 接口上下线与订阅审批流程
- [x] JWT 登录态与角色权限（菜单按角色隐藏）
- [x] 前端管理后台（Vue3 + Element Plus + Pinia）
- [x] 用户管理（列表 / 角色 / 启禁用）
- [x] 密码 BCrypt 加盐 + 存量账号登录惰性迁移（2026-08-02 完成并实测验证）
- **Status:** complete

### Phase 3: 工程化与质量保障

- [x] CI 启用单测执行（移除 -DskipTests）
- [x] 密码哈希单元测试（PasswordUtilsTest，6 用例）
- [x] 单元/集成测试补齐（签名工具、限流 Lua、鉴权拦截链路）
- [ ] 前端全功能回归测试（应用 / 接口 / 订阅 / 统计 / 日志 / 限流 / 用户管理）
- [x] 修复回归发现的前端缺陷（SecretKey 复制按钮复制空值已修复；「重置密钥弹窗截断」经复核非缺陷）
- [ ] JMeter 压测产出 QPS / RT 数据
- [x] DB 索引优化（invoke_log 按 user/app/时间维度组合索引，迁移 2026-08-13）
- [ ] 数据库定时备份
- [x] 稳定性排查：后端线程饥饿 / 挂起（HikariPool housekeeper 告警 → 定位为机器休眠 clock leap，显式配置 HikariCP 加固）
- **Status:** in_progress

### Phase 4: 可观测性与稳定性

- [ ] 统一日志与链路追踪（TraceId 贯穿网关→后端→MQ）
- [ ] 监控告警（JVM / 接口调用 / 限流命中指标）
- [x] 日志保留与归档策略（invoke_log 清理任务，InvokeLogCleanupTask）
- [x] 网关签名校验与后端校验一致性收敛（抽取 SignatureHeaderValidator 共用）
- **Status:** pending

### Phase 5: 微服务演进（可选）

- [ ] Spring Cloud Alibaba：Nacos 服务发现 + Sentinel 熔断限流
- [ ] 网关 lb:// 负载均衡与服务拆分
- [ ] 配置中心与多实例部署
- **Status:** pending

## Key Questions

1. 测试补齐是否按 TDD 流程逐任务推进（superpowers writing-plans）？
2. 是否引入 Spring Security 全量框架统一鉴权，还是保持现有拦截器方案？
3. 部署目标形态：单机 Docker Compose，还是集群 + 配置中心？
4. 压测目标指标：期望 QPS / P95 RT 是多少？

## Decisions Made

| Decision | Rationale |
|----------|-----------|
| 密码升级 BCrypt + 登录时惰性迁移 | 用户无感知；2026-08-02 已实测旧哈希自动迁移成功 |
| 仅引入 spring-security-crypto | 需要 BCrypt 编码器，无需整套 Spring Security |
| 前端测试使用 browser-use（CDP 控制本机 Chrome） | 需要真实点击/输入/登录态，普通 HTTP 请求无法覆盖 |
| 规划文件放项目根目录（task_plan.md 等） | planning-with-files 约定：文件系统即持久化工作记忆 |
| 前端直连后端 8101（vite 代理） | 现有 vite 配置；网关 8080 仅作 /api 统一入口 |

## Errors Encountered

| Error | Attempt | Resolution |
|-------|---------|------------|
| 后端 8101 挂起（HikariPool 线程饥饿告警，请求无响应） | 1 | 停止旧进程，重启新 jar（含 BCrypt 代码） |
| mvn package 无法 repackage（jar 被运行中进程占用） | 1 | 停止占用 jar 的后端进程后重试成功 |
| browser-use 子代理消息不送达（PROBE 均无响应） | 1 | 改本会话内联执行 / 直接控制浏览器 |
| PowerShell 管道中文乱码 | 1 | 脚本内使用 \uXXXX 转义，避免中文走 stdin |
| mysql -p123456 短参数被 PowerShell 解析错误 | 1 | 改用 --password= 长参数形式 |
| CDP DOM.getBoxModel backendNodeId 反序列化失败 | 1 | 改用 JS getBoundingClientRect 取坐标 |

## Notes

## 2026-08-08 P0 安全与质量改造

- [x] 输出 P0/P1/P2 改造清单并同步 Obsidian 文档
- [x] 修复应用创建/列表的 userId 越权边界
- [x] 收敛 SecretKey 返回范围，列表仅返回脱敏值
- [x] 禁用应用后拒绝签名调用
- [x] 登录 Cookie 增加 SameSite 属性
- [x] 增加应用权限边界和密钥暴露回归测试
- [x] 完成 Maven、前端构建与相关测试
- [x] 提交 Git，并记录提交号：`f69cdb4`

- 更新阶段状态：pending → in_progress → complete
- 2026-08-02 前端测试进展与发现见 findings.md；每次会话记录见 progress.md
- 阶段状态变化时同步更新「Next Step」为单一下一步动作
- 重要决策前重读本计划，避免目标漂移
