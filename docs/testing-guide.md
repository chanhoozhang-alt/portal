# 测试指南

## 一、环境准备

### 1.1 中间件检查清单

| 中间件 | 端口 | 检查命令 | 预期结果 |
|--------|------|----------|----------|
| MySQL 8.0 | 3306 | `mysql -u root -p -e "SELECT 1"` | 返回 `1` |
| Redis | 6379 | `redis-cli ping` | 返回 `PONG` |
| Nacos 2.2.3 | 8848 | 浏览器访问 `http://localhost:8848/nacos` | 能打开控制台 |

任一中间件未就绪，需先启动后再进行测试。

### 1.2 初始化数据库

```bash
mysql -u root -p < sql/portal-init.sql
```

执行后验证：

```sql
USE portal;
SHOW TABLES;
-- 应返回 10 张表
SELECT COUNT(*) FROM sys_user;
-- 应返回 3（初始用户：admin、张三、李四）
```

### 1.3 配置 Nacos

1. 访问 `http://localhost:8848/nacos`（nacos/nacos）
2. 命名空间管理 → 新建命名空间 `portal`
3. 配置管理 → 在 `SHARED` 组下创建以下共享配置：

**common-mysql.yaml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/portal?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的MySQL密码
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

### 1.4 启动后端服务

```bash
# 编译（项目根目录下）
mvn clean package -DskipTests

# 按顺序启动 4 个服务
java -jar portal-gateway/target/portal-gateway-1.0.0.jar
java -jar portal-auth/target/portal-auth-1.0.0.jar
java -jar portal-system/target/portal-system-1.0.0.jar
java -jar portal-business/target/portal-business-1.0.0.jar
```

或在 IDEA 中依次运行各模块的启动类。

### 1.5 启动前端

```bash
cd portal-web
pnpm install
pnpm dev
```

### 1.6 启动验证

| 验证项 | 方式 | 预期 |
|--------|------|------|
| Nacos 服务注册 | 控制台 → 服务列表 | 看到 4 个实例 |
| 网关可达 | 浏览器访问 `http://localhost:8080` | 有响应（非连接失败） |
| 前端可达 | 浏览器访问 `http://localhost:3000` | 页面正常渲染 |

---

## 二、接口测试

以下测试使用 curl 命令，需将 `<token>` 替换为实际的 Authorization 值。

### 2.1 认证模块

#### 2.1.1 SSO 登录验证

```bash
curl -X POST http://localhost:8080/api/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"token": "上游系统传入的JWT Token"}'
```

**验证点：**
- 返回 `code: 200`
- `data.user` 包含 id、empNo、realName、roles、permissions
- `data.menus` 返回菜单树
- `data.permissions` 返回权限标识列表

#### 2.1.2 获取当前用户信息

```bash
curl http://localhost:8080/api/auth/permissions \
  -H "Authorization: <token>"
```

#### 2.1.3 获取当前用户菜单树

```bash
curl http://localhost:8080/api/auth/menus \
  -H "Authorization: <token>"
```

**验证点：**
- admin 用户应返回所有菜单
- 普通用户只返回门户首页

#### 2.1.4 登出

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: <token>"
```

**验证点：**
- 返回成功
- 再次使用该 token 请求其他接口应返回未授权

### 2.2 人员管理

#### 2.2.1 分页查询用户

```bash
# 默认分页
curl "http://localhost:8080/api/system/users/page" \
  -H "Authorization: <token>"

# 带条件查询
curl "http://localhost:8080/api/system/users/page?realName=张&pageNum=1&pageSize=10" \
  -H "Authorization: <token>"
```

**验证点：**
- 返回分页数据，含 records、total、size、current
- realName 模糊查询生效

#### 2.2.2 更新用户状态

```bash
# 禁用用户
curl -X PUT "http://localhost:8080/api/system/users/2/status?status=0" \
  -H "Authorization: <token>"

# 恢复启用
curl -X PUT "http://localhost:8080/api/system/users/2/status?status=1" \
  -H "Authorization: <token>"
