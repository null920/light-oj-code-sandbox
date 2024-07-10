# 使用官方 OpenJDK 8 镜像作为基础镜像
FROM openjdk:8-jdk-alpine

# 复制应用的 jar 文件到镜像内部
COPY ./light-code-sandbox-0.0.1-SNAPSHOT.jar /light-code-sandbox-0.0.1-SNAPSHOT.jar
# 声明运行时容器提供服务端口（可选，用于EXPOSE指令告诉Docker该容器对外暴露哪些端口）
EXPOSE 9630
# 指定容器启动时运行 jar 包
ENTRYPOINT ["java","-jar","/light-code-sandbox-0.0.1-SNAPSHOT.jar"]
# 可以使用CMD指定默认启动参数（例如：--spring.profiles.active=prod）
CMD ["--spring.profiles.active=prod"]