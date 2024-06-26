package com.light.codesandbox.template;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.light.codesandbox.CodeSandbox;
import com.light.codesandbox.model.ExecuteCodeRequest;
import com.light.codesandbox.model.ExecuteCodeResponse;
import com.light.codesandbox.model.ExecuteMessage;
import com.light.codesandbox.model.JudgeInfo;
import com.light.codesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 代码沙箱模版
 *
 * @author null&&
 * @Date 2024/6/28 19:48
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    private static final String GLOBAL_CODE_DIR_NAME = "tempCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";


    public final ExecuteCodeResponse executeCode(ExecuteCodeRequest executeRequest) {
        // System.setSecurityManager(new DefaultSecurityManager());
        List<String> inputList = executeRequest.getInputList();
        String language = executeRequest.getLanguage();
        String code = executeRequest.getCode();
        // 前置操作
        if (!inspectIllegalCharacter(code)) return null;

        // 公共部分
        // 保存用户代码为文件
        File userCodeFile = saveCodeFIle(code);
        // 编译代码
        ExecuteMessage compileResult = compileCode(userCodeFile);
        System.out.println(compileResult);

        // 执行代码
        List<ExecuteMessage> executeMessageList = executeFile(inputList, userCodeFile);

        // 后置操作
        // 获取输出结果
        ExecuteCodeResponse executeCodeResponse = getOutputResponse(executeMessageList);
        // 删除临时文件
        boolean deleteResult = deleteCodeFile(userCodeFile);
        if (deleteResult) {
            log.info("删除临时文件成功");
        } else {
            log.info("删除临时文件失败");
        }
        return executeCodeResponse;
    }

    /**
     * 判断代码中是否包含非法字符
     *
     * @param code 代码
     * @return 是否有非法字符
     */
    protected abstract boolean inspectIllegalCharacter(String code);

    /**
     * 保存用户代码为文件
     *
     * @param code 代码
     * @return 文件
     */
    private File saveCodeFIle(String code) {
        // 判断全局代码目录是否存在，不存在则创建
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 编译代码
     *
     * @param userCodeFile 用户代码文件
     * @return 编译结果
     */
    private ExecuteMessage compileCode(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage compileMessage = ProcessUtils.getProcessMessage(compileProcess, "编译");
            if (compileMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return compileMessage;
        } catch (IOException e) {
            //System.out.println("编译错误" + e.getMessage());
            throw new RuntimeException("编译错误" + e.getMessage());
        }
    }


    /**
     * 执行代码（具体的逻辑由子类实现）
     *
     * @param inputList    输入参数列表
     * @param userCodeFile 用户代码文件
     */
    protected abstract List<ExecuteMessage> executeFile(List<String> inputList, File userCodeFile);


    /**
     * 获取输出结果
     *
     * @param executeMessageList 执行信息列表
     * @return 执行响应
     */
    protected ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        // 整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0L;
        long maxMemory = 0L;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            Long memory = executeMessage.getMemory();
            if (memory != null) {
                maxMemory = Math.max(maxMemory, memory);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 删除代码文件
     *
     * @param userCodeFile 用户代码文件
     * @return 是否删除成功
     */
    protected boolean deleteCodeFile(File userCodeFile) {
        // 删除临时文件
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除临时文件" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