```

#### 2.2.3 分配用户角色

```bash
# 给张三(id=2)分配管理员角色
curl -X POST "http://localhost:8080/api/system/users/2/roles" \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '[1]'
```

#### 2.2.4 获取用户角色列表

```bash
curl "http://localhost:8080/api/system/users/2/roles" \
  -H "Authorization: <token>"
```

### 2.3 角色管理

#### 2.3.1 分页查询角色

```bash
curl "http://localhost:8080/api/system/roles/page" \
  -H "Authorization: <token>"
```

#### 2.3.2 查询所有角色（下拉选择用）

```bash
curl "http://localhost:8080/api/system/roles/list" \
  -H "Authorization: <token>"
```

#### 2.3.3 新增角色

```bash
curl -X POST http://localhost:8080/api/system/roles \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"roleName":"编辑员","roleKey":"editor","sort":2,"status":1}'
```

#### 2.3.4 修改角色

```bash
curl -X PUT http://localhost:8080/api/system/roles \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"id":3,"roleName":"内容编辑","roleKey":"editor","sort":2,"status":1}'
```

#### 2.3.5 删除角色

```bash
curl -X DELETE "http://localhost:8080/api/system/roles/3" \
  -H "Authorization: <token>"
```

**验证点：**
- 角色下存在用户时，应提示不允许删除

#### 2.3.6 分配菜单权限

```bash
curl -X POST "http://localhost:8080/api/system/roles/2/menus" \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '[1, 8, 9]'
```

#### 2.3.7 获取角色已分配的菜单

```bash
curl "http://localhost:8080/api/system/roles/2/menus" \
  -H "Authorization: <token>"
```

#### 2.3.8 分配数据权限（部门）

```bash
curl -X POST "http://localhost:8080/api/system/roles/2/depts" \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '[1, 2]'
```

### 2.4 菜单管理

#### 2.4.1 获取菜单列表

```bash
curl "http://localhost:8080/api/system/menus/list" \
  -H "Authorization: <token>"
```

#### 2.4.2 新增菜单

```bash
# 新增目录
curl -X POST http://localhost:8080/api/system/menus \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"menuName":"数据看板","parentId":0,"path":"/dashboard","component":null,"icon":"chart","menuType":"D","permission":null,"sort":3,"status":1}'

# 新增菜单
curl -X POST http://localhost:8080/api/system/menus \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"menuName":"报表查看","parentId":11,"path":"/dashboard/report","component":"dashboard/Report","icon":"report","menuType":"M","permission":"dashboard:report:list","sort":1,"status":1}'

# 新增按钮
curl -X POST http://localhost:8080/api/system/menus \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"menuName":"导出报表","parentId":12,"path":null,"component":null,"icon":null,"menuType":"B","permission":"dashboard:report:export","sort":1,"status":1}'
```

#### 2.4.3 修改菜单

```bash
curl -X PUT http://localhost:8080/api/system/menus \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"id":11,"menuName":"数据分析","parentId":0,"path":"/analytics","component":null,"icon":"chart","menuType":"D","sort":3,"status":1}'
```

#### 2.4.4 删除菜单

```bash
curl -X DELETE "http://localhost:8080/api/system/menus/13" \
  -H "Authorization: <token>"
```

**验证点：**
- 存在子菜单时不允许删除

### 2.5 部门管理

#### 2.5.1 获取部门列表

```bash
curl "http://localhost:8080/api/system/depts/list" \
  -H "Authorization: <token>"
```

#### 2.5.2 获取部门树

```bash
curl "http://localhost:8080/api/system/depts/tree" \
  -H "Authorization: <token>"
