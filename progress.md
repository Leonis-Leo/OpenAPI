# Progress Log

## Session: 2026-08-02

## Session: 2026-08-08

### 订阅审批列表不刷新 + 右上角通知功能（2026-08-08）
- **Status:** complete
- 定位：`interface_subscribe` 已有 status=0 数据，但“订阅审批-待审批”页显示“暂无数据”。根因是 `AdminLayout.vue` 用 `<keep-alive>` 缓存页面，订阅审批/接口管理页重新进入时不会重新请求，停留在旧数据。
- 修复：`SubscribeManage.vue`、`InterfaceManage.vue` 由 `onMounted` 改为 `onActivated(load)`，每次页面激活都拉取最新数据。
- 通知功能：
  - 新增 `notification` 表（`db/migrations/2026-08-08-notification.sql`，已应用到本地库，同步更新 `db/init.sql`）。
  - 后端新增 `Notification` 实体/Mapper/Service/Controller（分页、未读数、单条已读、全部已读），订阅申请时通知所有管理员，审批通过/拒绝时通知申请人，并与状态变更同事务。
  - 前端 `AdminLayout.vue` 铃铛改为未读数角标 + 通知面板（点开加载列表、点击单条标记已读并跳转、全部已读、30 秒轮询未读数）。
- 验证：后端接口全链路测试通过（订阅触发通知、已读、审批通知）；`mvn -pl openapi-backend -am package` 与 `npm run build` 通过；测试数据已清理。

### 通知中心与弹窗容量（2026-08-08 跟进）
- **Status:** complete
- 弹窗固定只拉 10 条（原 20 条），底部新增“查看全部”入口，避免通知在弹窗内无限堆积。
- 新增通知中心页 `/notifications`（`NotificationCenter.vue`）：全部/未读筛选、分页、批量标记已读、删除、清空。
- 后端新增 `read` 筛选参数及删除/清空接口；`clearAll`/`delete` 仅作用于当前用户（逻辑删除）。
- 验证：`npm run build`、`mvn package` 通过；删除/清空/筛选接口已实测；测试数据已清理。

### 首页“API 生命周期”重新设计（2026-08-08）
- **Status:** complete
- 重写 `QuickStart.vue` 为“API 生命周期”卡片：六阶段流程（创建应用 → 发布接口 → 订阅申请 → 审批通过 → 签名调用 → 运营分析），带实时计数/状态（完成、待审批、未开始），按角色自适应文案与可访问性（非管理员“运营分析”步骤标注仅管理员并禁用）。
- 下方“快速使用”三步指引 + 快捷入口；管理员/普通用户入口按角色过滤。
- `DashboardView.vue` 调整布局：生命周期卡片整行置于统计/趋势/日志下方，计数 props 来自已有统计接口。
- 验证：`npm run build` 通过（vue-tsc + vite）。

### 首页卡片排版调整（2026-08-08 跟进）
- **Status:** complete
- 生命周期卡片上移至统计卡下方作为主视觉；底部“最近调用动态 + 快速使用”左右分栏。
- 从生命周期卡拆出“快速使用/快捷入口”为独立 `QuickAccess.vue` 侧边卡，卡片职责更清晰；普通用户视图下方整行展示。
- 验证：`npm run build` 通过。

### 快速使用上移为顶部横条（2026-08-08 跟进）
- **Status:** complete
- 用户反馈“快速使用放下面看不到”：将 `QuickAccess.vue` 改为紧凑横条卡（三步指引 + 快捷入口），置于页面标题下方、统计卡上方，一屏内直接可见。
- 管理员底部恢复“最近调用动态”整行；生命周期卡保持在统计卡下方。
- 验证：`npm run build` 通过。

### 首页新增卡片与排版优化（2026-08-08 跟进）
- **Status:** complete
- 新增卡片（真实数据）：
  - `TodoPanel.vue` 待办事项（管理员）：待审批订阅列表 + 未读通知提醒。
  - `LatestNotifications.vue` 最新通知：最近 3 条，点击标记已读并跳转。
  - `RateLimitStatus.vue` 限流状态（管理员）：接口/应用限流配置覆盖率进度条。
