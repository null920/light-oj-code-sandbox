package com.light.codesandbox;

import com.light.codesandbox.model.ExecuteCodeRequest;
import com.light.codesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱
 *
 * @author null&&
 * @Date 2024/6/21 19:00
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeRequest 请求体
     * @return 执行代码响应
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeRequest);
}
