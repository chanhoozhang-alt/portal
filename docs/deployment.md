# 部署文档

## 一、环境要求

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| JDK | 1.8.0_301 | 1.8.0_最新 | 所有后端服务 |
| MySQL | 8.0 | 8.0.x | 数据库 |
| Redis | 6.0 | 7.x | 缓存 + 会话存储 |
| Nacos | 2.2.3 | 2.2.3 | 注册中心 + 配置中心 |
| Node.js | 18.0 | 20+ | 前端构建 |
| pnpm | 8.0 | 最新 | 前端包管理 |
| Maven | 3.6 | 3.9.x | 后端构建 |
| Nginx | 1.20 | 最新 | 前端部署（生产环境 / 开发环境 Docker） |
| Docker | 20.0 | 最新 | 开发环境运行 Nginx |

## 二、中间件部署

### 2.1 MySQL

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE portal DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 执行初始化脚本
mysql -u root -p portal < sql/portal-init.sql
```

### 2.2 Redis

```bash
# Linux
yum install redis
systemctl start redis
systemctl enable redis

# Docker
docker run -d --name redis -p 6379:6379 redis:7
```

### 2.3 Nacos

```bash
# 下载 Nacos 2.2.3
wget https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.tar.gz
tar -xzf nacos-server-2.2.3.tar.gz
cd nacos/bin

# 单机模式启动
sh startup.sh -m standalone    # Linux
startup.cmd -m standalone       # Windows
```

访问 `http://localhost:8848/nacos`（默认账号密码 nacos/nacos）。

### 2.4 配置 Nacos

#### 创建命名空间

在 Nacos 控制台创建命名空间 `portal`。

#### 创建共享配置（SHARED 组）

**common-mysql.yaml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/portal?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**common-redis.yaml**

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
```

**common-satoken.yaml**

```yaml
sa-token:
  token-name: Authorization
  timeout: 86400
  is-concurrent: true
  token-style: uuid
  alone-redis:
    database: 1
    host: ${spring.redis.host}
    port: ${spring.redis.port}
```

## 三、后端服务部署

### 3.1 编译打包

```bash
cd portal
mvn clean package -DskipTests
```

生成的 jar 包位于各模块的 `target/` 目录：

```
portal-gateway/target/portal-gateway-1.0.0.jar
portal-auth/target/portal-auth-1.0.0.jar
portal-system/target/portal-system-1.0.0.jar
portal-business/target/portal-business-1.0.0.jar
```

### 3.2 启动服务

按顺序启动，每个服务需等待上一个注册到 Nacos 后再启动下一个：

```bash
# 1. 网关
nohup java -jar portal-gateway/target/portal-gateway-1.0.0.jar > logs/gateway.log 2>&1 &

# 2. 认证服务
nohup java -jar portal-auth/target/portal-auth-1.0.0.jar > logs/auth.log 2>&1 &

# 3. 系统管理服务
nohup java -jar portal-system/target/portal-system-1.0.0.jar > logs/system.log 2>&1 &

# 4. 业务服务
nohup java -jar portal-business/target/portal-business-1.0.0.jar > logs/business.log 2>&1 &
```

### 3.3 自定义 JVM 参数

```bash
java -Xms256m -Xmx512m -jar portal-gateway-1.0.0.jar
```

### 3.4 指定 Nacos 地址

```bash
java -DNACOS_ADDR=192.168.1.100:8848 -jar portal-gateway-1.0.0.jar
```

## 四、前端部署

### 4.1 开发环境

#### 启动 Vite Dev Server

```bash
cd portal-web
pnpm install
pnpm dev
# Vite 运行在 3000 端口，监听所有网络接口（0.0.0.0）
```

#### 启动 Nginx（Docker）

开发环境通过 Docker 运行 Nginx，作为统一入口，使 API 代理链路与生产环境保持一致。

```bash
# 在项目根目录执行
docker run -d --name portal-nginx -p 80:80 \
  -v "$(pwd)/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" nginx:stable