- 新增占位卡 `ComingSoonCard.vue`（“即将上线”）：监控告警、API 文档与 SDK、压测报告，后续可填充。
- 排版：趋势+排行 / 日志+待办 分栏；通知+限流 两列；即将上线 三列；普通用户视图：统计卡+生命周期+最新通知+即将上线。
- 验证：`npm run build` 通过。

### P1：列表导出补齐 + 接口发布版本/一键回滚（2026-08-08）
- **Status:** complete
- P1 清单第 1 项收尾：应用管理、接口管理补充“导出 CSV”（订阅/用户此前已有），统一分页/搜索/筛选/导出全部完成，清单已打勾。
- P1 清单第 2 项（在线调试 JSON Body/Header/历史/多语言示例）确认已完成，清单已打勾。
- 按推荐顺序实现 P1“发布版本/一键回滚”：
  - 新增 `interface_version` 表（迁移 `db/migrations/2026-08-08-interface-version.sql`，已应用并同步 `init.sql`）。
  - 后端：`InterfaceVersion` 实体/Mapper/Service；上线发布、在线更新自动生成版本快照；`GET /v1/interface/versions` 版本列表、`POST /v1/interface/rollback` 一键回滚（回滚本身也生成快照留痕）。
  - 前端：接口管理新增“版本历史”按钮与弹窗（版本列表、发布状态、当前版本标记、与当前接口的字段差异、一键回滚）。
- 验证：后端全链路实测（发布 v1 → 在线更新 v2 → 回滚 v1 还原并生成 v3）；`mvn package`、`npm run build` 通过；测试数据已清理。
- P1 清单第 10 项已勾选，灰度发布标注“后续补充”。

### P1：接口分组、标签 + OpenAPI 导入导出（2026-08-08）
- **Status:** complete
- 新增 `interface_group` / `interface_tag` / `interface_tag_relation` 表，`interface_info` 增加 `group_id`（迁移 `db/migrations/2026-08-08-interface-group-tag-openapi.sql`，已应用并同步 `init.sql`）。
- 后端：分组/标签 CRUD（含接口数统计、占用校验）；接口分页支持 groupId/tagId 筛选；接口详情/列表富化 groupName + tags；`POST /v1/interface/openapi/import`（JSON/YAML 解析、按路径+方法导入、自动生成请求参数/响应示例、跳过重复）、`GET /v1/interface/openapi/export`（json/yaml）。
- 前端：接口管理页新增分组/标签筛选、表格分组/标签列、表单分组/标签选择、分组标签管理弹窗、导入 OpenAPI 弹窗（粘贴/选文件）、导出 JSON/YAML。
- 验证：分组/标签创建、YAML 导入（含 x-group/tags/参数/响应示例）、标签筛选、JSON/YAML 导出均实测通过；`mvn package`、`npm run build` 通过；测试数据已清理。
- P1 清单第 3、12 项已勾选。

### 网络异常页跳转与提示优化（2026-08-08）
- **Status:** complete
- 问题：后端不可达时 Vite 代理返回 500+HTML，被当成普通 HTTP 错误，每个请求各弹一条“网络异常” toast 堆叠。
- 修复（`openapi-web/src/utils/request.ts`）：
  - 非 JSON 错误体（代理/网关 HTML）按网络异常处理，统一跳转 `/network-error`，只跳转一次（模块内防重）。
  - 普通错误 toast 去重（同消息 3 秒内只弹一次）；未知状态码提示“请求失败（xxx）”而非误导性的“网络异常”。
  - 通知轮询等后台请求加 `skipNetworkRedirect`，失败静默不跳转。
- 验证：`npm run build` 通过。

### 接口管理按钮排版优化：互斥操作合并（2026-08-08）
- **Status:** complete
- 用户反馈“按钮太多”：批量操作栏将互斥操作合并为自适应按钮：
  - 订阅/取消订阅 → 单个按钮（全选可订阅时显示“订阅”，全选已订阅时显示“取消订阅”，状态混杂时禁用“订阅/取消”）。
  - 上线/下线 → 单个按钮（按选中接口状态自适应文案与颜色，混杂时禁用）。