```

**验证点：**
- 返回树形结构，children 正确嵌套

#### 2.5.3 新增部门

```bash
curl -X POST http://localhost:8080/api/system/depts \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"deptName":"财务部","parentId":1,"sort":3,"status":1}'
```

#### 2.5.4 修改部门

```bash
curl -X PUT http://localhost:8080/api/system/depts \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"id":4,"deptName":"财务审计部","parentId":1,"sort":3,"status":1}'
```

#### 2.5.5 删除部门

```bash
curl -X DELETE "http://localhost:8080/api/system/depts/4" \
  -H "Authorization: <token>"
```

**验证点：**
- 存在子部门时不允许删除

### 2.6 操作日志

#### 2.6.1 分页查询日志

```bash
# 默认分页
curl "http://localhost:8080/api/system/logs/page" \
  -H "Authorization: <token>"

# 按操作人筛选
curl "http://localhost:8080/api/system/logs/page?username=admin&pageNum=1&pageSize=10" \
  -H "Authorization: <token>"
```

**验证点：**
- 执行过增删改操作后，日志表有对应记录
- 分页参数生效

### 2.7 应用管理

#### 2.7.1 分页查询应用

```bash
curl "http://localhost:8080/api/app/page" \
  -H "Authorization: <token>"
```

#### 2.7.2 查询所有启用应用

```bash
curl "http://localhost:8080/api/app/list" \
  -H "Authorization: <token>"
```

#### 2.7.3 新增应用

```bash
curl -X POST http://localhost:8080/api/app \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"appName":"Wiki","appUrl":"https://wiki.xxx.com","appIcon":"wiki","appDesc":"知识库","categoryId":2,"sort":2,"status":1}'
```

#### 2.7.4 修改应用

```bash
curl -X PUT http://localhost:8080/api/app \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"id":5,"appName":"Confluence","appUrl":"https://wiki.xxx.com","appIcon":"wiki","appDesc":"企业知识库","categoryId":2,"sort":2,"status":1}'
```

#### 2.7.5 删除应用

```bash
curl -X DELETE "http://localhost:8080/api/app/5" \
  -H "Authorization: <token>"
```

### 2.8 分类管理

#### 2.8.1 获取所有分类

```bash
curl "http://localhost:8080/api/category/list" \
  -H "Authorization: <token>"
```

#### 2.8.2 获取启用的分类

```bash
curl "http://localhost:8080/api/category/enabled" \
  -H "Authorization: <token>"
```

#### 2.8.3 新增分类

```bash
curl -X POST http://localhost:8080/api/category \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"categoryName":"测试分类","sort":3,"icon":"test","status":1}'
```

#### 2.8.4 修改分类

```bash
curl -X PUT http://localhost:8080/api/category \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"id":4,"categoryName":"测试分类改名","sort":3,"icon":"test","status":1}'
```

#### 2.8.5 删除分类

```bash
curl -X DELETE "http://localhost:8080/api/category/4" \
  -H "Authorization: <token>"
```

### 2.9 门户首页

#### 2.9.1 获取首页数据

```bash
curl "http://localhost:8080/api/portal/index" \
  -H "Authorization: <token>"
```

**验证点：**
- 返回分类列表，每个分类下包含对应的应用列表
- 只返回 `status=1` 的分类和应用

---

## 三、前端功能测试

### 3.1 登录流程

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 从上游系统带 token 跳转到 `http://localhost:3000?token=xxx` | 自动完成登录，跳转到门户首页 |
| 2 | 无 token 直接访问 `http://localhost:3000` | 提示未登录或跳转到上游登录页 |
| 3 | 使用无效 token 访问 | 提示 Token 无效 |

### 3.2 门户首页

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 查看首页 | 显示分类列表和各分类下的应用卡片 |
| 2 | 点击应用卡片 | 跳转到对应的应用 URL |
| 3 | 以普通用户登录 | 只能看到门户首页，无系统管理菜单 |

### 3.3 系统管理 - 人员管理

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击 系统管理 → 人员管理 | 显示用户分页列表 |
| 2 | 输入姓名搜索 | 列表按姓名模糊过滤 |
| 3 | 点击禁用/启用 | 用户状态切换成功 |
| 4 | 点击分配角色 | 弹出角色选择，保存后角色更新 |
| 5 | 无 `system:user:list` 权限的用户访问 | 不显示该菜单 |

