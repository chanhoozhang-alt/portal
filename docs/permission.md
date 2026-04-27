# 权限系统设计文档

## 一、整体架构

```
客户端
  ↓ 携带 JWT Token
网关（portal-gateway）
  ↓ JWT 验证 + 透传工号
认证服务（portal-auth）
  ↓ Sa-Token 建立会话 + 权限缓存（Redis）
系统服务（portal-system）
  ↓ 用户/角色/菜单/部门 CRUD + 操作日志
数据库（MySQL）
```

权限模型采用 **RBAC（基于角色的访问控制）**，支持菜单权限和数据权限两种粒度。

---

## 二、数据模型

### 2.1 核心表

| 表名 | 说明 | 数据来源 |
|------|------|----------|
| sys_user | 用户表 | 从上游系统同步 |
| sys_role | 角色表 | 后台维护 |
| sys_menu | 菜单表 | 后台维护 |
| sys_dept | 部门表 | 从上游系统同步 |

### 2.2 关联表

| 表名 | 说明 | 用途 |
|------|------|------|
| sys_user_role | 用户-角色 | 一个用户可分配多个角色 |
| sys_role_menu | 角色-菜单 | 一个角色可关联多个菜单/按钮权限 |
| sys_role_dept | 角色-部门 | 数据权限，角色可访问哪些部门的数据 |

### 2.3 辅助表

| 表名 | 说明 |
|------|------|
| sys_operation_log | 操作日志 |

### 2.4 表关系

```
sys_user ←——多对多——→ sys_role ←——多对多——→ sys_menu
                          |
                       多对多
                          |
                       sys_dept
```

---

## 三、菜单权限设计

### 3.1 菜单类型

sys_menu 表的 `menu_type` 字段分三种：

| 类型 | 标识 | 说明 | 示例 |
|------|------|------|------|
| 目录 | D | 前端路由分组，不涉及权限 | "系统管理" |
| 菜单 | M | 页面级权限 | "用户管理" |
| 按钮 | B | 操作级权限 | "新增用户"、"删除用户" |

### 3.2 权限标识

sys_menu 表的 `permission` 字段存储权限标识，格式为 `模块:资源:操作`：

```
system:user:list      查看用户列表
system:user:add       新增用户
system:user:edit      编辑用户
system:user:delete    删除用户
system:role:list      查看角色列表
app:list              查看应用列表
app:add               新增应用
```

按钮类型（B）的菜单必须设置 `permission`，用于后端接口级别的权限校验。

### 3.3 菜单树结构

通过 `parent_id` 构建树形结构，典型结构：

```
系统管理（D）
  ├── 用户管理（M）permission=null，页面级可见性
  │     ├── 新增用户（B）permission=system:user:add
  │     ├── 编辑用户（B）permission=system:user:edit
  │     └── 删除用户（B）permission=system:user:delete
  ├── 角色管理（M）
  │     ├── 新增角色（B）permission=system:role:add
  │     ├── 编辑角色（B）permission=system:role:edit
  │     └── 删除角色（B）permission=system:role:delete
  └── 部门管理（M）
```

---

## 四、认证流程

### 4.1 登录流程

```
用户访问上游系统
  ↓
上游系统签发 JWT Token
  ↓
前端携带 JWT Token 调用 /api/auth/verify
  ↓
① 网关 AuthGlobalFilter 校验 JWT Token
  ↓ 通过后透传工号（X-User-EmpNo Header）
② AuthController.verify() 接收请求
  ↓
③ AuthService.verifyAndLogin() 处理：
  - 解析 JWT Token 获取工号
  - Feign 调用 system-svc 查询用户信息
  - 校验用户状态（是否禁用）
  - Sa-Token 建立本地会话（StpUtil.login(userId)）
  - 查询用户权限和菜单信息
  ↓
④ 返回 UserInfoVO（包含 Sa-Token、菜单树、权限列表）
  ↓
前端存储 Sa-Token，后续请求通过 Header 传递
```

### 4.2 网关认证

**实现类**：`portal-gateway` 中的 `AuthGlobalFilter`

- 从请求 Header 获取 `Authorization` 字段中的 JWT Token
- 调用 `JwtUtils.verify()` 验证 Token 有效性
- 解析工号，添加到下游请求 Header `X-User-EmpNo`
- 白名单路径跳过认证：
  - `/api/auth/verify` — 登录验证
  - `/api/auth/test-login` — 测试登录
  - `/api/auth/logout` — 登出