- 工具栏导出收敛：导出 OpenAPI JSON / YAML + 接口 CSV 合并为“导出”下拉，减少按钮数量；分组标签改为文字按钮弱化视觉。
- 验证：`npm run build` 通过。

### 接口管理改为左树右表布局（2026-08-08）
- **Status:** complete
- 用户要求“有分组就改成左树右表”：接口管理页左侧新增分组树面板（全部接口/各分组/未分组，带数量），右侧为筛选、批量操作与表格。
- 后端：`GET /v1/interface/groups` 返回 `{groups, total, ungrouped}`；分页接口新增 `ungrouped=true` 筛选（groupId IS NULL）。
- 前端：移除工具栏分组下拉（由左侧树承担）；树节点点击筛选表格，支持全部/分组/未分组；窄屏下分组树变为横向排列。
- 验证：分组树数据、未分组筛选、分组筛选均实测通过；`mvn package`、`npm run build` 通过；后端已重启。

### 接口管理页重新设计（frontend-ui-engineering，2026-08-08）
- **Status:** complete
- 用户反馈“按钮和留白太多、上下不一致、按钮与过滤条件同行”：按 frontend-ui-engineering 技能重新分区：
  - 标题行：仅放页面级操作（分组标签 / 导入 OpenAPI / 导出下拉 / 新增接口）。
  - 过滤行：状态 Tab + 搜索 + 标签筛选，不再与按钮混排。
  - 批量操作行：统一 small plain 按钮（详情/调试、订阅、上线/下线、编辑、版本历史，删除靠右），去掉“批量操作：”标签前缀，间距收紧（8px/10px 内边距、12px 间隙）。
  - 移除原 toolbar 底部边框与 18px 大留白。
- 验证：`npm run build` 通过。

### 修复：全页面分页控件消失（2026-08-08）
- **Status:** complete
- 现象：所有页面的 `el-pagination` 渲染为空。
- 根因：node_modules 中 Element Plus 实际为 2.14.3（package.json 为 ^2.9.1）。2.14 起 Pagination 增加严格校验 `isAbsent = (v) => typeof v !== "number"`，不满足时直接 return null；后端 Jackson 全局把 Long 序列化为字符串（防雪花 ID 精度丢失），`total` 以 `"10"` 字符串返回，导致校验失败、分页整体不渲染。
- 修复：所有分页页面对 `total` 做 `Number(...)` 强转（AppManage / InterfaceManage / SubscribeManage×3 / UserManage / NotificationCenter；LogManage 已有、RateLimitManage 为本地计数）。
- 验证：浏览器实测 /users(共16条)、/apps(共5条)、/subscribes(共11条)、/interfaces(共11条) 分页全部恢复；`npm run build` 通过。

### 分页移入按钮区右侧 + 接口分组层级树（2026-08-08）
- **Status:** complete
- 分页布局：全部列表页分页控件移入批量操作按钮区最右侧（`bar-left` 按钮组居左 + `bar-pagination` 靠右、`size="small"` 紧凑）；去掉“批量操作：”前缀；涉及 AppManage / InterfaceManage / SubscribeManage×3 / UserManage / LogManage / NotificationCenter / RateLimitManage×2，共享样式收进 `base.css`。
- 接口分组层级化：
  - `interface_group` 新增 `parent_id`（迁移 `db/migrations/2026-08-08-interface-group-parent.sql`，已应用并同步 `init.sql`）。
  - 后端：`groupTree()` 返回嵌套树 `{tree, total, ungrouped}`；新建分组支持 `parentId`；删除分组校验子分组与接口占用。
  - 前端：左树改为 `el-tree`（可展开/折叠、当前节点高亮）；新增分组搜索框，命中后过滤并展开路径、聚焦叶子节点（Element Plus 2.14 无 `expand` 方法，改为直接设置节点 `expanded`）。
  - 左树可拖拽缩放（160~360px，宽度持久化到 localStorage）；分组管理弹窗支持选择父分组创建子分组、显示父分组列。
