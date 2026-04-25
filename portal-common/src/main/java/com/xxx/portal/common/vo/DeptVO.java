package com.xxx.portal.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 部门树节点
 */
@Data
public class DeptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptName;
    private Long parentId;
    private Integer sort;
    private Integer status;
    private List<DeptVO> children;
}