### 4.3 双重 Token 机制

| Token | 生成方 | 用途 | 存储 |
|-------|--------|------|------|
| JWT Token | 上游系统 | 网关层验证身份 | 前端传递 |
| Sa-Token | 本系统 | 服务间会话管理 | Redis |

---

## 五、权限缓存机制

### 5.1 缓存策略

**实现类**：`portal-auth` 中的 `PermissionService`

| 缓存键 | 值类型 | TTL | 说明 |
|--------|--------|-----|------|
| `perm:{userId}` | List\<String\> | 2小时 | 用户权限标识列表 |
| `menu:{userId}` | List\<MenuVO\> | 2小时 | 用户菜单树 |

### 5.2 读写流程

```
获取权限列表：
  ① 查 Redis 缓存（perm:userId）
  ② 命中 → 直接返回
  ③ 未命中 → Feign 调用 system-svc 查数据库 → 写入 Redis → 返回

获取菜单树：
  ① 查 Redis 缓存（menu:userId）
  ② 命中 → 直接返回
  ③ 未命中 → Feign 调用 system-svc 查数据库 → 写入 Redis → 返回
```

### 5.3 缓存刷新

- **被动过期**：Redis TTL 2小时到期后自动失效，下次请求重新加载
- **主动刷新**：管理员修改权限后调用 `/api/auth/refreshCache`，清除所有用户缓存

### 5.4 序列化方式

RedisTemplate 使用 fastjson2 序列化，配置类为 `RedisConfig`：
- 序列化时写入 `@type` 类型信息（`WriteClassName`）
- 反序列化时根据 `@type` 自动还原原始类型（`SupportAutoType`）

---

## 六、核心接口

### 6.1 认证接口（AuthController）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/verify | JWT Token 验证并建立 Sa-Token 会话 |
| POST | /api/auth/test-login | 测试登录（开发环境） |
| GET | /api/auth/permissions | 获取当前用户权限信息（菜单+权限） |
| GET | /api/auth/menus | 获取当前用户菜单树 |
| POST | /api/auth/logout | 登出 |
| POST | /api/auth/refreshCache | 刷新权限缓存 |

### 6.2 用户管理接口（UserController）

| 方法 | 路径 | 说明 | 日志 |
|------|------|------|------|
| GET | /api/system/users/{empNo} | 根据工号查询用户 | - |
| GET | /api/system/users/page | 分页查询用户 | - |
| PUT | /api/system/users/{id}/status | 更新用户状态 | @OperLog("更新用户状态") |
| POST | /api/system/users/{id}/roles | 分配用户角色 | @OperLog("分配用户角色") |
| GET | /api/system/users/{id}/roles | 获取用户角色列表 | - |

### 6.3 角色管理接口（RoleController）

| 方法 | 路径 | 说明 | 日志 |
|------|------|------|------|
| GET | /api/system/roles/page | 分页查询角色 | - |
| GET | /api/system/roles/list | 查询所有启用角色 | - |
| POST | /api/system/roles | 新增角色 | @OperLog("新增角色") |
| PUT | /api/system/roles | 修改角色 | @OperLog("修改角色") |
| DELETE | /api/system/roles/{id} | 删除角色 | @OperLog("删除角色") |
| POST | /api/system/roles/{id}/menus | 分配角色菜单 | @OperLog("分配角色菜单") |
| GET | /api/system/roles/{id}/menus | 获取角色菜单 | - |
| POST | /api/system/roles/{id}/depts | 分配数据权限 | @OperLog("分配角色数据权限") |
| GET | /api/system/roles/{id}/depts | 获取角色部门 | - |

### 6.4 菜单管理接口（MenuController）

| 方法 | 路径 | 说明 | 日志 |
|------|------|------|------|
| GET | /api/system/menus/tree | 菜单树（管理用，全部菜单） | - |
| GET | /api/system/menus/tree/{userId} | 菜单树（按用户权限过滤） | - |
| POST | /api/system/menus | 新增菜单 | @OperLog("新增菜单") |
| PUT | /api/system/menus | 修改菜单 | @OperLog("修改菜单") |
| DELETE | /api/system/menus/{id} | 删除菜单 | @OperLog("删除菜单") |

### 6.5 部门管理接口（DeptController）

