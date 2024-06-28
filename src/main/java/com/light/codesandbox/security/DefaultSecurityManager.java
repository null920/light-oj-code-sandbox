package com.light.codesandbox.security;

import java.security.Permission;

/**
 * 默认安全管理器
 *
 * @author null&&
 * @Date 2024/6/26 18:43
 */
public class DefaultSecurityManager extends SecurityManager {

    // 检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
//        System.out.println("默认不做任何限制");
//        System.out.println(perm);
        //super.checkPermission(perm);
    }
}