- 验证：层级树接口实测（父/子分组、接口计数）；浏览器实测搜索过滤+聚焦叶子、清空恢复、拖拽缩放持久化；`mvn package`、`npm run build` 通过；测试数据已清理。

### 分组面板拖拽手柄显性化 + 隐藏/展开左树（2026-08-08）
- **Status:** complete
- 拖拽手柄改为可见：面板与内容区间一条竖线 + 中间圆点抓手，悬停高亮变蓝，拖拽缩放 160~360px 仍记忆宽度。
- 分组面板头部新增收起按钮（Fold 图标）；收起后内容区左侧出现“展开分组面板”竖条按钮（Expand 图标）可恢复。
- 收起/展开状态持久化到 localStorage（`openapi-group-panel-visible`）。
- 验证：DOM 实测收起→面板隐藏/展开按钮出现/记忆保存，展开→面板恢复；`npm run build` 通过。（后续浏览器验证由用户负责）

### 按钮风格统一为现代 SaaS（2026-08-08）
- **Status:** complete
- 用户要求统一按钮风格并提供风格图选择，最终选择“现代 SaaS”风格，全局落地于 `base.css`：
  - 次级/批量按钮统一「白底 + 细灰描边 + 深灰文字」，hover 轻微加深；主操作（新增/新建等）唯一实心蓝。
  - 语义色统一为描边弱化（上线绿、删除红、警告黄），禁用态统一浅灰无边框、`opacity:1`。
  - 筛选 Tab 改为分段控件（灰底轨道 + 白色浮起选中段）。
  - 移除原 action-bar 内按钮的特殊覆盖（action-primary/action-secondary/danger-right 等），全部走统一体系。
- 预览页 `docs/ui-button-style-preview.html` 新增“方案 D · 现代 SaaS（推荐）”，保留 A/B/C 供对比。
- 验证：`npm run build` 通过；刷新页面生效。

### 调用统计持久化（与明细日志解耦，2026-08-09）
- **Status:** complete
- 新增聚合表 `invoke_stats_daily`（按天：日期/应用/接口 唯一）与 `invoke_stats_counter`（累计计数），永久保留。
- `InvokeLogConsumer` 在同一事务（TransactionTemplate）内：写明细日志 + upsert 当日聚合 + 累计计数；消息带 `createTime` 避免跨日偏差。
- 统计接口（overview/daily/top-interfaces/top-apps）全部改读聚合表，不再 GROUP BY invoke_log。
- 迁移脚本回填历史日志（先清空再按明细聚合）；`db/init.sql` 同步新表。
- 验证：发调用后 overview/daily/top 正常；删除明细日志后统计总数不变（8/8）；为多租户计费预留 (stat_date, app_id, interface_id) 维度。

### 调用统计页新增“调用明细”列表 + 日志联动（2026-08-09）
- **Status:** complete
- 统计页新增“调用明细”卡片：按天 / 按应用 / 按接口三个维度切换、分页、成功率/平均耗时列。
- 点击行（或“查看日志”）跳转 API 日志页，URL 带 date/appId/interfaceId，日志页自动过滤当天/应用/接口，并显示可清除的过滤标签。
- 后端：`GET /v1/stats/daily-page`（聚合表分页，dimension=day/app/interface，支持日期/应用/接口过滤）；`/v1/log/list` 新增 appId/interfaceId 过滤。
- 验证：三维度接口、日期+接口日志过滤均实测通过；`mvn package`、`npm run build` 通过。

