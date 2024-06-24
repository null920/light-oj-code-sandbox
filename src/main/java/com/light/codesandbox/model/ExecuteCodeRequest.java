package com.light.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代码执行请求
 *
 * @author null&&
 * @Date 2024/6/21 19:05
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    /**
     * 题目输入用例
     */
    private List<String> inputList;

    /**
     * 编程语言
     */
    private String language;

    /**
     * 代码
     */
    private String code;
}