### 3.4 系统管理 - 角色管理

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击 角色管理 | 显示角色分页列表 |
| 2 | 新增角色 | 填写表单保存成功 |
| 3 | 编辑角色 | 修改信息保存成功 |
| 4 | 删除无用户的角色 | 删除成功 |
| 5 | 删除有用户的角色 | 提示不允许删除 |
| 6 | 分配菜单权限 | 勾选菜单树保存，再次打开回显正确 |
| 7 | 分配数据权限 | 勾选部门树保存，再次打开回显正确 |

### 3.5 系统管理 - 菜单管理

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击 菜单管理 | 显示菜单平铺列表 |
| 2 | 新增目录 | 选择 menuType=D，填写信息保存成功 |
| 3 | 新增菜单 | 选择 menuType=M，填写组件路径保存成功 |
| 4 | 新增按钮 | 选择 menuType=B，填写权限标识保存成功 |
| 5 | 删除有子菜单的菜单 | 提示不允许删除 |
| 6 | 删除叶子菜单 | 删除成功 |

### 3.6 系统管理 - 部门管理

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击 部门管理 | 以树形结构展示部门 |
| 2 | 新增子部门 | 选择上级部门，填写信息保存成功 |
| 3 | 编辑部门 | 修改信息保存成功 |
| 4 | 删除有子部门的部门 | 提示不允许删除 |
| 5 | 删除叶子部门 | 删除成功 |

### 3.7 系统管理 - 操作日志

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击 操作日志 | 显示日志分页列表 |
| 2 | 按操作人搜索 | 列表按操作人过滤 |
| 3 | 按操作描述搜索 | 列表按操作描述过滤 |
| 4 | 执行增删改操作后查看日志 | 日志列表出现新记录 |

### 3.8 应用管理

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击 应用管理 → 应用列表 | 显示应用分页列表 |
| 2 | 新增应用 | 填写表单，选择分类保存成功 |
| 3 | 编辑应用 | 修改信息保存成功 |
| 4 | 删除应用 | 删除成功，首页同步消失 |
| 5 | 禁用应用 | 首页不再展示该应用 |
| 6 | 点击 应用管理 → 分类管理 | 显示分类列表 |
| 7 | 新增分类 | 填写信息保存成功 |
| 8 | 编辑/删除分类 | 操作成功 |

---

## 四、权限测试

### 4.1 菜单可见性

使用不同角色账号登录，验证菜单显示是否符合预期：

| 角色 | 应可见菜单 | 不应可见菜单 |
|------|------------|--------------|
| admin（超级管理员） | 全部菜单和按钮 | 无 |
| user（普通用户） | 门户首页 | 系统管理、应用管理 |

### 4.2 接口鉴权

| 测试场景 | 操作 | 预期 |
|----------|------|------|
| 无 token 请求 | 不带 Authorization 头访问受保护接口 | 返回未授权（401） |
| 无效 token | 使用随机字符串作为 token | 返回未授权（401） |
| 过期 token | 使用过期 token | 返回未授权（401） |
| 权限不足 | 普通用户访问 `system:user:list` 接口 | 返回无权限（403） |
| 正常访问 | admin 用户访问任意接口 | 正常返回数据 |

### 4.3 按钮权限

| 测试场景 | 操作 | 预期 |
|----------|------|------|
| 无新增权限 | 以无 `system:user:add` 权限的账号查看人员管理 | 新增按钮不可见或禁用 |
| 无删除权限 | 以无 `system:user:delete` 权限的账号查看人员管理 | 删除按钮不可见或禁用 |
| 有完整权限 | admin 账号查看任意页面 | 所有操作按钮可见且可用 |

---

## 五、边界与异常测试

### 5.1 参数校验

