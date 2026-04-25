# 门户管理系统

内部门户管理系统，提供 SSO 登录、RBAC 权限控制、应用展示与管理、操作日志等功能。人员/部门数据从上游系统同步。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8 | Java 运行环境 |
| Spring Boot | 2.7.18 | 基础框架 |
| Spring Cloud | 2021.0.8 | 微服务框架 |
| Spring Cloud Alibaba | 2021.0.5.0 | Nacos 注册/配置中心 |
| Spring Cloud Gateway | - | 服务网关 |
| OpenFeign | - | 服务间调用 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Sa-Token | 1.38.0 | 认证授权（JWT） |
| MySQL | 8.0 | 数据库 |
| Redis | - | 缓存 + 会话存储 |
| SpringDoc | 1.7.0 | 接口文档 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.x | 前端框架 |
| Element Plus | 2.13.x | UI 组件库 |
| Vite | 8.x | 构建工具 |
| Pinia | 3.x | 状态管理 |
| Vue Router | 5.x | 路由管理 |
| Axios | 1.x | HTTP 请求 |

## 项目结构

```
portal/
├── pom.xml                    # 父 POM（依赖管理）
├── sql/
│   └── portal-init.sql        # 数据库初始化脚本
├── portal-common/             # 公共模块（实体、VO、工具、Feign 接口）
├── portal-gateway/            # 网关服务（端口 8080）
├── portal-auth/               # 认证授权服务（端口 9001）
├── portal-system/             # 系统管理服务（端口 9002）
├── portal-business/           # 门户业务服务（端口 9003）
├── portal-web/                # 前端项目（端口 3000）
├── nginx/                     # Nginx 配置（Docker 开发环境）
│   └── nginx.conf             # Nginx 配置文件
└── docs/                      # 项目文档
    ├── deployment.md           # 部署文档
    ├── api.md                  # 接口文档
    └── architecture.md         # 技术架构文档
```

## 服务说明

| 服务 | 端口 | 职责 |
|------|------|------|
| portal-gateway | 8080 | 统一入口，JWT 校验，路由转发 |
| portal-auth | 9001 | JWT 验证、登录建会话、查询角色/权限/菜单 |
| portal-system | 9002 | 人员/部门/角色/菜单管理、数据同步、操作日志 |
| portal-business | 9003 | 应用管理、分类管理、门户首页 |

## 快速开始

### 环境要求

- JDK 1.8
- MySQL 8.0
- Redis 6+
- Nacos 2.2.3（单机模式）
- Node.js 18+、pnpm
- Docker（运行 Nginx）

### 1. 初始化数据库

```bash
mysql -u root -p < sql/portal-init.sql
```

### 2. 启动 Nacos

```bash
# 下载 Nacos 2.2.3 并启动单机模式
sh startup.sh -m standalone
```

在 Nacos 控制台创建命名空间 `portal`，并在 `SHARED` 组下创建共享配置：

- `common-mysql.yaml` — MySQL 连接信息
- `common-redis.yaml` — Redis 连接信息
- `common-satoken.yaml` — Sa-Token 配置

共享配置示例：

```yaml
# common-mysql.yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/portal?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

```yaml
# common-redis.yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
```

```yaml
# common-satoken.yaml
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

### 3. 启动后端服务

按顺序启动：

```bash
# 1. 网关
cd portal-gateway && mvn spring-boot:run

# 2. 认证服务
cd portal-auth && mvn spring-boot:run

# 3. 系统管理服务
cd portal-system && mvn spring-boot:run

# 4. 业务服务
cd portal-business && mvn spring-boot:run
```

启动后可在 Nacos 控制台看到 4 个注册实例。

### 4. 启动前端

```bash
cd portal-web
pnpm install
pnpm dev
```

### 5. 启动 Nginx（开发环境）

```bash
docker run -d --name portal-nginx -p 80:80 \
  -v "$(pwd)/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" nginx:stable
```

访问 `http://localhost`，从上游系统带 token 跳转即可登录。

> Nginx 作为统一入口：页面请求转发给 Vite（3000），API 请求转发给网关（8080）。

## 相关文档

- [部署文档](docs/deployment.md)
- [接口文档](docs/api.md)
- [技术架构文档](docs/architecture.md)
