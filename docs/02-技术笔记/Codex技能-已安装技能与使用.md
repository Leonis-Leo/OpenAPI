# Codex 技能：已安装技能与使用

> 归档日期：2026-08-02
> 适用范围：本机 Codex（桌面版）全局技能

## 已安装技能清单

| 技能 | 版本/来源 | 安装位置 | 状态 |
|------|-----------|----------|------|
| planning-with-files | v3.9.0（OthmanAdi/planning-with-files 插件市场） | `~/.codex/plugins` + 工作区 `.codex/` | 已启用 |
| code-review-and-quality | addyosmani/agent-skills | `C:\Users\fjp72\.codex\skills\code-review-and-quality` | 已安装 |
| frontend-ui-engineering | addyosmani/agent-skills | `C:\Users\fjp72\.codex\skills\frontend-ui-engineering` | 已安装 |

## 各技能怎么用

### 1. planning-with-files（持久化文件规划）

- **作用**：把任务计划、研究发现、进度写进 Markdown 文件，作为“磁盘上的工作记忆”，上下文清空/压缩后仍可恢复。
- **管理文件**（放在项目根目录，不在技能目录）：
  - `task_plan.md`：目标、当前阶段、阶段列表、决策、错误记录
  - `findings.md`：研究发现
  - `progress.md`：会话日志、测试结果
- **触发方式**：
  - 自动：3 步以上 / 5+ 次工具调用的复杂任务会自动启用
  - 手动：`用 planning-with-files 把这个任务拆成阶段，先建好 task_plan.md`
- **生命周期钩子**（Windows 已配好）：
  - SessionStart：注入上次计划上下文
  - PreToolUse：每次工具调用前重读 task_plan.md 前 30 行
  - PostToolUse：提醒更新 progress.md
  - Stop / PreCompact：完成度检查、压缩前同步
- **断线恢复**：新线程里说“用 planning-with-files 恢复上次的计划”，或手动跑
  `session-catchup.py`。
- **验证**：`sh .codex\skills\planning-with-files\scripts\plan-doctor.sh`
- **环境前提**：Python（已装 `E:\Python314`，`PYTHON_BIN` 已指向）、Git Bash、`hooks` 功能已启用。

### 2. code-review-and-quality（代码审查）

- **作用**：合并前多维度代码审查，覆盖五个轴：正确性、可读性、架构、安全性、性能。
- **触发方式**：`用 code-review-and-quality 审查这次改动`（自己写的、别人写的、AI 写的都适用）。
- **批准标准**：只要明显提升整体代码健康就通过，不追求“完美代码”。

### 3. frontend-ui-engineering（前端 UI 工程）

- **作用**：生产级、可访问、响应式的前端 UI：组件、布局、WCAG 无障碍、状态管理、交互细节。
- **触发方式**：`用 frontend-ui-engineering 实现这个页面/组件`。
- **目标**：输出像资深前端工程师写的，而不是“AI 生成感”界面。

## 生效方式与注意事项

- 新安装的技能/钩子需要**重启 Codex 应用 + 新开任务**才会加载。
- 本机没有系统级 Python 时已用 `E:\Python314` 兜底；删除前注意确认 `PYTHON_BIN` 指向。
- 一次性任务可临时禁用 planning hooks：`PLANNING_DISABLED=1`。

## 相关笔记

- [[Codex技能-OpenAPI工程使用的技能]]