### 调用统计页精简 + 明细独立页 + 左侧菜单分类重构（2026-08-09）
- **Status:** complete
- 修复统计页趋势图空白：StatsView 的 echarts 补齐 keep-alive 激活 resize、ResizeObserver 与渲染容错（与仪表盘趋势图一致）。
- 调用明细从统计页拆为独立页面 `/stats/detail`（`StatsDetailView.vue`），统计页只保留概览卡 + 趋势 + 排行，避免数据量大时页面卡顿。
- 左侧菜单分类重构，新增未来功能占位（点击进入“规划中”占位页 `ComingSoonView.vue`）：
  - 资源管理：+ 服务目录
  - 运营分析：+ 调用明细（真实）、监控告警
  - 开放治理（新分类）：策略中心 / 灰度发布 / 敏感脱敏 / API 文档
  - 平台设置：+ 团队与权限 / 多租户 / 计费账单
  - 开发者中心（新分类）：SDK 与示例 / 压测报告
  - 占位菜单带“规划中”徽标，路由 `/coming-soon?name=&desc=`。
- 验证：`npm run build` 通过；统计明细接口此前已实测。

### API 日志筛选区重设计（2026-08-09）
- **Status:** complete
- 原筛选区控件左对齐不统一、日期选择换行错位、按钮位置游离。
- 重做为 SaaS 标准筛选模式：标题“筛选条件”+ 右上角“重置/查询”；字段用带顶部标签的响应式网格（结果/状态码/时间范围/关键词，auto-fit 自适应列数），控件宽度统一 100%；URL 联动过滤标签独立成行。
- 新增“重置”按钮一键清空全部筛选（含 URL 带来的日期/应用/接口）。
- 验证：`npm run build` 通过。

### 筛选区改轻量内联筛选条（2026-08-09 迭代）
- **Status:** complete
- 用户反馈卡片式筛选区“太丑”：改为无卡片的内联筛选条（`CollapsibleFilter.vue`）：
  - 常用条件（搜索/结果等）内联一行，占位符代替标签，控件紧凑并排；
  - 次要条件（状态码/时间范围等）收进“更多筛选 ▾ / 收起 ▴”；
  - 右侧固定“重置 / 查询”，已选条件胶囊独立一行；
  - 应用到 API 日志、应用管理、用户管理、订阅审批、调用明细五个列表页。
- 验证：`npm run build` 通过。

### API 日志 / 接口管理按钮区与筛选区对齐应用管理标准（2026-08-09）
- **Status:** complete
- API 日志：移除表格上方左侧“调用记录 共 X 条”标题，改为与应用管理一致的全宽按钮栏（按钮靠左：查看详情/删除/清空日志，分页靠右）。
- 接口管理：旧式 tabs+搜索+标签 筛选行改为标准 `CollapsibleFilter` 筛选条（tabs/搜索/标签靠左，重置/查询靠右），新增 applyFilters/resetFilters。
- 验证：`npm run build` 通过。

### 调用明细页统一标准布局（2026-08-09）
- **Status:** complete
- 调用明细页按应用管理标准重排：标题区（标题+副标题+刷新）、标准筛选条（维度 tabs + 日期范围，右侧重置/查询）、表格卡片（无多余头部）、分页右下。
- 验证：`npm run build` 通过。

### 批量按钮分组优化 + 状态切换收进“更多筛选”（2026-08-09）
- **Status:** complete
- 应用管理：导出改为 CSV/JSON 下拉；批量栏重排为 复制AK/重命名/启用禁用(自适应)/更多▾(重置密钥)，删除红字靠右，已选胶囊独立。
- 用户管理：角色（设为管理员/普通用户）与启用/禁用均改为自适应按钮；重置密码收进“更多▾”。
- 接口管理：全部/已上线/已订阅/未订阅 tabs 从主筛选条收进“更多筛选”；调用明细：按天/按应用/按接口 维度切换同样收进“更多筛选”。
- 验证：`npm run build` 通过。

### 筛选控件轻量化：胶囊分段改下拉 + 日期控件收紧（2026-08-09）
- **Status:** complete
- 用户反馈胶囊式分段控件（按天/按应用/按接口）突兀、日期选择器过长：
  - 调用明细：维度切换改为 130px 下拉；日期范围改为 240px 短占位（开始/结束）。
  - 接口管理：全部/已上线/已订阅/未订阅 改为“状态下拉”（130px），与搜索、标签下拉并排。