```

Nginx 配置文件位于 `nginx/nginx.conf`，职责如下：

| 路径 | 转发目标 | 说明 |
|------|----------|------|
| `/` | Vite Dev Server (:3000) | 页面请求，支持 HMR 热更新 |
| `/api/` | 网关 (:8080) | API 请求，与生产环境链路一致 |

> 注意：Nginx 运行在 Docker 容器内，访问宿主机服务需使用 `host.docker.internal` 而非 `localhost`。

#### 启动顺序

整体启动顺序如下，每步需等上一步就绪后再执行：

```
1. MySQL（3306）— 数据库，基础依赖
2. Redis（6379）— 缓存 + 会话存储
3. Nacos（8848）— 注册中心 + 配置中心，单机模式
4. portal-gateway（8080）— 网关，后端统一入口
5. portal-auth（9001）— 认证授权服务
6. portal-system（9002）— 系统管理服务
7. portal-business（9003）— 业务服务
8. portal-web（3000）— 前端 Vite Dev Server
9. Nginx（80）— Docker 容器，统一入口代理
```

> 步骤 1-3 为中间件，需确保已安装并运行。步骤 4-7 为后端服务，每启动一个需等其注册到 Nacos 后再启动下一个。
> 步骤 8 和 9 可互换，但必须在网关之后。浏览器访问 http://localhost。

**详细启动命令：**

```bash
# ===== 中间件（Docker 容器） =====

# 1. MySQL
docker start mysql

# 2. Redis
docker start redis

# 3. Nacos（启动后等待端口就绪，约 10 秒）
docker start nacos

# ===== 后端服务 =====

# 4. 网关（8080），需等 Nacos 就绪后再启动
cd portal-gateway && mvn spring-boot:run

# 5. 认证服务（9001），需等网关注册到 Nacos
cd portal-auth && mvn spring-boot:run

# 6. 系统管理服务（9002），需等认证服务注册到 Nacos
cd portal-system && mvn spring-boot:run

# 7. 业务服务（9003），需等系统管理服务注册到 Nacos
cd portal-business && mvn spring-boot:run

# ===== 前端 =====

# 8. Vite Dev Server（3000）
cd portal-web && pnpm dev

# ===== Nginx =====

# 9. Nginx（首次启动用 docker run，后续用 docker start）
docker start portal-nginx
# 首次：
# docker run -d --name portal-nginx -p 80:80 \
#   -v "$(pwd)/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" nginx:stable
```

**判断服务就绪方式：**

```bash
# 检查端口是否在监听（Windows）
netstat -ano | grep ":<端口号>" | grep LISTEN

# 检查 Nacos 注册情况
# 浏览器访问 http://localhost:8848/nacos，查看服务列表
```

#### 常见启动问题

##### Q: 后端服务启动报 `Could not resolve dependencies` / `Could not find artifact com.xxx.portal:portal-common`

后端各模块依赖 `portal-common`，首次启动或清理本地 Maven 仓库后需先安装公共模块：

```bash
# 在项目根目录执行
mvn install -pl portal-common -am -DskipTests
```

安装完成后重新启动后端服务即可。

##### Q: 后端服务启动报 Nacos 连接失败

检查 Nacos 容器是否已启动且端口就绪（`netstat -ano | grep :8848`）。Nacos 启动后需要等待约 10 秒才能完全就绪。

##### Q: 前端请求 404

确认网关（8080）已启动，Nginx 容器正在运行（`docker ps --filter name=portal-nginx`）。

#### 管理 Nginx 容器

```bash
# 查看状态
docker ps --filter name=portal-nginx

# 重启（修改配置后）
docker restart portal-nginx

# 停止
docker stop portal-nginx

# 删除（需重新 docker run）
docker rm -f portal-nginx