| 测试项 | 操作 | 预期 |
|--------|------|------|
| 分页参数越界 | `pageNum=-1` 或 `pageSize=0` | 使用默认值或返回参数错误 |
| 必填字段为空 | 新增角色时 roleName 传空 | 返回参数校验失败 |
| 非法 ID | 访问 `/api/system/roles/99999` | 返回数据不存在 |
| SQL 注入 | realName 传入 `' OR 1=1 --` | 参数被转义，不返回异常数据 |
| XSS | appName 传入 `<script>alert(1)</script>` | 内容被转义存储 |

### 5.2 并发与数据一致性

| 测试项 | 操作 | 预期 |
|--------|------|------|
| 角色删除冲突 | 两个用户同时删除同一角色 | 只有一个成功，另一个提示已不存在 |
| 状态快速切换 | 快速多次点击启用/禁用 | 最终状态与最后一次操作一致 |

### 5.3 服务异常

| 测试项 | 操作 | 预期 |
|--------|------|------|
| Redis 宕机 | 停止 Redis 后访问接口 | 返回明确错误信息，不暴露堆栈 |
| 某服务停止 | 停止 portal-system 后访问系统管理接口 | 网关返回服务不可用提示 |
| Nacos 不可用 | 停止 Nacos 后重启服务 | 服务启动失败并报连接错误 |

---

## 六、测试用例清单汇总

| 编号 | 模块 | 用例名称 | 优先级 |
|------|------|----------|--------|
| T01 | 认证 | SSO 登录验证成功 | P0 |
| T02 | 认证 | 无效 Token 登录 | P0 |
| T03 | 认证 | 获取用户信息和菜单 | P0 |
| T04 | 认证 | 登出后 Token 失效 | P0 |
| T05 | 认证 | 权限缓存刷新 | P1 |
| T06 | 人员 | 分页查询用户 | P0 |
| T07 | 人员 | 条件模糊搜索 | P1 |
| T08 | 人员 | 更新用户状态 | P0 |
| T09 | 人员 | 分配/查询用户角色 | P0 |
| T10 | 角色 | 分页查询角色 | P0 |
| T11 | 角色 | 新增/修改/删除角色 | P0 |
| T12 | 角色 | 删除有用户的角色被拒绝 | P0 |
| T13 | 角色 | 分配菜单权限并回显 | P0 |
| T14 | 角色 | 分配数据权限（部门）并回显 | P1 |
| T15 | 菜单 | 获取菜单列表 | P0 |
| T16 | 菜单 | 新增目录/菜单/按钮 | P0 |
| T17 | 菜单 | 删除有子菜单的菜单被拒绝 | P0 |
| T18 | 部门 | 获取部门树 | P0 |
| T19 | 部门 | 新增/修改/删除部门 | P0 |
| T20 | 部门 | 删除有子部门的部门被拒绝 | P0 |
| T21 | 日志 | 分页查询日志 | P1 |
| T22 | 日志 | 增删改操作产生日志记录 | P1 |
| T23 | 应用 | 分页查询应用 | P0 |
| T24 | 应用 | 新增/修改/删除应用 | P0 |
| T25 | 应用 | 禁用应用后首页不展示 | P1 |
| T26 | 分类 | 获取启用的分类 | P0 |
| T27 | 分类 | 新增/修改/删除分类 | P0 |
| T28 | 首页 | 获取首页数据（分类+应用） | P0 |
| T29 | 权限 | admin 可见全部菜单 | P0 |
| T30 | 权限 | 普通用户只可见门户首页 | P0 |
| T31 | 权限 | 无 Token 请求被拦截 | P0 |
| T32 | 权限 | 权限不足时接口返回 403 | P0 |
| T33 | 权限 | 按钮级别权限控制 | P1 |
| T34 | 边界 | 参数校验（空值、越界） | P1 |
| T35 | 边界 | SQL 注入防护 | P1 |
| T36 | 边界 | XSS 防护 | P1 |
| T37 | 异常 | Redis 宕机错误处理 | P2 |
| T38 | 异常 | 服务不可用时的网关响应 | P2 |
