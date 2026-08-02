# Findings & Decisions

## Requirements

- 密码哈希升级为 BCrypt（加盐），存量账号无感知迁移 —— 已完成并验证
- 前端功能完整回归测试 —— 进行中
- OpenAPI 项目阶段化路线（本 task_plan.md）—— 已完成

## Research Findings

- **BCrypt 迁移实测成功**（2026-08-02）：旧 SHA-256 哈希（`8d969eef...` = sha256("123456")）登录后自动变为 `$2a$10$...`（60 字符），登录/错误密码/未鉴权接口行为均正确。
- **前端 bug（疑似）**：应用管理「重置密钥」弹窗展示的 SecretKey 疑似被截断——弹窗显示 34 位（`SKbe2763373ab944507a9fce8febdff881`），数据库实际 66 位（`SKbe2763373ab944507a9fce8febdff8810b3eec9fac863178a249e6e4f3412c96`）。若"复制"按钮复制的是截断值，会导致开发者拿到无效密钥。**待复核**：打开弹窗核对完整 innerText 与复制内容。
- **后端稳定性隐患**：本机后端 20:39 出现 `HikariPool-1 - Thread starvation or clock leap detected`（机器休眠 31 分钟后），随后 8101 全部请求超时（GET/POST 均无响应），前端表现为「网络异常」。重启后恢复。生产需关注线程池与机器休眠/时钟跳变。
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

- 重置密钥弹窗截断是否为真 bug、影响面多大（复制按钮行为）？
- 是否需要对 invoke_log 增加按时间清理的定时任务？
- 压测目标指标（QPS / P95 RT）由谁定？