# 查看日志
docker logs portal-nginx
```

### 4.2 生产构建

```bash
cd portal-web
pnpm build
# 产物在 dist/ 目录
```

### 4.3 Nginx 配置（生产环境）

```nginx
server {
    listen 80;
    server_name portal.xxx.com;

    # 前端静态资源
    location / {
        root /opt/portal-web/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到网关
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Host $host;
    }
}
```

## 五、Nginx 的作用详解

### 5.1 为什么需要 Nginx

本项目是前后端分离架构，浏览器同时需要获取**页面**和**接口数据**，这两类请求由不同的服务处理：

| 请求类型 | 处理方 | 说明 |
|----------|--------|------|
| 页面请求 `/` | 前端（静态文件） | HTML/JS/CSS，无需后端计算 |
| 接口请求 `/api/**` | 后端（网关 → 微服务） | 动态数据查询 |

如果没有 Nginx，浏览器需要分别访问不同端口，存在跨域问题，且用户需要记住多个地址。

Nginx 作为**统一入口**，根据请求路径分发到不同服务，浏览器只需访问一个地址。

### 5.2 Nginx 在架构中的位置

```
浏览器
  ↓ 所有请求都发到 Nginx
Nginx（统一入口，80 端口）
  │
  ├── 页面请求 /          → 前端静态文件（HTML/JS/CSS）
  │
  └── 接口请求 /api/**    → 网关（8080） → 各微服务 → 数据库
```

### 5.3 浏览器访问完整流程

#### 访问页面（GET /）

```
浏览器请求 GET /
  ↓
Nginx 收到请求，匹配 location / 规则
  ↓
返回 dist/index.html 给浏览器
  ↓
浏览器解析 HTML，发现引用了 JS/CSS 文件
  ↓
浏览器再次请求 /assets/index-abc123.js、/assets/index-def456.css
  ↓
Nginx 匹配 location / 规则，返回对应静态文件
  ↓
浏览器执行 JS，Vue 应用启动，Vue Router 渲染页面
```

#### 调用接口（GET /api/system/users/page）

```
JS 代码调用 axios.get('/api/system/users/page')
  ↓
浏览器发出 HTTP 请求到 Nginx
  ↓
Nginx 匹配 location /api/ 规则，转发到网关（8080）
  ↓
网关认证 + 路由 → 微服务 → 数据库
  ↓
响应 JSON 返回浏览器
  ↓
JS 拿到数据渲染页面
```

### 5.4 开发环境 vs 生产环境

#### 开发环境

```
浏览器 → Nginx(:80) → 页面请求 /   → Vite Dev Server(:3000)
                      → API请求 /api/ → 网关(:8080)
```

- Nginx 通过 Docker 运行
- 页面请求转发到 Vite Dev Server，支持热更新
- API 请求转发到网关，与生产环境链路一致

#### 生产环境

```
浏览器 → Nginx(:80) → 页面请求 /   → 直接返回 dist/ 静态文件
                      → API请求 /api/ → 网关(:8080)
```

- Nginx 直接托管打包后的静态文件，无需 Vite/Node.js 进程
- 静态文件由 `pnpm build` 生成，放在 `dist/` 目录

#### 关键区别

| 对比项 | 开发环境 | 生产环境 |
|--------|----------|----------|
| 页面服务 | Vite Dev Server（实时编译） | Nginx 直接返回静态文件 |
| 热更新 | 支持 | 不支持 |
| 需要 Node.js | 是（Vite 运行需要） | 否 |
| 性能 | 低（实时编译） | 高（直接返回文件） |
| Nginx 运行方式 | Docker 容器 | 直接安装或 Docker |

### 5.5 生产环境 Nginx 的额外能力

| 能力 | 说明 | 配置示例 |
|------|------|----------|
| SSL 终结 | HTTPS 证书配在 Nginx，后端不用管 | `listen 443 ssl;` |
| Gzip 压缩 | 压缩 JS/CSS，减少传输体积 | `gzip on; gzip_types text/css application/javascript;` |
| 静态资源缓存 | 浏览器缓存 JS/CSS，减少重复请求 | `expires 30d;` |
| 负载均衡 | 多个网关实例时分发请求 | `upstream gateway { server 8080; server 8081; }` |
| 安全 | 隐藏后端真实地址和端口 | 通过 `proxy_pass` 转发 |

### 5.6 如果不搞 Nginx 怎么办

有几种替代方案，但各有不足：

**方案一：Vite 代理（仅开发环境）**

```js
// vite.config.js
server: {
  proxy: {
    '/api': { target: 'http://localhost:8080' }
  }
}
```

浏览器访问 `localhost:3000`，Vite 同时提供页面和代理接口。但仅适用于开发环境，生产环境不能用 Vite。

**方案二：网关托管静态文件**

把打包后的 `dist` 放到网关服务中，网关同时处理页面和接口。但会让网关职责变重，违背单一职责原则。

**方案三：前后端分域名 + CORS**

前端 `portal.xxx.com`，接口 `api.xxx.com`，通过跨域配置解决。但跨域配置复杂，且多了一个域名。

**结论**：生产环境建议使用 Nginx，是前后端分离项目的标准做法。

## 六、验证

| 验证项 | 方式 |
|--------|------|
| Nacos 注册 | 控制台 `服务列表` 看到 4 个实例 |
| 网关路由 | `curl http://localhost:8080/api/auth/logout` |
| 前端访问 | 浏览器打开 `http://localhost`（开发环境通过 Nginx:80） |
| 登录流程 | 从上游系统带 token 跳转到门户 |
| 接口文档 | `http://localhost:9002/swagger-ui.html`（如已配置） |

## 七、浏览器访问流程

浏览器访问 `http://localhost` 的完整请求链路如下：

### 7.1 直接访问（无 token）

```
浏览器访问 http://localhost
  → Docker Nginx（80）匹配 location /
  → 转发到 Vite Dev Server（3000）
  → 返回 index.html，加载 Vue 应用
  → Vue 路由守卫检查 localStorage 中的 portal_token
  → 无 token，重定向到 /login
  → 显示登录页面
```

### 7.2 SSO 登录（从上游系统跳转）

```
上游系统跳转 http://localhost?token=xxx
  → Docker Nginx（80）→ Vite（3000）→ 返回 index.html
  → Vue 应用启动，extractTokenFromUrl() 从 URL 提取 token
  → 将 token 存入 localStorage（key: portal_token）
  → 清除 URL 中的 token 参数，地址栏变为干净的 URL
  → 进入 7.3「有 token」流程
```

### 7.3 已登录（有 token）

```
浏览器访问 http://localhost（localStorage 中有 token）
  → Docker Nginx（80）→ Vite（3000）→ 返回 index.html
  → Vue 路由守卫检查 localStorage 中的 portal_token
  → 有 token，调用 userStore.fetchUserInfo()
  → 发送请求 GET /api/auth/user/info，Header 带 Authorization: <token>
  → Nginx（80）匹配 location /api/
  → 转发到网关 portal-gateway（8080）
  → 网关校验 JWT token（Sa-Token）
    → token 无效/过期 → 返回 401 → 前端清除 token，跳转 /login
    → token 有效 → 根据路由规则转发到 portal-auth（9001）
  → portal-auth 返回用户信息（角色、权限、菜单）
  → 前端根据菜单动态生成路由
  → 重定向到 /portal（门户首页）
```

### 7.4 关键节点说明

| 节点 | 说明 |
|------|------|
| Nginx 路径分发 | `/` 转发 Vite（3000），`/api/` 转发网关（8080） |
| host.docker.internal | Docker 容器内访问宿主机的特殊域名 |
| token 存储 | localStorage，key 为 `portal_token` |
| API 请求基础路径 | axios 的 baseURL 为 `/api`，自动拼接到所有接口请求前 |
| 鉴权方式 | 请求头 `Authorization` 携带 token，网关通过 Sa-Token 校验 |
| 动态路由 | 登录后根据用户菜单权限动态注册路由，无权限的页面不可访问 |

## 八、常见问题

### Q: 服务启动报 Nacos 连接失败

检查 Nacos 是否启动，`NACOS_ADDR` 环境变量是否正确，命名空间 `portal` 是否已创建。

### Q: 前端请求 404

确认后端 Gateway 已启动在 8080 端口，Nginx 容器正在运行（`docker ps --filter name=portal-nginx`），`nginx/nginx.conf` 中 `proxy_pass` 地址正确。

### Q: 登录提示 Token 无效

确认 Redis 已启动，Sa-Token 的 JWT 密钥在 Nacos 共享配置中正确配置。

### Q: Maven 编译下载依赖慢

配置国内镜像源（阿里云），或在有网环境先 `mvn dependency:resolve` 下载依赖。
