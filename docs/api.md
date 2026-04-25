# 接口文档

基础路径：所有请求通过 Gateway（`http://host:8080`）转发，需在 Header 中携带 `Authorization: token`。

统一响应格式：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

---

## 一、认证模块（portal-auth）

### 1.1 SSO 登录验证

```
POST /api/auth/verify
```

**请求体：**

```json
{
  "token": "上游系统传入的 JWT Token"
}
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "user": {
      "id": 1,
      "empNo": "admin",
      "realName": "系统管理员",
      "deptName": "技术部",
      "roles": ["admin"],
      "permissions": ["system:user:list", "system:role:list", ...]
    },
    "menus": [
      {
        "id": 1,
        "menuName": "门户首页",
        "path": "/portal",
        "component": "portal/Index",
        "icon": "home",
        "menuType": "M",
        "children": []
      }
    ],
    "permissions": ["system:user:list", "system:role:list", ...]
  }
}
```

### 1.2 获取当前用户信息

```
GET /api/auth/permissions
```

响应格式同 1.1 的 data 部分。

### 1.3 获取当前用户菜单树

```
GET /api/auth/menus
```

**响应：** 返回当前用户有权限的菜单树。

### 1.4 登出

```
POST /api/auth/logout
```

### 1.5 刷新权限缓存

```
POST /api/auth/refreshCache
```

供其他服务通过 Feign 内部调用。

---

## 二、人员管理（portal-system）

### 2.1 分页查询用户

```
GET /api/system/users/page
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |
| realName | string | 否 | 姓名模糊查询 |
| empNo | string | 否 | 工号模糊查询 |
| status | int | 否 | 状态筛选（1启用 0禁用） |

**响应：**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "empNo": "admin",
        "username": "admin",
        "realName": "系统管理员",
        "email": "admin@xxx.com",
        "phone": "13800000000",
        "deptId": 2,
        "status": 1,
        "createTime": "2024-01-01 00:00:00"
      }
    ],
    "total": 3,
    "size": 10,
    "current": 1
  }
}
```

### 2.2 根据工号查询用户

```
GET /api/system/users/byEmpNo?empNo={empNo}
```

Feign 内部调用接口，返回用户完整信息（含角色、权限）。

### 2.3 更新用户状态

```
PUT /api/system/users/{id}/status?status={0|1}
```

### 2.4 分配用户角色

```
POST /api/system/users/{id}/roles
```

**请求体：** `[1, 2]`（角色 ID 列表）

### 2.5 获取用户角色列表

```
GET /api/system/users/{id}/roles
```

---

## 三、角色管理（portal-system）

### 3.1 分页查询角色

```
GET /api/system/roles/page
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |
| roleName | string | 否 | 角色名称模糊查询 |

### 3.2 查询所有角色

```
GET /api/system/roles/list
```

返回所有启用状态的角色，用于下拉选择。

### 3.3 新增角色

```
POST /api/system/roles
```

```json
{
  "roleName": "编辑员",
  "roleKey": "editor",
  "sort": 2,
  "status": 1
}
```

### 3.4 修改角色

```
PUT /api/system/roles
```

请求体同新增，需包含 `id`。

### 3.5 删除角色

```
DELETE /api/system/roles/{id}
```

删除前会校验该角色下是否存在用户。

### 3.6 分配菜单权限

```
POST /api/system/roles/{id}/menus
```

**请求体：** `[1, 2, 3, 100, 101]`（菜单 ID 列表，含按钮）

### 3.7 获取角色已分配的菜单 ID

```
GET /api/system/roles/{id}/menus
```

**响应：** `[1, 2, 3, ...]`

### 3.8 分配数据权限（部门）

```
POST /api/system/roles/{id}/depts
```

**请求体：** `[1, 2, 3]`（部门 ID 列表）

### 3.9 获取角色已分配的部门 ID

```
GET /api/system/roles/{id}/depts
```

**响应：** `[1, 2, 3]`

---

## 四、菜单管理（portal-system）

### 4.1 获取菜单列表

```
GET /api/system/menus/list
```

返回所有菜单平铺列表，按排序字段升序。

### 4.2 获取菜单树（按用户权限过滤）

```
GET /api/system/menus/tree?userId={userId}
```

Feign 内部调用接口，根据用户拥有的角色返回对应的菜单树。

### 4.3 新增菜单

```
POST /api/system/menus
```

```json
{
  "menuName": "用户管理",
  "parentId": 2,
  "path": "/system/user",
  "component": "system/user/Index",
  "icon": "user",
  "menuType": "M",
  "permission": "system:user:list",
  "sort": 1,
  "status": 1
}
```

| 字段 | 说明 |
|------|------|
| menuType | `D` 目录、`M` 菜单、`B` 按钮 |
| parentId | 父菜单 ID，顶级为 0 |
| component | 前端组件路径（仅 M 类型需要） |
| permission | 权限标识（如 `system:user:add`） |

### 4.4 修改菜单

```
PUT /api/system/menus
```

请求体同新增，需包含 `id`。

### 4.5 删除菜单

```
DELETE /api/system/menus/{id}
```

存在子菜单时不允许删除。

---

## 五、部门管理（portal-system）

### 5.1 获取部门列表

```
GET /api/system/depts/list
```

### 5.2 获取部门树

```
GET /api/system/depts/tree
```

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "deptName": "总公司",
      "parentId": 0,
      "sort": 0,
      "status": 1,
      "children": [
        {
          "id": 2,
          "deptName": "技术部",
          "parentId": 1,
          "children": []
        }
      ]
    }
  ]
}
```

