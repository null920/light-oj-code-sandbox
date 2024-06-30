package com.light.codesandbox.template.impl;

import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.light.codesandbox.model.ExecuteMessage;
import com.light.codesandbox.template.JavaCodeSandboxTemplate;
import com.light.codesandbox.utils.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Java 代码沙箱原生实现
 *
 * @author null&&
 * @Date 2024/6/29 22:47
 */
@Component
public class NativeCodeSandboxImpl extends JavaCodeSandboxTemplate {
    private static final long TIME_OUT = 5000L;
    private static final List<String> blackList = Arrays.asList("Runtime", "File", "Files", "exec", "Process", "ClassLoader", "System.exit");
    private static final WordTree WORD_TREE;

    static {
        // 初始化字典树
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    /**
     * 检查代码是否包含黑名单关键字
     *
     * @param code 代码
     * @return 是否合法
     */
    @Override
    protected boolean inspectIllegalCharacter(String code) {
        // 检查代码是否包含黑名单关键字
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null) {
            System.out.println("代码中有非法字符：" + foundWord.getFoundWord());
            return false;
        }
        return true;
    }


    /**
     * 执行文件
     *
     * @param inputList    输入参数列表
     * @param userCodeFile 用户代码文件
     * @return 执行结果
     */
    @Override
    protected List<ExecuteMessage> executeFile(List<String> inputList, File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            // 限制最大堆空间大小
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 防止进程一直卡住，占用资源
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        if (runProcess.isAlive()) {
                            runProcess.destroy();
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.getProcessMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }
}