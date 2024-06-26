package com.light.codesandbox.security;

import java.io.FileDescriptor;

/**
 * @author null&&
 * @Date 2024/6/26 19:04
 */
public class MySecurityManager extends SecurityManager {
    // 检测程序是否可执行
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("程序执行权限被禁用：" + cmd);
        //super.checkExec(cmd);
    }

    // 检测程序是否可读取文件
    @Override
    public void checkRead(String file) {
        System.out.println(file);
        throw new SecurityException("程序读取权限被禁用：" + file);
        //super.checkRead(file);
    }

    // 检测程序是否可写入文件
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("程序写入权限被禁用：" + file);
        //super.checkWrite(file);
    }

    // 检测程序是否可删除文件
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("程序删除权限被禁用：" + file);
        //super.checkDelete(file);
    }

    // 检测程序是否可连接网络
    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("程序网络连接权限被禁用：" + host + ":" + port);
        //super.checkConnect(host, port);
    }
}
