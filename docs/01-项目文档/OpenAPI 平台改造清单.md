# OpenAPI 平台改造清单（P0/P1/P2 待办清单）

> 本文档由《P0-安全与质量改造清单》与《P0-P2-参考项目增补清单》融合而成。**⚠️ 2026-08-13 起 P0/P1/P2 待办已并入根目录 `task_plan.md`（唯一计划来源）**，本文档保留「参考项目速查」与「推荐实施顺序」供查阅。
> 参考项目（本地克隆）：`E:\MyProject\OepnAPI-study\APIPark` 与 `E:\MyProject\OepnAPI-study\references\`（8 个高星项目，索引见该目录 README.md）。

## 优先级说明

- **P0：必须优先完成**：涉及越权、密钥泄露、鉴权失效、数据正确性和生产安全基线。
- **P1：核心能力增强**：影响平台可运营性、开发者效率和日常管理效率。
- **P2：中长期演进**：商业化、多租户、服务治理和规模化部署能力。

## 参考项目速查

| 参考项目 | 学习重点 |
|---|---|
| `references\NEAZE-open-platform` | OAuth2 授权码/客户端凭证、应用接入审批、授信管控 |
| `references\apache-shenyu` | 网关插件 SPI 链、SignPlugin / RateLimiterPlugin、请求/响应改写 |
| `references\EhsanTang-ApiManager` | 接口表结构、Word/PDF 文档导出、在线调试 |
| `references\wso2-product-apim` | API 生命周期状态机、订阅工作流、OAuth2 应用、限流分级 |
| `references\OpenAPITools-openapi-generator` | 多语言 SDK 生成、mustache 模板定制 |
| `references\springdoc-springdoc-openapi` | securitySchemes 展示 AK/SK、文档分组（已在用） |
| `references\xiaoymin-knife4j` | 在线调试、文档增强（已在用） |
| `references\Brotherc-openplatform` | 文档中心 + API 中心布局 |

## P0：安全与正确性

- [x] 应用创建接口不再接收客户端 `userId`，始终使用当前登录用户。
- [x] 应用列表接口不再接收客户端 `userId`，避免越权查看其他用户应用。
- [x] 应用列表和详情不返回完整 SecretKey，仅返回脱敏提示。
- [x] 在线调试改用独立调试应用列表接口，避免普通管理列表默认暴露密钥。
- [x] 创建应用和重置密钥时一次性返回完整 SecretKey。
- [x] 禁用应用后拒绝签名调用。
- [x] 登录 Cookie 增加 `SameSite=Lax`。
- [x] 增加应用归属边界和密钥暴露回归测试。
- [x] SecretKey 重置增加重新验证密码或二次确认。
- [x] Redis 不可用时 nonce 防重放不再静默放行。
- [x] 补齐登录限流、失败锁定、JWT 注销和 CSRF 防护。
- [x] 数据库密码、JWT 密钥和 Cookie 安全配置改为环境变量。
- [x] 增加管理员操作审计日志。
- [ ] 生产配置启动自检：启动时检查 JWT 密钥、数据库密码、Redis、RabbitMQ 和 Cookie Secure 配置，发现默认凭据时告警或拒绝生产启动。

## P1：平台能力与体验

- [x] 应用、接口、订阅、用户列表统一服务端分页、搜索、筛选和导出。
- [x] 在线调试支持 JSON Body、Header 编辑、请求历史和多语言代码示例。
- [x] API 分组、标签、版本和 OpenAPI 导入/导出。
- [x] 支持 OpenAPI JSON/YAML 导入校验并自动生成接口文档。
- [x] 增加发布版本、变更 diff 和一键回滚（灰度发布后续补充）。
- [ ] 应用配额、订阅有效期、Scope 和 IP 白名单。
- [ ] 应用成员协作和更细粒度 RBAC。
- [ ] 首页增加时间范围、应用/接口筛选、P95 延迟和限流指标。
- [ ] 增加 TraceId、RabbitMQ 重试/死信、日志归档和异常告警。
- [ ] 增加服务目录、标签、开发者搜索和订阅申请入口。（参考 APIPark / Brotherc-openplatform）
- [ ] 增加上游服务配置、超时、重试、健康检查和熔断降级。（参考 APIPark / wso2-product-apim）
- [ ] 增加请求/响应敏感字段脱敏策略及审计日志查询导出。
- [ ] 增加按应用、用户、IP、接口、方法和路径维度的策略中心。（参考 apache/shenyu 插件链）
- [ ] API 生命周期状态机：接口状态升级为 CREATED → PROTOTYPED → PUBLISHED → DEPRECATED → RETIRED 多态，配合版本快照与回滚。（参考 wso2/product-apim）
- [ ] 接口文档导出增强：Word/PDF 导出、模块化分组与数据库表文档。（参考 EhsanTang/ApiManager）
- [ ] 证书管理：统一管理 HTTPS/TLS 证书、过期时间与续期提醒，网关按域名绑定。

## P2：规模化与商业化

- [ ] 多租户隔离。
- [ ] 测试环境与生产环境分离。
- [ ] API 套餐、配额计费、订单和账单。
- [ ] 服务发现、配置中心、多实例部署和灰度发布。
- [ ] 团队、组织、成员和资源级 RBAC，逐步演进到多租户隔离。
- [ ] API Key、Basic、JWT、OAuth2 等多认证方式。
- [ ] OAuth2 授权码/客户端凭证流程：作为 AK/SK 之外的第三方应用认证方式，含应用接入审批、token 签发/刷新、Scope 授信管控。（参考 NEAZE/open-platform）
- [ ] 网关插件 SPI 化重构：把拦截器/过滤器抽象为插件链 + SPI 接口，统一上下文传递。（参考 apache/shenyu，仅借鉴思想）
- [ ] 多语言 SDK 自动生成：Java/Python/Go/TS 等，作为独立服务引入。（参考 OpenAPITools/openapi-generator）
- [ ] AI Provider、模型 Key、余额和 Token 成本管理。
- [ ] MCP 工具暴露、协议转换插件和可插拔网关策略。
- [ ] 控制面/数据面分离、集群节点健康检查和配置版本下发。
- [ ] 国际化与开放平台门户：多语言包 + 面向开发者的开放 API 门户。

## 推荐实施顺序

`版本发布/回滚`（已完成）→ `上游与超时重试` → `熔断与脱敏` → `审计查询导出` → `API 生命周期状态机` → `服务目录/OpenAPI 导入` → `策略中心/灰度` → `团队 RBAC/多租户` → `文档导出/插件 SPI` → `OAuth2/多认证` → `SDK 生成` → `AI/MCP/协议插件`。

## 暂不建议直接照搬的内容

- APINTO、EOSC、NSQ、InfluxDB、Loki 等基础设施：当前项目已有 Spring Boot、RabbitMQ、Redis 和 MySQL，先补能力抽象，避免无明确指标的中间件迁移。
- ShenYu 整体框架：不整体引入，只借鉴其插件链 + SPI + 上下文传递的设计思想，落到现有 Spring Cloud Gateway 的过滤器体系。
- OpenAPI Generator：不内嵌进主工程，作为独立服务或参考其模板引擎，避免引入庞大的多语言样本与模板仓库。
- 35+ AI Provider：只有在确定平台转向 AI 网关后再建设，当前优先完善通用 API 生命周期。
- 全量插件市场：先抽象策略接口并落地 3~5 个高价值策略，再考虑插件动态加载。

## 本轮实施记录

- 范围：P0 第一批安全基线。
- 状态：代码、测试和文档已完成，已提交 Git（`f69cdb4`）。
- 关联记录：项目根目录 `task_plan.md`、`findings.md`、`progress.md`。
