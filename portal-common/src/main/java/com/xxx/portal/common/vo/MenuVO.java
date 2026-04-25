package com.xxx.portal.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单树节点
 */
@Data
public class MenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String menuName;
    private Long parentId;
    private String path;
    private String component;
    private String icon;
    /** D目录 M菜单 B按钮 */
    private String menuType;
    private String permission;
    private Integer sort;
    private List<MenuVO> children;
}
