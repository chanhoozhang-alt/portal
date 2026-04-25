-- =============================================
-- 门户管理系统 - 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS portal DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE portal;

-- ----------------------------
-- 1. 部门表（从上游同步）
-- ----------------------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
    id BIGINT NOT NULL COMMENT '主键',
    dept_name VARCHAR(128) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    sync_time DATETIME COMMENT '最后同步时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ----------------------------
-- 2. 人员表（从上游同步）
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT NOT NULL COMMENT '主键',
    emp_no VARCHAR(32) NOT NULL COMMENT '工号，上游唯一标识',
    username VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    real_name VARCHAR(64) DEFAULT NULL COMMENT '真实姓名',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    sync_time DATETIME COMMENT '最后同步时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_no (emp_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员表';

-- ----------------------------
-- 3. 菜单表
-- ----------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id BIGINT NOT NULL COMMENT '主键',
    menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    path VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
    component VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
    icon VARCHAR(64) DEFAULT NULL COMMENT '图标',
    menu_type CHAR(1) NOT NULL COMMENT 'D目录 M菜单 B按钮',
    permission VARCHAR(128) DEFAULT NULL COMMENT '权限标识，如 system:user:add',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- ----------------------------
-- 4. 角色表
-- ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT NOT NULL COMMENT '主键',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_key VARCHAR(64) NOT NULL COMMENT '角色标识，如 admin, editor',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 5. 角色-菜单关联
-- ----------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ----------------------------
-- 6. 用户-角色关联
-- ----------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- 7. 角色-部门关联（数据权限）
-- ----------------------------
DROP TABLE IF EXISTS sys_role_dept;
CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (role_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色部门关联表';

-- ----------------------------
-- 8. 应用分类
-- ----------------------------
DROP TABLE IF EXISTS portal_category;
CREATE TABLE portal_category (
    id BIGINT NOT NULL COMMENT '主键',
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    sort INT DEFAULT 0 COMMENT '排序',
    icon VARCHAR(255) DEFAULT NULL COMMENT '图标',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用分类表';

-- ----------------------------
-- 9. 应用信息
-- ----------------------------
DROP TABLE IF EXISTS portal_app;
CREATE TABLE portal_app (
    id BIGINT NOT NULL COMMENT '主键',
    app_name VARCHAR(128) NOT NULL COMMENT '应用名称',
    app_url VARCHAR(512) NOT NULL COMMENT '应用地址',
    app_icon VARCHAR(255) DEFAULT NULL COMMENT '应用图标',
    app_desc VARCHAR(512) DEFAULT NULL COMMENT '应用描述',
    category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用信息表';

-- ----------------------------
-- 10. 操作日志
-- ----------------------------
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id BIGINT NOT NULL COMMENT '主键',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '操作用户名',
    operation VARCHAR(128) DEFAULT NULL COMMENT '操作描述',
    method VARCHAR(255) DEFAULT NULL COMMENT '请求方法',
    request_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_code INT DEFAULT NULL COMMENT '响应状态码',
    ip VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    cost_time BIGINT DEFAULT NULL COMMENT '耗时(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';


-- =============================================
-- 初始数据
-- =============================================

-- 初始部门
INSERT INTO sys_dept (id, dept_name, parent_id, sort) VALUES
(1, '总公司', 0, 0),
(2, '技术部', 1, 1),
(3, '产品部', 1, 2);

-- 初始管理员用户
INSERT INTO sys_user (id, emp_no, username, real_name, email, phone, dept_id, status) VALUES
(1, 'admin', 'admin', '系统管理员', 'admin@xxx.com', '13800000000', 2, 1),
(2, 'zhangsan', 'zhangsan', '张三', 'zhangsan@xxx.com', '13800000001', 2, 1),
(3, 'lisi', 'lisi', '李四', 'lisi@xxx.com', '13800000002', 3, 1);

-- 初始角色
INSERT INTO sys_role (id, role_name, role_key, sort) VALUES
(1, '超级管理员', 'admin', 0),
(2, '普通用户', 'user', 1);

-- 初始菜单
INSERT INTO sys_menu (id, menu_name, parent_id, path, component, icon, menu_type, permission, sort) VALUES
(1, '门户首页', 0, '/portal', 'portal/Index', 'home', 'M', NULL, 0),
(2, '系统管理', 0, '/system', NULL, 'setting', 'D', NULL, 1),
(3, '人员管理', 2, '/system/user', 'system/user/Index', 'user', 'M', 'system:user:list', 1),
(4, '角色管理', 2, '/system/role', 'system/role/Index', 'role', 'M', 'system:role:list', 2),
(5, '菜单管理', 2, '/system/menu', 'system/menu/Index', 'menu', 'M', 'system:menu:list', 3),
(6, '部门管理', 2, '/system/dept', 'system/dept/Index', 'dept', 'M', 'system:dept:list', 4),
(7, '操作日志', 2, '/system/log', 'log/Index', 'log', 'M', 'system:log:list', 5),
(8, '应用管理', 0, '/app', NULL, 'app', 'D', NULL, 2),
(9, '应用列表', 8, '/app/list', 'app/Index', 'list', 'M', 'app:list', 1),
(10, '分类管理', 8, '/app/category', 'app/Category', 'category', 'M', 'app:category:list', 2);

-- 菜单按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, path, component, icon, menu_type, permission, sort) VALUES
(100, '用户新增', 3, NULL, NULL, NULL, 'B', 'system:user:add', 1),
(101, '用户编辑', 3, NULL, NULL, NULL, 'B', 'system:user:edit', 2),
(102, '用户删除', 3, NULL, NULL, NULL, 'B', 'system:user:delete', 3),
(103, '角色新增', 4, NULL, NULL, NULL, 'B', 'system:role:add', 1),
(104, '角色编辑', 4, NULL, NULL, NULL, 'B', 'system:role:edit', 2),
(105, '角色删除', 4, NULL, NULL, NULL, 'B', 'system:role:delete', 3),
(106, '菜单新增', 5, NULL, NULL, NULL, 'B', 'system:menu:add', 1),
(107, '菜单编辑', 5, NULL, NULL, NULL, 'B', 'system:menu:edit', 2),
(108, '菜单删除', 5, NULL, NULL, NULL, 'B', 'system:menu:delete', 3),
(109, '应用新增', 9, NULL, NULL, NULL, 'B', 'app:add', 1),
(110, '应用编辑', 9, NULL, NULL, NULL, 'B', 'app:edit', 2),
(111, '应用删除', 9, NULL, NULL, NULL, 'B', 'app:delete', 3);

-- 管理员角色分配所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 普通用户角色分配门户首页
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1);

-- 用户-角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 2);

-- 初始应用分类
INSERT INTO portal_category (id, category_name, sort, icon) VALUES
(1, '常用工具', 0, 'tool'),
(2, '办公协同', 1, 'office'),
(3, '研发工具', 2, 'dev');

-- 初始应用
INSERT INTO portal_app (id, app_name, app_url, app_icon, app_desc, category_id, sort) VALUES
(1, '企业邮箱', 'https://mail.xxx.com', 'email', '企业内部邮箱', 2, 0),
(2, 'OA系统', 'https://oa.xxx.com', 'oa', '办公自动化', 2, 1),
(3, 'GitLab', 'https://git.xxx.com', 'git', '代码仓库', 3, 0),
(4, 'Jenkins', 'https://ci.xxx.com', 'jenkins', '持续集成', 3, 1);