- 验证：`npm run build` 通过。

### 日期控件改为“预设下拉 + 自定义”（2026-08-09）
- **Status:** complete
- 用户反馈日期控件仍过长：调用明细默认只显示 130px 预设下拉（全部时间/近7/30/90天/自定义），选“自定义”才展开日期范围；API 日志时间同理（全部时间/近1小时/近24小时/近7天/自定义）。
- 验证：`npm run build` 通过。

### 页面操作按钮全部并入按钮区（2026-08-09）
- **Status:** complete
- 规则：标题行只放标题，除筛选区（重置/查询）外所有按钮统一进按钮区（应用管理为例：导出/新建应用并入批量栏左侧）。
- 涉及页面：应用管理（导出下拉/新建应用）、接口管理（分组标签/导入/导出/新增接口）、用户管理（导出/新增用户）、订阅审批（导出×3标签页）、API 日志（导出）、调用明细（导出/刷新，分页并入按钮栏）、通知中心（全部已读/清空）。
- 验证：`npm run build` 通过。

### 筛选区加卡片底色 + 接口页按钮区分行防分页挤压（2026-08-09）
- **Status:** complete
- `CollapsibleFilter` 统一加白色卡片底色与边框（所有列表页筛选区自动生效）。
- 接口管理按钮区改为单卡片两行：第一行页面操作（分组标签/导入/导出/新增接口），第二行批量操作+已选/删除+分页，分页固定右侧不再被挤下。
- 验证：`npm run build` 通过。

### API 日志 / 调用明细按钮区与表格卡片分离（2026-08-09）
- **Status:** complete
- 两页按钮栏原嵌在表格卡片内，改为独立卡片：筛选卡片 → 按钮卡片（导出/批量/分页） → 表格卡片。
- 验证：`npm run build` 通过。

### 修复筛选区残留虚线 + 限流配置筛选区补卡片底色（2026-08-09）
- **Status:** complete
- 移除筛选组件“已选条件”区域的虚线分隔（空内容时不再出现多余虚线）。
- 限流配置两个 Tab 的搜索框包进 `CollapsibleFilter`，补齐卡片底色与重置/查询。
- 验证：`npm run build` 通过。

### 设计规范文档 design.md（2026-08-09）
- **Status:** complete
- 输出项目根目录 `design.md`：配色色值、字体字号/字重/行高、间距刻度、组件样式（按钮/卡片/导航/表格/筛选控件）、列表页五层布局（标题/筛选/按钮/表格/分页）、各组件体验流程与自适应按钮语义。
- 约定：后续新建页面/组件前先读 `design.md`，严格按规范设计。

### 接口管理按钮区优化：批量行按需出现（2026-08-09）
- **Status:** complete
- 用户反馈按钮多时按钮区不美观：批量操作行改为仅在选中数据时出现，未选中时只保留页面操作行（分组标签/导入/导出/新增接口）+ 分页右上。
- design.md 布局规范同步更新为“第一行页面操作+分页、第二行批量操作按需出现”。
- 验证：`npm run build` 通过。

### 详情弹窗内列表加分页（2026-08-09）
- **Status:** complete
- 应用管理详情“已订阅接口”、用户管理详情“所属应用/订阅记录”内部列表新增紧凑分页（每页 5 条，超出才显示），数据多时不再一列到底。
- 验证：`npm run build` 通过。

### 详情弹窗列表健壮化 + 筛选（2026-08-09）
- **Status:** complete
- 用户详情列表加载加 try/catch 与 id 字符串归一（避免类型不一致导致空数据）。
- 详情内列表按规范加筛选：应用管理“已订阅接口”、用户管理“所属应用/订阅记录”均加标题行+搜索框（客户端过滤），分页/空态按筛选结果计算（无数据/无匹配分别提示）。
- 验证：后端接口实测 admin 有 5 应用/12 订阅；`npm run build` 通过。

