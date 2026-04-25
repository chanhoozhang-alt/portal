package com.xxx.portal.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxx.portal.common.exception.BusinessException;
import com.xxx.portal.common.model.SysDept;
import com.xxx.portal.common.vo.DeptVO;
import com.xxx.portal.system.mapper.SysDeptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门管理服务
 */
@Service
public class DeptService {

    @Autowired
    private SysDeptMapper deptMapper;

    public List<SysDept> list() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSort));
    }

    /**
     * 获取部门树
     */
    public List<DeptVO> tree() {
        List<SysDept> depts = list();
        List<DeptVO> voList = depts.stream().map(d -> {
            DeptVO vo = new DeptVO();
            vo.setId(d.getId());
            vo.setDeptName(d.getDeptName());
            vo.setParentId(d.getParentId());
            vo.setSort(d.getSort());
            vo.setStatus(d.getStatus());
            return vo;
        }).collect(Collectors.toList());

        Map<Long, List<DeptVO>> grouped = voList.stream()
                .collect(Collectors.groupingBy(DeptVO::getParentId));
        voList.forEach(vo -> vo.setChildren(grouped.get(vo.getId())));

        return voList.stream()
                .filter(vo -> vo.getParentId() == null || vo.getParentId() == 0)
                .collect(Collectors.toList());
    }

    public SysDept getById(Long id) {
        return deptMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(SysDept dept) {
        deptMapper.insert(dept);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysDept dept) {
        deptMapper.updateById(dept);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查是否有子部门
        Long count = deptMapper.selectCount(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id));
        if (count > 0) {
            throw new BusinessException("存在子部门，不能删除");
        }
        deptMapper.deleteById(id);
    }
}
