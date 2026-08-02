# BCrypt 密码加盐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将用户密码哈希从无盐 SHA-256 升级为 BCrypt（加盐），存量账号登录时自动惰性迁移，且不改变任何对外接口契约。

**Architecture:** 在 `openapi-backend` 引入 `spring-security-crypto`，由 `PasswordUtils` 统一提供 `encode / matches / isLegacySha256 / sha256`；`UserServiceImpl` 的所有密码写入点改用 BCrypt，登录先按哈希格式分流校验，旧 SHA-256 校验通过且账号启用后静默重写为 BCrypt。演示数据、CI、文档同步更新，并新增单元测试守护安全逻辑。

**Tech Stack:** Java 17、Spring Boot 3.3.5、spring-security-crypto（BCryptPasswordEncoder，cost 10）、JUnit 5（spring-boot-starter-test）、MyBatis-Plus、GitHub Actions。

## Global Constraints

- BCrypt cost = 10（`BCryptPasswordEncoder` 默认值），哈希存于现有 `user_password VARCHAR(128)` 列，无需改表。
- 对外接口、参数、响应结构不变；登录失败文案一律为"账号或密码错误"。
- 密码强度校验保持不变（仅非空）。
- 演示账号 admin 的明文密码保持为 `admin`。
- 旧格式判定标准：64 位小写十六进制字符串。
- 不引入 Spring Security 全量框架，只添加 `spring-security-crypto`。
- 所有密码写入/校验必须经由 `PasswordUtils`，禁止在 Service 中直接调用编码器。
- 直接在当前分支 `feature_dev` 上实施，无需新建工作树。

---

### Task 1: spring-security-crypto 依赖 + PasswordUtils BCrypt 改造（TDD）

**Files:**
- Modify: `openapi-backend/pom.xml`（dependencies 段，`spring-boot-starter-amqp` 之后）
- Modify: `openapi-backend/src/main/java/com/openapi/backend/common/PasswordUtils.java`
- Create: `openapi-backend/src/test/java/com/openapi/backend/common/PasswordUtilsTest.java`

**Interfaces:**
- Consumes: 无（本任务无前置任务）
- Produces（后续任务依赖的精确签名）:
  - `PasswordUtils.encode(String raw): String` — 返回 BCrypt 哈希
  - `PasswordUtils.matches(String raw, String encoded): boolean` — BCrypt 比对，非 BCrypt 格式返回 false
  - `PasswordUtils.isLegacySha256(String encoded): boolean` — 64 位小写十六进制判定
  - `PasswordUtils.sha256(String raw): String` — 保留，仅用于旧哈希校验

- [ ] **Step 1: 在 pom.xml 添加依赖**

在 `openapi-backend/pom.xml` 的 `<dependencies>` 中、`spring-boot-starter-amqp` 依赖之后加入（版本由父 POM 托管，不写版本号）：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

- [ ] **Step 2: 写失败测试**

创建 `openapi-backend/src/test/java/com/openapi/backend/common/PasswordUtilsTest.java`：

```java
package com.openapi.backend.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {

    private static final String DEMO_ADMIN_HASH =
            "$2b$10$4SPZdBW1mq9oyas7qVL62ukMG9JB8tJ6fBlwdn6h11UfI0mgfG6SK";

    @Test
    void encode_producesBcryptHashWithRandomSalt() {
        String hash1 = PasswordUtils.encode("admin");
        String hash2 = PasswordUtils.encode("admin");
        assertTrue(hash1.startsWith("$2"), "哈希应以 $2 开头");
        assertEquals(60, hash1.length(), "BCrypt cost 10 哈希应为 60 字符");
        assertNotEquals(hash1, hash2, "相同密码两次编码盐应不同");
    }

    @Test
    void matches_roundTrip() {
        String hash = PasswordUtils.encode("123456");
        assertTrue(PasswordUtils.matches("123456", hash));
        assertFalse(PasswordUtils.matches("wrong", hash));
    }

    @Test
    void matches_verifiesInitSqlDemoHash() {
        assertTrue(PasswordUtils.matches("admin", DEMO_ADMIN_HASH));
    }

    @Test
    void matches_returnsFalseForNonBcryptFormat() {
        assertFalse(PasswordUtils.matches("admin", "not-a-bcrypt-hash"));
        assertFalse(PasswordUtils.matches("admin", null));
    }

    @Test
    void isLegacySha256_detectsOldFormat() {
        String legacy = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";
        assertTrue(PasswordUtils.isLegacySha256(legacy));
        assertFalse(PasswordUtils.isLegacySha256("$2b$10$4SPZdBW1mq9oyas7qVL62ukMG9JB8tJ6fBlwdn6h11UfI0mgfG6SK"));
        assertFalse(PasswordUtils.isLegacySha256(""));
        assertFalse(PasswordUtils.isLegacySha256(null));
    }

    @Test
    void sha256_stillVerifiesLegacyHash() {
        assertEquals("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918",
                PasswordUtils.sha256("admin"));
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `mvn -B -ntp -pl openapi-backend -am test -Dtest=PasswordUtilsTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL（`encode`/`matches`/`isLegacySha256` 不存在，编译报错）。

