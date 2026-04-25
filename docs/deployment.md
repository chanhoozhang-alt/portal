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

#### 日常启动顺序

```
1. 启动网关（8080）
2. 启动 Vite：cd portal-web && pnpm dev
3. 启动 Nginx：docker start portal-nginx（首次用 docker run，后续用 docker start）
4. 浏览器访问 http://localhost
```

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

## 五、验证

| 验证项 | 方式 |
|--------|------|
| Nacos 注册 | 控制台 `服务列表` 看到 4 个实例 |
| 网关路由 | `curl http://localhost:8080/api/auth/logout` |
| 前端访问 | 浏览器打开 `http://localhost`（开发环境通过 Nginx:80） |
| 登录流程 | 从上游系统带 token 跳转到门户 |
| 接口文档 | `http://localhost:9002/swagger-ui.html`（如已配置） |

## 六、常见问题

### Q: 服务启动报 Nacos 连接失败

检查 Nacos 是否启动，`NACOS_ADDR` 环境变量是否正确，命名空间 `portal` 是否已创建。

### Q: 前端请求 404

确认后端 Gateway 已启动在 8080 端口，Nginx 容器正在运行（`docker ps --filter name=portal-nginx`），`nginx/nginx.conf` 中 `proxy_pass` 地址正确。

### Q: 登录提示 Token 无效

确认 Redis 已启动，Sa-Token 的 JWT 密钥在 Nacos 共享配置中正确配置。

### Q: Maven 编译下载依赖慢

配置国内镜像源（阿里云），或在有网环境先 `mvn dependency:resolve` 下载依赖。
