package com.light.codesandbox.model;

import lombok.Data;

/**
 * 进程执行信息
 *
 * @author null&&
 * @Date 2024/6/24 15:21
 */
@Data
public class ExecuteMessage {
    private Integer exitValue;
    private String message;
    private String errorMessage;
    private Long time;
}
