package com.xxx.portal.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxx.portal.common.exception.BusinessException;
import com.xxx.portal.common.model.SysMenu;
import com.xxx.portal.common.model.SysRoleMenu;
import com.xxx.portal.common.model.SysRole;
import com.xxx.portal.common.model.SysUserRole;
import com.xxx.portal.common.vo.MenuVO;
import com.xxx.portal.system.mapper.SysMenuMapper;
import com.xxx.portal.system.mapper.SysRoleMenuMapper;
import com.xxx.portal.system.mapper.SysRoleMapper;
import com.xxx.portal.system.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单管理服务
 */
@Service
public class MenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    /**
     * 获取所有菜单列表
     */
    public List<SysMenu> list() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort));
    }

    /**
     * 获取菜单树
     */
    public List<MenuVO> tree() {
        List<SysMenu> menus = list();
        return buildTree(menus);
    }

    /**
     * 根据用户 ID 获取菜单树（按权限过滤）
     */
    public List<MenuVO> treeByUserId(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);

        // 补充所有父级菜单，确保树结构完整
        java.util.Set<Long> allIds = new java.util.HashSet<>(menuIds);
        for (SysMenu menu : menus) {
            addParentIds(menu.getParentId(), allIds);
        }
        if (allIds.size() > menuIds.size()) {
            allIds.removeAll(menuIds);
            menus.addAll(menuMapper.selectBatchIds(allIds));
        }

        return buildTree(menus);
    }

    /**
     * 递归补充父菜单 ID
     */
    private void addParentIds(Long parentId, java.util.Set<Long> allIds) {
        if (parentId == null || parentId == 0 || allIds.contains(parentId)) {
            return;
        }
        allIds.add(parentId);
        SysMenu parent = menuMapper.selectById(parentId);
        if (parent != null) {
            addParentIds(parent.getParentId(), allIds);
        }
    }

    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(SysMenu menu) {
        checkParentExists(menu.getParentId());
        menuMapper.insert(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysMenu menu) {
        checkParentExists(menu.getParentId());
        if (menu.getId() != null && menu.getId().equals(menu.getParentId())) {
            throw new BusinessException("父菜单不能是自己");
        }
        menuMapper.updateById(menu);
    }

    /**
     * 校验父菜单存在性
     */
    private void checkParentExists(Long parentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (menuMapper.selectById(parentId) == null) {
            throw new BusinessException("父菜单不存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long count = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (count > 0) {
            throw new BusinessException("存在子菜单，不能删除");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
    }

    /**
     * 构建菜单树
     */
    public List<MenuVO> buildTree(List<SysMenu> menus) {
        List<MenuVO> voList = menus.stream().map(m -> {
            MenuVO vo = new MenuVO();
            vo.setId(m.getId());
            vo.setMenuName(m.getMenuName());
            vo.setParentId(m.getParentId());
            vo.setPath(m.getPath());
            vo.setComponent(m.getComponent());
            vo.setIcon(m.getIcon());
            vo.setMenuType(m.getMenuType());
            vo.setPermission(m.getPermission());
            vo.setSort(m.getSort());
            return vo;
        }).collect(Collectors.toList());

        Map<Long, List<MenuVO>> grouped = voList.stream()
                .collect(Collectors.groupingBy(MenuVO::getParentId));

        voList.forEach(vo -> vo.setChildren(grouped.get(vo.getId())));

        return voList.stream()
                .filter(vo -> vo.getParentId() == null || vo.getParentId() == 0)
                .collect(Collectors.toList());
    }

    /**
     * 构建指定菜单 ID 的菜单树
     */
    public List<MenuVO> buildTreeByIds(List<SysMenu> menus) {
        return buildTree(menus);
    }
}
