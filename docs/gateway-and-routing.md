# 网关与页面路由流程

## 一、整体架构

本项目采用前后端分离的微服务架构，各层职责如下：

```
浏览器 ──→ Nginx(:80) ──→ 网关(8080) ──→ 各微服务
              │                            ├─ portal-auth    (9001) 认证授权
              │                            ├─ portal-system  (9002) 系统管理
              │                            └─ portal-business(9003) 门户业务
              │
              └──→ Vite(:3000) 页面服务（开发环境）
```

| 层级 | 技术栈 | 端口 | 职责 |
|------|--------|------|------|
| Nginx | Docker + Nginx | 80 | 统一入口，页面转发 + API 反向代理 |
| 前端 | Vue 3 + Element Plus + Vue Router + Pinia | 3000 | 页面渲染、路由管理、Token 管理 |
| 网关 | Spring Cloud Gateway (WebFlux) | 8080 | JWT 鉴权、路由分发、跨域处理 |
| 认证服务 | Spring Boot + Sa-Token | 9001 | 登录验证、权限查询、菜单获取 |
| 系统服务 | Spring Boot + MyBatis-Plus | 9002 | 用户/角色/菜单/部门/日志管理 |
| 业务服务 | Spring Boot + MyBatis-Plus | 9003 | 门户首页、应用管理、分类管理 |

中间件依赖：MySQL、Redis、Nacos（注册中心 + 配置中心，namespace: `portal`）

---

## 二、页面访问流程

```
浏览器输入地址
    ↓
前端 Vue Router 匹配路由
    ↓
检查本地是否有 Token？
    ├─ 无 → 重定向到 /login 登录页
    └─ 有 → 渲染对应页面组件
```

### 2.1 前端路由结构

| 路径 | 组件 | 说明 |
|------|------|------|
| `/login` | Login.vue | 登录页 |
| `/` | 重定向到 `/portal` | 默认入口 |
| `/portal` | portal/Index.vue | 门户首页 |
| `/system/user` | system/user/Index.vue | 人员管理 |
| `/system/role` | system/role/Index.vue | 角色管理 |
| `/system/menu` | system/menu/Index.vue | 菜单管理 |
| `/system/dept` | system/dept/Index.vue | 部门管理 |
| `/system/log` | log/Index.vue | 操作日志 |
| `/app/list` | app/Index.vue | 应用列表 |
| `/app/category` | app/Category.vue | 分类管理 |
| `/:pathMatch(.*)*` | 404.vue | 未匹配路由 |

### 2.2 动态路由生成

前端路由并非全部静态写死，而是根据后端返回的菜单树动态生成。菜单分三种类型：

| menuType | 含义 | 作用 |
|----------|------|------|
| `D`（目录） | 如"系统管理"、"应用管理" | 仅做分组，不对应具体页面 |
| `M`（菜单） | 如"人员管理"、"角色管理" | 对应一个 Vue 页面组件，会注册为前端路由 |
| `B`（按钮） | 如"用户新增"、"用户编辑" | 控制页面内按钮的显示/隐藏，不注册路由 |

流程：

```
登录成功 → 后端返回菜单树 → 前端遍历 menuType=M 的节点
    → 根据 path 和 component 字段动态添加路由
    → menuType=B 的节点存入权限列表，用于控制按钮显隐
```

---

## 三、登录认证流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  浏览器   │    │  前端     │    │   网关    │    │  Auth服务 │
└────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘
     │               │               │               │
     │ 1. 输入工号    │               │               │
     │──────────────→│               │               │
     │               │               │               │
     │               │ 2. POST /api/auth/test-login   │
     │               │  (或 /api/auth/verify SSO登录) │
     │               │──────────────→│               │
     │               │               │               │
     │               │               │ 3. 白名单放行   │
     │               │               │──────────────→│
     │               │               │               │
     │               │               │               │ 4. 验证登录
     │               │               │               │ Sa-Token 登录
     │               │               │               │ 查询用户信息
     │               │               │               │ 查询权限菜单
     │               │               │               │
     │               │ 5. 返回 {user, menus, permissions, token} │
     │               │←──────────────│←──────────────│
     │               │               │               │
     │               │ 6. 存储 Token，生成动态路由      │
     │               │               │               │
     │ 7. 跳转到首页  │               │               │
     │←──────────────│               │               │
