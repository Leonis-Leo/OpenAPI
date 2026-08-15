# Findings & Decisions

## Requirements

- 密码哈希升级为 BCrypt（加盐），存量账号无感知迁移 —— 已完成并验证
- 前端功能完整回归测试 —— 进行中
- OpenAPI 项目阶段化路线（本 task_plan.md）—— 已完成

## Research Findings

- **BCrypt 迁移实测成功**（2026-08-02）：旧 SHA-256 哈希（`8d969eef...` = sha256("123456")）登录后自动变为 `$2a$10$...`（60 字符），登录/错误密码/未鉴权接口行为均正确。
- **前端 bug（已排除，2026-08-13 复核）**：应用管理「重置密钥」弹窗 SecretKey 截断问题。复核结论：当前代码不存在截断——`KeyGeneratorUtils.generateKey(32)` 生成 32 字节 → 64 位 hex，加 `SK` 前缀共 **66 位**；「密钥信息」弹窗 `.key-secret` 完整显示 66 位（浏览器实测 textContent 长度 66、`overflow-wrap: anywhere` + `word-break: break-all` 换行 2 行无裁剪）。原「34 位」观察应为旧版本或视觉误读，疑点关闭。
- **后端稳定性隐患（已定位并加固，2026-08-13）**：本机后端 20:39 出现 `HikariPool-1 - Thread starvation or clock leap detected`（机器休眠 31 分钟后），随后 8101 全部请求超时。根因：机器休眠 → 系统时钟跳变（clock leap）→ HikariCP housekeeper 告警；休眠期间连接池中的 MySQL 连接变陈旧，唤醒后借用连接挂起。属本机休眠伪问题，非生产代码缺陷。加固：`application.yml` 显式配置 HikariCP（`keepalive-time` 300s 主动保活空闲连接、`connection-timeout` 10s 快速失败、`validation-timeout` 3s、`max-lifetime` 25min）。
- **本地 admin 密码是 `123456`**（数据库哈希 sha256("123456")），接口文档旧示例写 admin/123456 与 init.sql 的 admin/admin 不一致——文档已更正为 admin，但本地库实际为 123456，仅影响本地演示。
- **环境技巧**：
  - browser-use 安装于 `C:\Users\fjp72\.local\bin\browser-use.exe`（不在 PATH）；`--doctor` 可诊断连接。
  - Element Plus 关闭的 dialog/message-box 仍留在 DOM，判断可见性要用 `offsetParent !== null` 或 `display`。
  - 表格工具栏按钮（复制AK/重置密钥/删除等）需先勾选行复选框才启用。

## Technical Decisions

| Decision | Rationale |
|----------|-----------|
| BCryptPasswordEncoder cost 10（Spring 默认） | 安全与性能均衡，版本由 Boot 3.3.5 托管 |
| 登录时惰性迁移，迁移失败仅告警不影响登录 | 用户无感知；迁移写库失败不阻断登录主流程 |
| 旧格式判定 = 64 位小写十六进制 | 与现有 HexFormat 输出完全一致，无歧义 |
| 前端测试优先 AX 树 + DOM 文本断言，截图存档 | 无视觉模型，断言比截图更可靠 |

## Open Questions

## 2026-08-08 P0 改造发现

- 为保留在线调试能力，新增 `/v1/app/debug-list`，只对当前登录用户开放并返回调试所需 SecretKey；该接口仍需后续加入重新验证密码或短时授权。

- `AppController` 的创建和列表接口直接接收 `userId`，而认证拦截器只保证“已登录”，未保证请求参数中的用户归属；这会造成应用查看和创建的对象级越权。
- `App` 实体在列表、创建、重置等响应中默认序列化完整 `secretKey`；列表接口应改为脱敏响应，完整密钥只在创建/重置成功时一次性返回。
- `SignatureInterceptor` 查询到应用后未检查应用状态，禁用应用仍可能通过签名校验；需要在签名校验阶段拒绝非启用应用。
- 登录 Cookie 当前仅设置 HttpOnly/Path，补充 SameSite=Lax；Secure 是否开启由部署环境配置决定。

- ~~重置密钥弹窗截断是否为真 bug、影响面多大（复制按钮行为）？~~ 已复核排除（2026-08-13）：弹窗完整显示 66 位，复制按钮复制完整明文。
- ~~是否需要对 invoke_log 增加按时间清理的定时任务？~~ 已完成（2026-08-13）：`InvokeLogCleanupTask` 每天凌晨 3 点按 `openapi.log.retention-days`（默认 30 天）清理，聚合统计表永久保留。
- 压测目标指标（QPS / P95 RT）由谁定？

## 2026-08-15 TraceId 链路追踪发现

- **竞品结论**：高星项目主要分两类——网关插件/Agent 无侵入（Kong / APISIX / ShenYu）和 Spring 生态原生观测（Spring Cloud Gateway + Micrometer Tracing）。本项目第一阶段采用轻量 `MDC + X-Trace-Id` 透传最合适，避免引入 Agent / OTel Collector。
- **TraceId 格式**：约定 32 位小写 hex（UUID 去连字符），入口优先透传合法 Header，否则生成。
- **关键接入点**：
  - 网关 `TraceIdGlobalFilter` 负责入口生成/透传。
  - 数据面 / 控制面用 `OncePerRequestFilter` 恢复 MDC，过滤器需 `@Order(HIGHEST_PRECEDENCE)`。
  - RabbitMQ 用 `MessagePostProcessor` 写 header，消费端用 `@Header` 读取并恢复 MDC。
- **注意**：Spring Cloud Gateway 是 WebFlux，MDC 不会自动沿 Reactor 链传播；网关侧只对入口日志做 MDC 写入/清理，下游由 Servlet 服务自行恢复。
- **响应头重复 `X-Trace-Id`**：初版网关与数据面都会设置，导致值为 `id,id`；已改为只由网关返回给客户端，数据面仅恢复 MDC 与请求属性。

## 2026-08-15 RabbitMQ 可靠投递与敏感脱敏发现

- RabbitMQ 队列参数变更不能对已存在队列直接重声明，会报 `PRECONDITION_FAILED`；本地开发可删除旧队列后重建，生产需规划迁移策略。
- 消费幂等采用 `messageId + Redis key`，成功后再标记；若处理失败则重试，避免提前标记导致消息丢失。
- 审计日志导出若逐条关联用户会产生 N+1 查询，数据量稍大就会超时；应按 `userId` 批量查询再组装。
- 敏感脱敏统一在 `openapi-common` 做 JSON 递归掩码，数据面请求参数/请求头/响应体与审计详情复用同一策略。
