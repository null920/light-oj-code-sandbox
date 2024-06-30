package com.light.codesandbox.controller;

import com.light.codesandbox.model.ExecuteCodeRequest;
import com.light.codesandbox.model.ExecuteCodeResponse;
import com.light.codesandbox.template.impl.DockerCodeSandboxImpl;
import com.light.codesandbox.template.impl.NativeCodeSandboxImpl;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author null&&
 * @Date 2024/6/23 19:11
 */
@RestController()
@RequestMapping("/")
public class MainController {

    @Resource
    private DockerCodeSandboxImpl dockerCodeSandbox;

    @Resource
    private NativeCodeSandboxImpl nativeCodeSandbox;

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }


    /**
     * 执行代码
     *
     * @param executeRequest 请求体
     * @return 执行代码响应
     */
    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeRequest) {
        if (executeRequest == null) {
            throw new RuntimeException("参数为空");
        }
        return dockerCodeSandbox.executeCode(executeRequest);
    }
}