### 用户详情左右分栏 + 全站列表排序与过滤增强（2026-08-09）
- **Status:** complete
- 用户详情弹窗改为左右分栏（所属应用 | 订阅记录），高度大幅降低。
- 列表排序：应用（名称/状态/创建时间）、接口（名称/路径/状态）、用户（账号/角色/状态/创建时间）、订阅（状态/申请时间）、日志（接口/路径/应用/用户/IP，时间/状态码/耗时原有）、调用明细（成功率/平均耗时）、通知（时间）、限流（配置状态/启用/容量/补充速率）。
- 新增过滤：用户（角色/状态，后端 `/user/page` 加 role/status）、接口（请求方式，后端加 method）、日志（请求方式，后端加 method）、限流（配置状态，客户端）。
- 验证：后端过滤实测通过；`mvn package`、`npm run build` 通过；后端已重启。

### 枚举字段筛选改为表头列筛选（2026-08-09 梳理）
- **Status:** complete
- 用户要求“状态/角色等少数值字段只需列表头 filter”：移除筛选条中新增的角色/状态/请求方式/配置状态下拉，统一改为 `el-table-column` 的 `filters + filter-method` 表头筛选（漏斗+复选框）。
- 涉及：用户（角色/状态）、接口（方式/状态）、日志（方式）、应用（状态）、订阅（状态）、限流（配置状态/启用）、通知（状态）。
- 筛选条回归“搜索框 + 更多筛选（文本类条件）”的轻量形态；design.md 同步补充“枚举字段用表头列筛选”规则。
- 验证：`npm run build` 通过。

### 页面标题区紧凑化（2026-08-09）
- **Status:** complete
- 用户反馈标题区太大太空：全局标题 24px→20px，去掉 toolbar 底边框与 18px 内边距，标题与筛选区间距收紧到 12px（符合 design.md 规范并同步更新）。
- 涉及所有列表页（应用/接口/用户/订阅/日志/通知/限流/调用明细）。
- 验证：`npm run build` 通过。

### 列表链接颜色调整（2026-08-09）
- **Status:** complete
- 用户反馈表格超链接颜色太深：链接改为亮蓝 `#3b82f6`、字重 600→500，hover 变深 `#2563eb` 并加下划线；design.md 配色表补充链接色。
- 验证：`npm run build` 通过。

### 应用管理页体验样板（2026-08-09）
- **Status:** complete
- 以应用管理为样板验证改进方案：
  - 表格新增行内操作列（详情/重命名/删除图标按钮，@click.stop 不干扰行选中）。
  - 筛选同步 URL（keyword/status），刷新/返回保留筛选；进入页面自动回填。
  - 补 onActivated 刷新，避免 keep-alive 下数据陈旧。
- 待用户评估后决定是否推广到其他页面。
- 验证：`npm run build` 通过。

### 应用管理样板调整（2026-08-09）
- **Status:** complete
- 用户反馈操作列不佳：已移除行内操作列（含相关函数/图标），恢复原表格；保留筛选 URL 同步与 onActivated 刷新。

### 体验改进全站落地（除操作列，2026-08-09）
- **Status:** complete
- 详情抽屉：应用/用户详情改为右侧 el-drawer（640/880px），子列表左右分栏保留。
- 筛选 URL 同步：接口（keyword/status/tag）、用户（keyword）、订阅（keyword）、通知（filter）、调用明细（dimension/range）、限流（tab+关键字）全部同步 URL，进页回填、变更写地址栏。
- 数字列右对齐：调用明细（调用量/成功/失败/成功率/耗时）、日志（状态码/耗时）、限流（容量/补充速率）。
- 骨架屏：新增 `TableSkeleton.vue`，应用/用户/接口/日志/订阅主列表加载时显示骨架代替转圈。
- 全局搜索 + 快捷键：顶栏搜索打开全局搜索（接口/应用/用户，分组结果、回车跳转），`/` 打开、Esc 关闭。
- 深色模式保守补齐：暗色下表格头/边框/hover/current-row/batch-tip 适配，仅影响 `.dark`。
- 用户否决项：行内操作列、左侧蓝条不实施。
- 验证：`npm run build` 通过。

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
