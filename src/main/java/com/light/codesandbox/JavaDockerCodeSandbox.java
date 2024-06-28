package com.light.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.light.codesandbox.model.ExecuteCodeRequest;
import com.light.codesandbox.model.ExecuteCodeResponse;
import com.light.codesandbox.model.ExecuteMessage;
import com.light.codesandbox.model.JudgeInfo;
import com.light.codesandbox.security.DefaultSecurityManager;
import com.light.codesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author null&&
 * @Date 2024/6/23 19:46
 */
public class JavaDockerCodeSandbox implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tempCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 5000L;


    public static void main(String[] args) {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/unsafeCode/SleepError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/unsafeCode/ReadFileError.java", StandardCharsets.UTF_8);
        //String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        executeCodeRequest.setLanguage("java");
        executeCodeRequest.setCode(code);
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeRequest) {
        System.setSecurityManager(new DefaultSecurityManager());

        List<String> inputList = executeRequest.getInputList();
        String language = executeRequest.getLanguage();
        String code = executeRequest.getCode();


        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，不存在则创建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 编译代码
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage compileMessage = ProcessUtils.getProcessMessage(compileProcess, "编译");
            System.out.println(compileMessage);
        } catch (IOException e) {
            return getErrorResponse(e);
        }

        // 创建容器，把用户代码复制到容器
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String imageName = "openjdk:8-alpine";

        List<Image> imageList = dockerClient.listImagesCmd().exec();
        boolean imageExist = imageList.stream()
                .flatMap(image -> Arrays.stream(image.getRepoTags()))
                .anyMatch(tag -> tag.equals(imageName));
        // 拉取镜像，如果镜像已经存在，跳过下载
        if (imageExist) {
            System.out.println("镜像存在: " + imageName);
        } else {
            System.out.println("镜像不存在: " + imageName);
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(imageName);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("下载完成");
        }
        // 创建容器
        String containerId = null;
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1024 * 1024L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        try (CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageName)) {
            CreateContainerResponse containerResponse = containerCmd
                    .withHostConfig(hostConfig)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withTty(true)
                    .exec();
            containerId = containerResponse.getId();
            System.out.println("容器创建成功，容器ID：" + containerId);
        } catch (DockerClientException e) {
            System.out.println("容器创建失败" + e.getMessage());
        }
        // 启动容器
        if (containerId != null) {
            dockerClient.startContainerCmd(containerId).exec();
            System.out.println("容器启动成功，容器ID：" + containerId);
        }

        // 执行容器
        // docker exec [containerId] java -cp /app Main 1 3
        ExecStartResultCallback execStartResultCallback = null;
        for (String inputArgs : inputList) {
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);
            String id = execCreateCmdResponse.getId();
            execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        System.out.println("执行命令错误：" + new String(frame.getPayload()));
                    } else {
                        System.out.println("执行命令输出：" + new String(frame.getPayload()));
                    }
                    super.onNext(frame);
                }
            };
            if (id != null) {
                try {
                    dockerClient.execStartCmd(id).exec(execStartResultCallback).awaitCompletion();
                } catch (InterruptedException e) {
                    System.out.println("执行命令异常" + e.getMessage());
                    throw new RuntimeException(e);
                }
            }

        }
        try {
            if (execStartResultCallback != null) {
                execStartResultCallback.close();
            }
            dockerClient.close();
        } catch (IOException e) {
            System.out.println("关闭回调异常");
            throw new RuntimeException(e);
        }
        return null;
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
