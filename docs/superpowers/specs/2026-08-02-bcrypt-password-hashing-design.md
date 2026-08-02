# BCrypt 密码加盐设计文档

> 日期：2026-08-02
> 状态：已评审通过

## 背景与目标

当前系统所有用户密码均使用**无盐 SHA-256** 存储（`PasswordUtils.sha256`），属于已知安全隐患：相同密码产生相同哈希，且可被彩虹表/暴力破解。

本次目标：将密码哈希升级为 **BCrypt（加盐）**，同时保证存量用户无感知迁移，不改变任何对外接口契约。

## 现状

- 密码哈希仅在 `openapi-backend` 使用，共 4 处调用点（均在 `UserServiceImpl`）：
  - `register`：注册时哈希存储
  - `login`：登录时比对
  - `createUser`：管理员新增用户
  - `updateUser`：管理员/自助修改密码
- `db/init.sql` 中演示账号 admin 的哈希为 SHA-256(`admin`)，即明文密码为 `admin`（文档中 123456 为旧说法）。
- 无任何 JUnit 测试；CI（`.github/workflows/ci.yml`）使用 `mvn package -DskipTests`，Apifox 用例基于明文密码登录，不依赖哈希格式。

## 决策记录

| 决策点 | 选择 | 理由 |
| --- | --- | --- |
| 存量密码迁移 | 登录时惰性迁移 | 用户无感知，一个迭代周期内自然完成升级 |
| BCrypt 实现 | `spring-security-crypto` 的 `BCryptPasswordEncoder` | 生态标准、由 Spring 团队维护、版本由 Boot 3.3.5 父 POM 托管 |
| 密码强度规则 | 本次不做 | 锁定范围为哈希升级，避免牵连前端表单与测试用例 |
| 哈希强度 | cost 10（Spring 默认值） | 与 Spring 默认一致，安全与性能均衡 |
| 演示数据 | init.sql 固定写入 `admin` 的 BCrypt 哈希 | 保证 CI 可复现；明文密码保持不变 |
| 单元测试 | 新增 PasswordUtilsTest，并让 CI 真正执行测试 | 安全逻辑必须有回归保障，CI 已具备测试依赖 |

## 方案设计

### 1. 依赖

`openapi-backend/pom.xml` 增加：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

版本由 `spring-boot-starter-parent` 3.3.5 托管，无需手写版本号。该依赖只引入加密模块，不引入整套 Spring Security。

### 2. PasswordUtils 改造

保留静态工具类风格，新增/调整方法：

- `encode(String raw)` → 返回 BCrypt 哈希（cost 10）
- `matches(String raw, String encoded)` → BCrypt 比对（兼容 `$2a$`/`$2b$`/`$2y$` 前缀）
- `isLegacySha256(String encoded)` → 是否为 64 位小写十六进制（旧格式识别）
- 保留 `sha256(String raw)`，仅用于旧哈希比对

类内持有静态 `BCryptPasswordEncoder` 实例。

### 3. 登录惰性迁移

`UserServiceImpl.login` 校验逻辑：

1. 按哈希格式分流：
   - `$2a$`/`$2b$`/`$2y$` 开头 → `matches` 比对
   - 64 位十六进制 → `sha256` 比对
   - 其他格式 → 判定密码错误
2. 失败统一返回"账号或密码错误"（沿用现状文案，不做用户枚举区分）。
3. **旧 SHA-256 校验通过且账号启用时**，将 `userPassword` 重写为 `encode(raw)` 并落库（禁用账号不落库）。
4. 新注册、管理员新增用户、修改密码三处改为 `encode`。

### 4. 数据与文档

- `db/init.sql`：admin 演示账号哈希替换为 `admin` 的固定 BCrypt 哈希（cost 10）。
- `docs/01-项目文档/项目规划.md`：勾掉"密码 BCrypt 加盐"待办并补充迁移策略说明。
- `docs/05-接口文档/接口文档.md`：admin 密码 123456 的旧说法更正为与 init.sql 一致（顺带修正）。

### 5. 测试与 CI

- 新增 `openapi-backend/src/test/java/com/openapi/backend/common/PasswordUtilsTest.java`，覆盖：
  - `encode` 输出以 `$2a$` 开头，且两次编码盐不同
  - `matches` 往返成功、错误密码失败
  - `isLegacySha256` 正确识别旧格式
  - 旧 `sha256` 仍可校验
- CI：`mvn package -DskipTests` 改为执行测试（去除 `-DskipTests`），使新单测真正生效。

## 错误处理

- 未识别哈希格式：视为密码错误，不抛内部异常、不泄露哈希格式细节。
- 迁移写库失败：校验已通过，登录照常成功，下次登录重试迁移（`updateById` 异常不影响登录主流程的已有行为）。

## 非目标

- 不改动密码强度策略（长度/复杂度校验）。
- 不引入 Spring Security 全量框架。
- 不做批量数据迁移脚本（依赖登录惰性迁移）。

## 验证方式

1. `mvn test`：新增单测全绿。
2. 本地启动后端：旧 SHA-256 账号登录成功且库中哈希自动变为 BCrypt；新注册账号直接为 BCrypt；错误密码仍提示"账号或密码错误"。
3. CI：Apifox 用例继续全绿（登录明文密码不变）。
