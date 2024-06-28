package com.light.codesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * @author null&&
 * @Date 2024/6/27 21:34
 */
public class DockerDemo {
    public static void main(String[] args) throws InterruptedException, IOException {

        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String imageName = "nginx:latest";

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
            pullImageCmd
                    .exec(pullImageResultCallback)
                    .awaitCompletion();
            System.out.println("下载完成");
        }
        // 创建容器
        String containerId = null;
        try (CreateContainerCmd containerCmd = dockerClient.createContainerCmd(imageName)) {
            CreateContainerResponse containerResponse = containerCmd
                    .withCmd("echo", "Hello Docker")
                    .exec();
            containerId = containerResponse.getId();
            System.out.println("容器创建成功，容器ID：" + containerId);
        } catch (DockerClientException e) {
            System.out.println(e.getMessage());
        }

        // 查看已创建容器
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        List<Container> containerList = listContainersCmd.withShowAll(true).exec();
        containerList.forEach(container -> {
            System.out.println("容器ID：" + container.getId());
            System.out.println("容器名称：" + container.getNames()[0]);
            System.out.println("容器状态：" + container.getStatus());
            System.out.println("容器镜像：" + container.getImage());
            System.out.println("容器创建时间：" + Instant.ofEpochSecond(container.getCreated()));
        });

        // 启动容器
        if (containerId != null) {
            dockerClient.startContainerCmd(containerId).exec();

            // 查看日志
            dockerClient.logContainerCmd(containerId)
                    .withStdErr(true)
                    .withStdOut(true)
                    .withFollowStream(true)
                    .withTailAll()
                    .exec(new LogContainerResultCallback() {
                        @Override
                        public void onNext(Frame item) {
                            System.out.println("日志：" + new String(item.getPayload()));
                            super.onNext(item);
                        }
                    })
                    .awaitCompletion();

            // 删除容器
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();

            // 删除镜像
            dockerClient.removeImageCmd(imageName).exec();
        }
        dockerClient.close();
    }

}
