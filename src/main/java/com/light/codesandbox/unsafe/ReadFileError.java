package com.light.codesandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 读取服务器文件（文件信息泄露）
 *
 * @author null&&
 * @Date 2024/6/24 18:03
 */
public class ReadFileError {
    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");
        // 通过相对路径，直接读取到项目的配置文件
        String filePath = userDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "application.yml";
        List<String> allLines = Files.readAllLines(Paths.get(filePath));
        System.out.println(String.join("\n", allLines));
    }

}