- [ ] **Step 4: 实现 PasswordUtils**

用以下内容整体替换 `openapi-backend/src/main/java/com/openapi/backend/common/PasswordUtils.java`：

```java
package com.openapi.backend.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 密码工具：BCrypt 加盐哈希（生产标准，cost 10）。
 * SHA-256 仅保留用于存量哈希的迁移期校验，新数据一律走 BCrypt。
 */
public final class PasswordUtils {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final Pattern LEGACY_SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    private PasswordUtils() {
    }

    /** 生成 BCrypt 哈希（cost 10，自动加盐）。 */
    public static String encode(String raw) {
        return ENCODER.encode(raw);
    }

    /** BCrypt 比对；非 BCrypt 格式（含 null）一律返回 false。 */
    public static boolean matches(String raw, String encoded) {
        return ENCODER.matches(raw, encoded);
    }

    /** 是否为旧版 64 位小写 SHA-256 哈希。 */
    public static boolean isLegacySha256(String encoded) {
        return encoded != null && LEGACY_SHA256_PATTERN.matcher(encoded).matches();
    }

    /** 旧版 SHA-256 哈希（仅用于存量数据校验）。 */
    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("不支持的哈希算法", e);
        }
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -B -ntp -pl openapi-backend -am test -Dtest=PasswordUtilsTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS（6 个用例全绿）。

- [ ] **Step 6: 提交**

```bash
git add openapi-backend/pom.xml openapi-backend/src/main/java/com/openapi/backend/common/PasswordUtils.java openapi-backend/src/test/java/com/openapi/backend/common/PasswordUtilsTest.java
git commit -m "feat: 密码哈希升级为 BCrypt 并补充单元测试"
```

---

### Task 2: UserServiceImpl 接入 BCrypt + 登录惰性迁移

**Files:**
- Modify: `openapi-backend/src/main/java/com/openapi/backend/service/impl/UserServiceImpl.java`

**Interfaces:**
- Consumes: `PasswordUtils.encode` / `PasswordUtils.matches` / `PasswordUtils.isLegacySha256` / `PasswordUtils.sha256`（Task 1 产出）
- Produces: 无新对外接口；行为约定——`login` 对旧 SHA-256 账号校验通过且启用时自动迁移为 BCrypt；未识别格式视为密码错误

- [ ] **Step 1: 类注解加 @Slf4j 并补充 import**

在类注解处加 `@Slf4j`（Lombok 已依赖），并补充 `import lombok.extern.slf4j.Slf4j;`。

- [ ] **Step 2: 替换注册/新增/改密的哈希调用**

将 `UserServiceImpl.java` 中三处 `PasswordUtils.sha256(userPassword)` 全部替换为 `PasswordUtils.encode(userPassword)`，分别位于：
- `register` 方法（约第 26 行）
- `createUser` 方法（约第 88 行）
- `updateUser` 方法（约第 108 行）

- [ ] **Step 3: 重构 login 为格式分流 + 惰性迁移**

将 `login` 方法整体替换为：

```java
@Override
public User login(String userAccount, String userPassword) {
    User user = lambdaQuery().eq(User::getUserAccount, userAccount).one();
    if (user == null || !passwordMatches(user, userPassword)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
    }
    if (Integer.valueOf(0).equals(user.getStatus())) {
        throw new BusinessException(ErrorCode.NO_AUTH, "账号已被禁用");
    }
    migrateLegacyPassword(user, userPassword);
    user.setUserPassword(null);
    return user;
}

private boolean passwordMatches(User user, String rawPassword) {
    String stored = user.getUserPassword();
    if (PasswordUtils.isLegacySha256(stored)) {
        return PasswordUtils.sha256(rawPassword).equalsIgnoreCase(stored);
    }
    return PasswordUtils.matches(rawPassword, stored);
}

