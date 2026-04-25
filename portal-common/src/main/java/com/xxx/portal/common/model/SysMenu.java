package com.xxx.portal.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单表
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String menuName;

    private Long parentId;

    /** 路由路径 */
    private String path;

    /** 前端组件路径 */
    private String component;

    private String icon;

    /** D目录 M菜单 B按钮 */
    private String menuType;

    /** 权限标识，如 system:user:add */
    private String permission;

    private Integer sort;

    /** 1启用 0禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