| 方法 | 路径 | 说明 | 日志 |
|------|------|------|------|
| GET | /api/system/depts/tree | 部门树 | - |
| POST | /api/system/depts | 新增部门 | @OperLog("新增部门") |
| PUT | /api/system/depts | 修改部门 | @OperLog("修改部门") |
| DELETE | /api/system/depts/{id} | 删除部门 | @OperLog("删除部门") |

---

## 七、服务间调用

### 7.1 Feign 接口

**SystemFeignClient**（portal-common）：auth-svc 调用 system-svc

| 方法 | 说明 |
|------|------|
| getByEmpNo(empNo) | 根据工号查询用户 |
| getMenuTree(userId) | 获取用户菜单树（权限过滤后） |
| getPermissions(userId) | 获取用户权限列表 |

**AuthFeignClient**（portal-common）：system-svc 调用 auth-svc

| 方法 | 说明 |
|------|------|
| refreshCache() | 刷新权限缓存 |

### 7.2 调用场景

```
auth-svc 登录时：
  AuthService → SystemFeignClient.getByEmpNo() → 查询用户信息
  PermissionService → SystemFeignClient.getMenuTree() → 查询菜单树
  PermissionService → SystemFeignClient.getPermissions() → 查询权限列表

system-svc 修改权限后：
  RoleService → AuthFeignClient.refreshCache() → 清除权限缓存
```

---

## 八、操作日志

### 8.1 实现方式

- **注解**：`@OperLog`，标注在 Controller 方法上
- **切面**：`OperLogAspect`，AOP 环绕通知拦截

### 8.2 记录内容

| 字段 | 来源 |
|------|------|
| userId / username | Sa-Token 当前登录用户 |
| operation | 注解 value 值 |
| requestUrl | HttpServletRequest |
| method | HTTP 方法（GET/POST/PUT/DELETE） |
| requestParams | 方法参数 JSON 序列化 |
| responseCode | 返回结果状态码 |
| ip | 请求来源 IP（支持代理） |
| costTime | 方法执行耗时（毫秒） |

### 8.3 使用方式

在需要记录日志的 Controller 方法上加注解：

```java
@OperLog("新增角色")
@PostMapping
public R<Void> add(@RequestBody RoleVO roleVO) {
    roleService.addRole(roleVO);
    return R.ok();
}
```

仅在增删改等写操作上使用，查询接口不需要加。日志通过 `LogService.saveAsync()` 异步保存，不影响接口性能。

---

## 九、数据权限

### 9.1 设计思路

通过 `sys_role_dept` 关联表实现数据权限：

- 为角色分配可访问的部门范围
- 用户通过角色继承部门权限
- 查询业务数据时，根据用户角色的部门范围过滤数据

### 9.2 分配方式

调用 `POST /api/system/roles/{id}/depts` 接口，为角色分配部门列表。

### 9.3 应用场景

例如：用户 A 拥有"华南区管理员"角色，该角色关联了"深圳分公司"、"广州分公司"两个部门，则用户 A 只能查看这两个部门下的数据。

---

## 十、异常处理

### 10.1 业务异常

**BusinessException**：业务逻辑校验不通过时主动抛出。

```java
throw new BusinessException("用户不存在");
throw new BusinessException(403, "该角色下存在用户，无法删除");
```

### 10.2 全局异常处理

**GlobalExceptionHandler**：拦截所有 Controller 层异常，统一转为 `R` 响应格式。

| 异常类型 | 处理方式 | 日志级别 |
|----------|----------|----------|
| BusinessException | 返回业务错误码和消息 | warn |
| 其他 Exception | 兜底返回"系统内部错误" | error |

---

## 十一、技术栈

| 组件 | 选型 | 版本 |
|------|------|------|
| 安全框架 | Sa-Token | 1.38.0 |
| Redis 客户端 | Lettuce | Spring Boot 默认 |
| 序列化 | Fastjson2 | 2.0.47 |
| 服务间调用 | OpenFeign | - |
| 配置中心/注册中心 | Nacos | - |
| ORM | MyBatis-Plus | 3.5.5 |
| ID 生成 | 雪花算法 | MyBatis-Plus 内置 |

---

## 十二、菜单树构建逻辑

`MenuService.treeByUserId()` 方法实现：

1. 查询所有启用菜单
2. 查询用户角色关联的菜单 ID 集合
3. 过滤出用户有权限的菜单
4. 补充父级菜单（确保树结构完整，即使父级菜单未直接授权）
5. 递归构建树形结构
6. 按排序字段排序
