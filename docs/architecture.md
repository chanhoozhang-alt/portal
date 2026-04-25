# 技术架构文档

## 一、整体架构

```
                     ┌───────────────┐
  浏览器 ──────────→│   Nginx       │  统一入口（开发环境 Docker:80）
  (localhost)       │   (:80)       │
                     └───────┬───────┘
                             │
                    ┌────────┴────────┐
                    │                 │
              页面请求(/)        API请求(/api/**)
                    │                 │
                    ▼                 ▼
             ┌──────────┐   ┌───────────────┐
             │  Vite    │   │   Gateway     │  Spring Cloud Gateway
             │  (:3000) │   │   (8080)      │  JWT 校验 + 路由转发
             │  页面服务  │   └───────┬───────┘
             └──────────┘           │
                            ┌───────┼───────┐
                            │       │       │
                      ┌─────▼──┐ ┌─▼──────┐ ┌▼──────────┐
                      │  auth  │ │ system │ │ business  │
                      │ (9001) │ │ (9002) │ │  (9003)   │
                      └────────┘ └────────┘ └───────────┘
                            │       │       │
                            └───────┼───────┘
                                    │
                    ┌───────────────┼──────────────┐
                    │               │              │
              ┌─────▼────┐   ┌─────▼────┐   ┌─────▼────┐
              │  Nacos   │   │  MySQL   │   │  Redis   │
              │  2.2.3   │   │  8.0     │   │          │
              └──────────┘   └──────────┘   └──────────┘
```

## 二、模块职责

### portal-common（公共模块）

被所有微服务依赖，包含：

