package com.light.codesandbox.security;

import cn.hutool.core.io.FileUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author null&&
 * @Date 2024/6/26 19:09
 */
public class TestSecurityManager {

    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());

        List<String> readLines = FileUtil.readLines("/Users/Ycri/IdeaProjects/light-code-sandbox/src/main/resources/application.yml", StandardCharsets.UTF_8);
        System.out.println(readLines);

    }
}
