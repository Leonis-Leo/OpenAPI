# Progress Log

## Session: 2026-08-02

## Session: 2026-08-08

### P0 安全与质量改造

- **Status:** complete
- 已完成 P0/P1/P2 优先级清单，并新增 Obsidian 文档 `docs/01-项目文档/P0-安全与质量改造清单.md`。
- 已修复应用创建/列表的 userId 越权，应用列表 SecretKey 脱敏，新增 debug-list/admin-list 的明确边界接口。
- 已增加禁用应用签名拒绝、SameSite=Lax Cookie，以及应用权限/密钥暴露回归测试。
- 验证通过：`mvn test -q`、`npm.cmd run build`、`git diff --check`。
- 构建保留既有 Vite bundle 体积警告，未新增编译错误。
- Git 提交：`f69cdb4 fix: harden application ownership and secret exposure`。

### 首页概览重设计（数据大盘）— 2026-08-02 晚间
- **Status:** complete（实现与验证；浏览器 E2E 按用户要求跳过）
- 流程：brainstorming → spec（docs/superpowers/specs/2026-08-02-homepage-overview-redesign-design.md）→ writing-plans（docs/superpowers/plans/2026-08-02-homepage-overview-redesign.md）→ 执行
- 执行方式：子代理驱动尝试失败（环境问题，子代理只返回空闲问候），切换 executing-plans 内联执行，7 个任务全部完成
- 交付：
  - 数据模型 dashboard-model.ts + 零依赖单元测试（node --test，5/5，commit a233030）
  - 组件 StatCard / TrendChart / RankList(TOP10) / RecentLogs / QuickStart（src/components/dashboard/）
  - DashboardView 容器重写：角色感知（admin 全量大盘 / 用户个人视图）、模块级加载/错误/空态、暗色自适应、响应式
- 验证：node --test 5/5 通过；npm run build（vue-tsc + vite）零错误
- 已知 Minor（deferred）：roundRate 未被组件复用；loadCards 无错误态 UI

### Phase 2 收尾：BCrypt 密码加盐
- **Status:** complete
- **Started:** 2026-08-02 20:00
- Actions taken:
  - 走 brainstorming → writing-plans → 内联执行流程，产出设计文档与实施计划（docs/superpowers/）
  - 4 个提交：PasswordUtils BCrypt 改造 + 单测、UserServiceImpl 惰性迁移、init.sql 演示数据、CI/文档
  - 全量 `mvn test` 通过（6/6 单测）；已推送 origin/feature_dev（69eebf1），CI 待观察
- 验证：本机旧 SHA-256 账号登录后自动迁移为 BCrypt，错误密码正确拒绝

### Phase 3 进行中：前端回归测试
- **Status:** in_progress
- **Started:** 2026-08-02 21:00
- Actions taken:
  - 用 browser-use（CDP）控制本机 Chrome 测试前端 http://localhost:5173
  - 概览页：统计卡片/快捷入口/暗色切换正常
  - 应用管理：列表加载、新建应用成功（UI-TEST-85677758）、重置密钥弹窗展示新 SK
  - 发现：后端 8101 挂起导致「网络异常」→ 已重启新 jar 恢复
  - 发现（疑似 bug）：重置密钥弹窗 SK 显示截断，待复核
- 遗留：接口管理/订阅审批/统计/日志/限流/用户管理页面测试未完成

## Session: 2026-08-08 UI redesign

- 按现代 SaaS 方向重构 `AdminLayout.vue`：分组导航、工作区切换、环境状态、通知、用户菜单和响应式折叠。
- 增加全局设计变量、卡片、表格、按钮、暗色模式基础样式。
- 重做 `LogManage.vue`：调用量/成功率/平均耗时/异常请求概览卡片，筛选工具栏，卡片化日志表格和详情抽屉。
- 验证：`npm.cmd run build` 通过；仅保留既有 Vite 大包体积提示。

### Session: 2026-08-08 UI polish follow-up

- 概览/统计页增加低数据图表空态、单日摘要和紧凑图表高度。
- 排行卡片增加相对调用量进度，列表页统一批量操作栏和表格密度。
- SecretKey 增加行内复制入口，技术字段统一等宽字体，补充 hover、分页、空状态细节。
- 验证：`npm.cmd run build`、`git diff --check` 通过。