### 5.3 新增部门

```
POST /api/system/depts
```

```json
{
  "deptName": "财务部",
  "parentId": 1,
  "sort": 3,
  "status": 1
}
```

### 5.4 修改部门

```
PUT /api/system/depts
```

### 5.5 删除部门

```
DELETE /api/system/depts/{id}
```

存在子部门时不允许删除。

---

## 六、权限查询（portal-system）

### 6.1 获取用户权限列表

```
GET /api/system/permissions?userId={userId}
```

Feign 内部调用接口，返回用户的权限标识列表。

**响应：** `["system:user:list", "system:role:add", ...]`

---

## 七、操作日志（portal-system）

### 7.1 分页查询日志

```
GET /api/system/logs/page
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |
| username | string | 否 | 操作人模糊查询 |
| operation | string | 否 | 操作描述模糊查询 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "userId": 1,
        "username": "admin",
        "operation": "新增角色",
        "method": "add",
        "requestUrl": "/api/system/roles",
        "responseCode": 200,
        "ip": "192.168.1.100",
        "costTime": 23,
        "createTime": "2024-01-01 10:00:00"
      }
    ],
    "total": 50
  }
}
```

---

## 八、应用管理（portal-business）

### 8.1 分页查询应用

```
GET /api/app/page
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 10 |
| appName | string | 否 | 应用名称模糊查询 |
| status | int | 否 | 状态筛选 |

### 8.2 查询所有启用应用

```
GET /api/app/list
```

门户首页展示用，返回所有 `status=1` 的应用。

### 8.3 新增应用

```
POST /api/app
```

```json
{
  "appName": "企业邮箱",
  "appUrl": "https://mail.xxx.com",
  "appIcon": "email",
  "appDesc": "企业内部邮箱",
  "categoryId": 2,
  "sort": 0,
  "status": 1
}
```

### 8.4 修改应用

```
PUT /api/app
```

### 8.5 删除应用

```
DELETE /api/app/{id}
```

---

## 九、分类管理（portal-business）

### 9.1 获取所有分类

```
GET /api/category/list
```

### 9.2 获取启用的分类

```
GET /api/category/enabled
```

门户首页展示用。

### 9.3 新增分类

```
POST /api/category
```

```json
{
  "categoryName": "常用工具",
  "sort": 0,
  "icon": "tool",
  "status": 1
}
```

### 9.4 修改分类

```
PUT /api/category
```

### 9.5 删除分类

```
DELETE /api/category/{id}
```

---

## 十、门户首页（portal-business）

### 10.1 获取首页数据

```
GET /api/portal/index
```

返回分类列表及各分类下的启用应用，供门户首页渲染。
