package com.light.codesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.light.codesandbox.model.ExecuteCodeRequest;
import com.light.codesandbox.model.ExecuteCodeResponse;
import com.light.codesandbox.template.JavaCodeSandboxTemplate;
import com.light.codesandbox.template.impl.DockerCodeSandboxImpl;
import com.light.codesandbox.template.impl.NativeCodeSandboxImpl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author null&&
 * @Date 2024/6/30 15:46
 */
public class Main {
    public static void main(String[] args) {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/unsafeCode/SleepError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/unsafeCode/ReadFileError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        executeCodeRequest.setLanguage("java");
        executeCodeRequest.setCode(code);
        JavaCodeSandboxTemplate dockerCodeSandbox = new NativeCodeSandboxImpl();
        ExecuteCodeResponse executeCodeResponse = dockerCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);

    }
}
