package com.light.codesandbox.template.impl;

import com.light.codesandbox.model.ExecuteMessage;
import com.light.codesandbox.template.JavaCodeSandboxTemplate;

import java.io.File;
import java.util.List;

/**
 * @author null&&
 * @Date 2024/6/29 22:47
 */
public class NativeCodeSandboxImpl extends JavaCodeSandboxTemplate {
    @Override
    protected boolean inspectIllegalCharacter(String code) {
        return false;
    }

    @Override
    protected List<ExecuteMessage> executeFile(List<String> inputList, File userCodeFile) {

        return null;
    }
}