private void migrateLegacyPassword(User user, String rawPassword) {
    if (PasswordUtils.isLegacySha256(user.getUserPassword())) {
        try {
            user.setUserPassword(PasswordUtils.encode(rawPassword));
            updateById(user);
        } catch (Exception e) {
            log.warn("存量密码迁移 BCrypt 失败，账号：{}", user.getUserAccount(), e);
        }
    }
}
```

- [ ] **Step 4: 编译 + 全量测试验证**

Run: `mvn -B -ntp -pl openapi-backend -am test -Dsurefire.failIfNoSpecifiedTests=false`

Expected: BUILD SUCCESS，PasswordUtilsTest 6 个用例全绿。

- [ ] **Step 5: 手工冒烟验证（可选，本机服务可用时执行）**

1. 若 8101 端口已有旧后端进程，先停止它（`Get-Process java | Stop-Process` 需确认只杀本项目的进程），然后 `mvn -B -ntp package -DskipTests` 重新打 jar 并 `java -jar openapi-backend/target/openapi-backend-0.0.1-SNAPSHOT.jar` 启动。
2. 用旧 SHA-256 的 admin 账号登录（本地库中若还是旧哈希，本次登录应成功且触发迁移）：

```powershell
curl.exe -s -X POST "http://localhost:8101/v1/user/login?userAccount=admin&userPassword=admin"
```

Expected: 响应 `"code":0`。

3. 查库确认哈希已迁移：

```powershell
mysql -h127.0.0.1 -uroot -p123456 openapi -e "SELECT user_password FROM user WHERE user_account='admin'"
```

Expected: 输出以 `$2` 开头（非 64 位十六进制）。

4. 错误密码仍被拒绝：

```powershell
curl.exe -s -X POST "http://localhost:8101/v1/user/login?userAccount=admin&userPassword=wrong"
```

Expected: 响应非 `"code":0`，文案为"账号或密码错误"。

> 本机无 mysql 客户端时跳过第 3 步；端到端行为由 CI 的 Apifox 用例兜底。

- [ ] **Step 6: 提交**

```bash
git add openapi-backend/src/main/java/com/openapi/backend/service/impl/UserServiceImpl.java
git commit -m "feat: 登录支持存量 SHA-256 密码惰性迁移为 BCrypt"
```

---

### Task 3: init.sql 演示数据更新

**Files:**
- Modify: `db/init.sql`（约第 130 行，admin 演示账号 INSERT）

**Interfaces:**
- Consumes: Task 1 的 `PasswordUtils.matches`（用于验证哈希正确性，Task 1 的测试已含固定哈希断言）
- Produces: 新装环境 admin 账号（明文密码 `admin`）的哈希为 BCrypt，CI/本地初始化均可直接登录

- [ ] **Step 1: 替换 admin 哈希**

将 `db/init.sql` 第 130 行：

```sql
VALUES (1, 'admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', '管理员', 'admin');
```

替换为：

```sql
-- admin 的 BCrypt 哈希（cost 10），明文密码仍为 admin
VALUES (1, 'admin', '$2b$10$4SPZdBW1mq9oyas7qVL62ukMG9JB8tJ6fBlwdn6h11UfI0mgfG6SK', '管理员', 'admin');
```

- [ ] **Step 2: 验证哈希与 Task 1 测试一致**

确认 `db/init.sql` 中写入的哈希字符串与 `PasswordUtilsTest.DEMO_ADMIN_HASH` 完全一致（`matches_verifiesInitSqlDemoHash` 用例守护其正确性）。

Run: `mvn -B -ntp -pl openapi-backend -am test -Dtest=PasswordUtilsTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS（证明该哈希匹配明文 `admin`）。

- [ ] **Step 3: 提交**

```bash
git add db/init.sql
git commit -m "chore: init.sql 演示账号改用 BCrypt 哈希"
```

---

### Task 4: CI 执行测试 + 文档更新

**Files:**
- Modify: `.github/workflows/ci.yml`（约第 64 行）
- Modify: `docs/01-项目文档/项目规划.md`（约第 45 行）
- Modify: `docs/05-接口文档/接口文档.md`（第 9-10 行）

**Interfaces:**
- Consumes: 无代码依赖
- Produces: CI 真正执行单测；文档反映 BCrypt 已完成与演示密码事实

- [ ] **Step 1: CI 去掉 -DskipTests**

`.github/workflows/ci.yml` 第 64 行：

```yaml
run: mvn -B -ntp package -DskipTests
```

改为：

```yaml
run: mvn -B -ntp package
```

- [ ] **Step 2: 勾选项目规划待办**

`docs/01-项目文档/项目规划.md` 第 45 行：

```markdown
- [ ] 密码 BCrypt 加盐
```

改为：

```markdown
- [x] 密码 BCrypt 加盐（存量账号登录时惰性迁移）
```

- [ ] **Step 3: 更正接口文档演示密码**

`docs/05-接口文档/接口文档.md` 第 9 行与第 10 行中的 `userPassword = 123456` 均改为 `userPassword = admin`（与 init.sql 演示账号一致）。

- [ ] **Step 4: 本地全量验证**

Run: `mvn -B -ntp package`

Expected: BUILD SUCCESS（含 PasswordUtilsTest 6 个用例）。

再执行 `git diff` 复核：只应有 ci.yml、两个 docs 文件的预期改动。

- [ ] **Step 5: 提交**

```bash
git add .github/workflows/ci.yml docs/01-项目文档/项目规划.md docs/05-接口文档/接口文档.md
git commit -m "chore: CI 执行单测，文档更新 BCrypt 完成状态"
```