- **model/** — 10 个实体类（SysUser、SysRole、SysMenu 等）
- **vo/** — 视图对象（LoginVO、MenuVO、SysUserVO、UserInfoVO、DeptVO）
- **result/** — 统一响应体 `R<T>`
- **exception/** — BusinessException + 全局异常处理器
- **feign/** — 服务间调用的 Feign 接口定义
- **utils/** — JWT 工具类
- **annotation/** — @OperLog 操作日志注解
- **config/** — MyBatis-Plus 自动填充配置

### portal-gateway（网关服务）

- **路由转发** — 按 URL 前缀将请求分发到对应微服务
- **JWT 校验** — AuthGlobalFilter 对非白名单请求校验 Token
- **用户信息透传** — 校验通过后将工号写入 `X-User-EmpNo` Header 传递给下游

路由规则：

| 路径 | 目标服务 |
|------|----------|
| /api/auth/** | portal-auth |
| /api/system/** | portal-system |
| /api/portal/**、/api/app/**、/api/category/** | portal-business |

### portal-auth（认证服务）

- **SSO 登录** — 验证上游传入的 JWT Token，通过 Sa-Token 建立本地会话
- **权限查询** — 获取用户的菜单树和权限列表
- **缓存管理** — Redis 缓存权限数据，支持缓存刷新

### portal-system（系统管理服务）

- **用户管理** — 分页查询、状态启禁、角色分配
- **角色管理** — CRUD、菜单权限分配、数据权限（部门）分配
- **菜单管理** — 树形 CRUD、按用户权限过滤菜单树
- **部门管理** — 树形 CRUD、树结构查询
- **操作日志** — AOP 自动记录、分页查询
- **数据同步** — 定时从上游同步人员/部门数据（接口已预留）

### portal-business（门户业务服务）

- **应用管理** — 应用 CRUD、分类管理
- **门户首页** — 返回分类及应用的展示数据

## 三、认证流程

```
用户从上游系统点击门户链接（URL 带 ?token=xxx）
       │
       ▼
 前端提取 token → 存入 localStorage
       │
       ▼
 调用 POST /api/auth/verify
       │
       ▼
 Gateway AuthGlobalFilter 放行（白名单）
       │
       ▼
 auth-svc 验证 token → 解析工号
       │
       ▼
 Feign 调用 system-svc 查询用户信息
       │
       ▼
 Sa-Token 建立会话 → 返回用户信息 + 菜单树 + 权限列表
       │
       ▼
 前端根据菜单树动态生成路由，根据 permissions 控制按钮
```

### Sa-Token 跨服务会话共享

所有微服务共用同一个 Redis 作为 Sa-Token 存储，确保任意服务都能读取会话状态。

## 四、权限模型（RBAC）

```
用户(sys_user) ──N:N── 用户角色(sys_user_role) ──N:N── 角色(sys_role)
                                                      │
                                              ┌───────┼───────┐
                                              │               │
                                    角色菜单(sys_role_menu)  角色部门(sys_role_dept)
                                              │               │
                                              ▼               ▼
                                        菜单(sys_menu)    部门(sys_dept)
```

### 权限层级

- **目录（D）** — 一级导航分组
- **菜单（M）** — 可访问的页面，对应前端路由
- **按钮（B）** — 页面内的操作权限，如 `system:user:add`

### 权限控制方式

| 层级 | 方式 | 说明 |
|------|------|------|
| 菜单 | 动态路由 | 后端返回用户有权限的菜单树，前端据此生成路由 |
| 按钮 | v-permission 指令 | `v-permission="'system:user:add'"` 控制按钮显示 |
| 数据 | 角色-部门关联 | 不同角色查看不同部门的数据（预留） |

## 五、服务间调用

```
auth-svc ──Feign──→ system-svc    查询用户信息、菜单、权限
system-svc ──Feign──→ auth-svc    数据同步后通知刷新缓存
```

Feign 接口定义在 portal-common 中：

- **SystemFeignClient** — getUserByEmpNo、getMenuTree、getPermissions
- **AuthFeignClient** — refreshPermissionCache

## 六、数据库设计

共 10 张表，位于同一个 MySQL 库：

| 分类 | 表名 | 说明 |
|------|------|------|
| 数据同步 | sys_user | 人员表（从上游同步） |
| 数据同步 | sys_dept | 部门表（从上游同步） |
| 权限 | sys_menu | 菜单表（目录/菜单/按钮三级） |
| 权限 | sys_role | 角色表 |
| 权限 | sys_role_menu | 角色-菜单关联 |
| 权限 | sys_user_role | 用户-角色关联 |
| 权限 | sys_role_dept | 角色-部门关联（数据权限） |
| 业务 | portal_category | 应用分类 |
| 业务 | portal_app | 应用信息 |
| 日志 | sys_operation_log | 操作日志 |

主键策略：雪花算法（ASSIGN_ID），通过 MyBatis-Plus 自动填充 createTime/updateTime。

## 七、操作日志

通过 AOP 切面 + 自定义注解实现：

1. 在需要记录日志的 Controller 方法上添加 `@OperLog("操作描述")`
2. `OperLogAspect` 拦截被注解的方法，记录请求参数、响应状态、耗时等
3. 异步写入 `sys_operation_log` 表，不影响接口性能

## 八、数据同步（预留）

- **DataSyncService** — 定义了 syncUsers()、syncDepts() 接口
- **DataSyncScheduler** — 每天凌晨 2 点定时执行同步
- **同步后处理** — 清理 Redis 权限缓存 + 通知 auth-svc 刷新

当前实现为空壳，待上游数据源确定后补充具体逻辑。

## 九、Nacos 配置规划

```
namespace: portal
├── group: SHARED（共享配置）
│   ├── common-mysql.yaml      ← MySQL 连接信息
│   ├── common-redis.yaml      ← Redis 连接信息
│   └── common-satoken.yaml    ← Sa-Token 配置
│
├── group: DEFAULT_GROUP（各服务配置）
│   ├── portal-gateway.yaml    ← 网关专属配置
│   ├── portal-auth.yaml       ← 认证服务配置
│   ├── portal-system.yaml     ← 系统服务配置
│   └── portal-business.yaml   ← 业务服务配置
```

各服务通过 `bootstrap.yml` 指定 Nacos 地址和命名空间，运行时可通过 `-DNACOS_ADDR=host:port` 覆盖。

## 十、前端架构

### 请求流转

```
浏览器 → http://localhost → Nginx(:80)
                              │
                              ├── 页面请求(/)      → Vite Dev Server(:3000)
                              └── API请求(/api/**)  → Gateway(:8080) → 各微服务
```

### 动态路由

1. 用户登录后，后端返回菜单树
2. 前端将菜单树转换为 Vue Router 路由配置
3. 通过 `router.addRoute()` 动态注册
4. 页面组件使用 `import.meta.glob` 按约定路径自动匹配

### 状态管理（Pinia）

- **useUserStore** — 用户信息、菜单树、权限列表、登录/登出

### 目录结构

```
portal-web/src/
├── api/            ← 8 个接口模块（与后端一一对应）
├── router/         ← 路由配置 + 动态路由生成
├── store/          ← Pinia 状态管理
├── layout/         ← 布局组件（侧边栏 + 顶栏）
├── views/          ← 页面组件
│   ├── portal/     ← 门户首页
│   ├── system/     ← 系统管理（user/role/menu/dept）
│   ├── log/        ← 操作日志
│   └── app/        ← 应用管理
├── directives/     ← v-permission 权限指令
└── utils/          ← request.js（Axios）、auth.js（Token）
```