```

### 3.1 认证方式

| 方式 | 接口 | 适用场景 |
|------|------|---------|
| SSO 登录 | `POST /api/auth/verify` | 生产环境，上游系统传入 Token |
| 测试登录 | `POST /api/auth/test-login` | 开发测试，通过工号直接登录 |

---

## 四、API 请求流程

前端发起的每一个 API 请求都经过以下链路：

```
前端发起请求（如 GET /api/system/users/page）
    ↓
浏览器 → Nginx(:80) → /api/ 匹配 → 转发到网关(:8080)
    ↓
网关 AuthGlobalFilter（执行顺序: order=-1）
    ↓
检查是否在白名单中？
    ├─ 是（/api/auth/verify、test-login、logout）→ 直接放行
    └─ 否 → 从 Header 取 Authorization
         ├─ 无 Token 或 JWT 验证失败 → 返回 401 Unauthorized
         └─ 验证通过 → 解析工号，写入 Header X-User-EmpNo → 放行
    ↓
网关路由匹配（根据 path 前缀分发）
    ↓
下游微服务处理请求 → 返回结果 → 原路返回给前端
```

### 4.1 网关路由规则

| 路径前缀 | 目标服务 | 负载均衡 |
|----------|---------|---------|
| `/api/auth/**` | portal-auth (9001) | `lb://portal-auth` |
| `/api/system/**` | portal-system (9002) | `lb://portal-system` |
| `/api/portal/**` | portal-business (9003) | `lb://portal-business` |
| `/api/app/**` | portal-business (9003) | `lb://portal-business` |
| `/api/category/**` | portal-business (9003) | `lb://portal-business` |

### 4.2 网关白名单

以下路径不需要携带 Token：

| 路径 | 方法 | 用途 |
|------|------|------|
| `/api/auth/verify` | POST | SSO 登录验证 |
| `/api/auth/test-login` | POST | 测试登录（开发环境） |
| `/api/auth/logout` | POST | 登出 |

### 4.3 网关跨域配置（CorsConfig）

网关全局配置了 CORS，允许前端跨域访问：

- 允许来源：`*`
- 允许方法：GET、POST、PUT、DELETE、OPTIONS
- 允许头部：`*`
- 允许凭证：true
- 预检缓存：3600秒

---

## 五、环境部署

### 5.1 开发环境

开发环境通过 Docker 运行 Nginx，统一入口并保持与生产环境一致的 API 代理链路：

```bash
# 启动 Nginx 容器（项目根目录执行）
docker run -d --name portal-nginx -p 80:80 \
  -v "$(pwd)/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" nginx:stable
```

请求流转：

```
浏览器访问 http://localhost
    ↓
Nginx 判断路径
    ├─ 非 /api → 转发到 Vite Dev Server(:3000) → 页面 + 热更新
    └─ /api    → 转发到网关(:8080) → 微服务处理
```

### 5.2 生产环境

生产环境 Nginx 直接托管前端静态资源：

```nginx
server {
    listen 80;
    server_name portal.xxx.com;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;  # Vue Router history 模式
    }

    # API 反向代理到网关
    location /api {
        proxy_pass http://gateway:8080;
    }
}
```

请求流转：

```
浏览器访问 portal.xxx.com
    ↓
Nginx 判断路径
    ├─ 非 /api → 返回 Vue 静态文件（HTML/JS/CSS）
    └─ /api    → 反向代理到网关:8080 → 微服务处理
```

---

## 六、常见问题

### Q1: 直接访问 localhost:8080 为什么报 404？

8080 是纯 API 网关，不提供页面，只处理 `/api/**` 接口请求。页面需通过 Nginx（`http://localhost`）访问，Nginx 会将页面请求转发给 Vite Dev Server。

### Q2: 直接访问 /api/xxx 为什么报 401？

网关的 `AuthGlobalFilter` 会对白名单以外的所有请求进行 JWT 校验。必须在请求 Header 中携带 `Authorization: <token>`。获取 Token 的方式：

```bash
# 测试登录获取 Token
curl -X POST http://localhost:8080/api/auth/test-login \
  -H "Content-Type: application/json" \
  -d '{"empNo": "admin"}'
```

### Q3: 前端页面刷新后白屏/404？

Vue Router 使用 history 模式，开发环境下 Nginx 将所有非 `/api` 请求转发给 Vite Dev Server，由 Vite 处理路由回退。生产环境需在 Nginx 配置 `try_files $uri $uri/ /index.html`，将所有非文件路径回退到 index.html，由前端路由接管。

### Q4: 微服务启动顺序？

推荐顺序：Nacos → Redis → MySQL → portal-auth → portal-system → portal-business → portal-gateway。网关依赖其他服务先注册到 Nacos 才能正确路由。
