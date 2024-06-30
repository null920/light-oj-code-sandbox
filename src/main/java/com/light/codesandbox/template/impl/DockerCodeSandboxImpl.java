package com.light.codesandbox.template.impl;

import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.light.codesandbox.model.ExecuteMessage;
import com.light.codesandbox.template.JavaCodeSandboxTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Java 代码沙箱实现（Docker）
 *
 * @author null&&
 * @Date 2024/6/29 22:47
 */
@Component
public class DockerCodeSandboxImpl extends JavaCodeSandboxTemplate {

    private static final long TIME_OUT = 5000L;

    @Override
    protected boolean inspectIllegalCharacter(String code) {
        return true;
    }


    @Override
    protected List<ExecuteMessage> executeFile(List<String> inputList, File userCodeFile) {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String imageName = pullImage(dockerClient);
        String containerId = createContainer(dockerClient, imageName, userCodeFile);
        startContainer(dockerClient, containerId);
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            ExecCreateCmdResponse execCreateCmdResponse = createExecCmd(inputArgs, dockerClient, containerId);
            ExecuteMessage executeMessage = startExecCmd(dockerClient, containerId, execCreateCmdResponse);
            executeMessageList.add(executeMessage);
        }
        // 删除容器
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
        try {
            dockerClient.close();
        } catch (IOException e) {
            System.out.println("关闭资源异常");
            throw new RuntimeException(e);
        }
        return executeMessageList;
    }

    /**
     * 拉取镜像
     *
     * @param dockerClient docker客户端
     * @return 是否拉取成功
     */
    private String pullImage(DockerClient dockerClient) {
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
        return imageName;
    }

    /**
     * 创建容器
     *
     * @param dockerClient docker客户端
     * @param imageName    镜像名称
     * @param userCodeFile 代码文件
     * @return 容器id
     */
    private String createContainer(DockerClient dockerClient, String imageName, File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 创建容器，把用户代码复制到容器
        String containerId = null;
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1024 * 1024L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        try (CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageName)) {
            CreateContainerResponse containerResponse = containerCmd
                    .withHostConfig(hostConfig)
                    .withNetworkDisabled(true)
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
        return containerId;
    }

    /**
     * 启动容器
     *
     * @param dockerClient docker客户端
     * @param containerId  容器id
     */
    private void startContainer(DockerClient dockerClient, String containerId) {
        // 启动容器
        if (containerId != null) {
            dockerClient.startContainerCmd(containerId).exec();
            System.out.println("容器启动成功，容器ID：" + containerId);
        }
    }

    /**
     * 创建执行命令
     *
     * @param inputArgs    输入参数
     * @param dockerClient docker客户端
     * @param containerId  容器id
     * @return 执行命令响应
     */
    private ExecCreateCmdResponse createExecCmd(String inputArgs, DockerClient dockerClient, String containerId) {
        // docker exec [containerId] java -cp /app Main 1 3
        // 创建执行命令
        String[] inputArgsArray = inputArgs.split(" ");
        String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd(cmdArray)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .exec();
        System.out.println("创建执行命令：" + execCreateCmdResponse);
        return execCreateCmdResponse;
    }

    /**
     * 启动执行命令
     *
     * @param dockerClient          docker客户端
     * @param containerId           容器id
     * @param execCreateCmdResponse 创建命令响应
     */
    private ExecuteMessage startExecCmd(DockerClient dockerClient, String containerId, ExecCreateCmdResponse execCreateCmdResponse) {
        StopWatch stopWatch = new StopWatch();
        ExecuteMessage executeMessage = new ExecuteMessage();
        final String[] message = {null};
        final String[] errorMessage = {null};
        long time = 0L;
        final boolean[] timeout = {true};
        String execId = execCreateCmdResponse.getId();
        ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
            // 如果程序执行完成则表示没超时
            @Override
            public void onComplete() {
                timeout[0] = false;
                super.onComplete();
            }

            @Override
            public void onNext(Frame frame) {
                StreamType streamType = frame.getStreamType();
                if (StreamType.STDERR.equals(streamType)) {
                    errorMessage[0] = new String(frame.getPayload());
                    System.out.println("执行命令错误：" + errorMessage[0]);
                } else {
                    message[0] = new String(frame.getPayload());
                    System.out.println("执行命令输出：" + message[0]);
                }
                super.onNext(frame);
            }
        };

        final long[] maxMemory = {0L};
        // 获取内存占用
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        ResultCallback<Statistics> statisticsResultCallback = new ResultCallback<Statistics>() {
            @Override
            public void onNext(Statistics statistics) {
                System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
            }

            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {

            }
        };
        statsCmd.exec(statisticsResultCallback);

        // 执行命令
        if (execId != null) {
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId).exec(execStartResultCallback).awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                if (timeout[0]) {
                    System.out.println("执行超时");
                }
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("执行命令异常" + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        executeMessage.setMessage(message[0]);
        executeMessage.setErrorMessage(errorMessage[0]);
        executeMessage.setTime(time);
        executeMessage.setMemory(maxMemory[0]);
        try {
            execStartResultCallback.close();
            statisticsResultCallback.close();
        } catch (IOException e) {
            throw new RuntimeException("关闭资源失败" + e);
        }
        return executeMessage;
    }

}