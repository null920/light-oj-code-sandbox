package com.light.codesandbox.model;

import lombok.Data;

/**
 * 判题信息
 *
 * @author null&&
 * @Date 2024/6/16 16:28
 */
@Data
public class JudgeInfo {
    /**
     * 程序执行信息
     */
    private String message;
    /**
     * 消耗时间（ms）
     */
    private Long time;

    /**
     * 消耗内存（KB）
     */
    private Long memory;

}
