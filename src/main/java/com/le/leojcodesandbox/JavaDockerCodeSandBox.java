package com.le.leojcodesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.le.leojcodesandbox.model.ExecuteCodeRequest;
import com.le.leojcodesandbox.model.ExecuteCodeResponse;
import com.le.leojcodesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Component
public class JavaDockerCodeSandBox extends JavaCodeSandBoxTemplate {

    public static final long TIME_OUT = 5000L;

    public static final Boolean FIRST_INIT = true;

    public static void main(String[] args) {
        JavaDockerCodeSandBox javaNativeCodeSandBox = new JavaDockerCodeSandBox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));

        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);

    }



    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        // 3. 创建容器，把文件复制到容器内
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        // 获取默认的Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        // 拉取镜像
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
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
        }
        System.out.println("下载完成");
        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);

        // 创建容器时，可以指定文件路径（Volumn） 映射，作用是把本地的文件同步到容器中，可以让容器访问,也可叫容器挂载目录。
        HostConfig hostConfig = new HostConfig();
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        // 限制最大内存
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        // 限制cpu核心数量
        hostConfig.withCpuCount(1L);
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                // 禁用网络
                .withNetworkDisabled(true)
                .withAttachStdout(true)
                .withReadonlyRootfs(true)
                .withAttachStderr(true)
                // 支持交互式命令
                .withTty(true)
                .exec();

        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();


        // 启动容器
        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId);
        startContainerCmd.exec();
        // docker exec naughty_merkle java -cp /app Main 1 2
        // 执行命令并获取结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        for (String inputArgs: inputList) {
            StopWatch stopWatch = new StopWatch();
            String [] inputArgsArray = inputArgs.split(" ");
            String [] cmdArray = ArrayUtil.append( new String[] {"java","-cp","/app","Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
            System.out.println("创建执行命令" + execCreateCmdResponse);
            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            final long[] time = {0l};
            String execId = execCreateCmdResponse.getId();
            final boolean[] timeOut = {true};
            // 程序异步执行
            ExecStartResultCallback execStartResultCallback =  new ExecStartResultCallback() {
                // 规定时间内完成才会执行此方法
                @Override
                public void onComplete() {
                    timeOut[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    // 获取程序执行结果
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            // 获取内存占用情况
            final Long[] maxMemory = {0L};
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(maxMemory[0], statistics.getMemoryStats().getUsage());
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
            });
            statsCmd.exec(statisticsResultCallback);

            try {
                // 使用StopWatch获取程序执行时间
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        // 此处使用await等待执行完菜继续执行后续流程
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                statsCmd.close();
                time[0] = stopWatch.getLastTaskTimeMillis();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }


            statsCmd.close();
            executeMessage.setMemory(maxMemory[0]);
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time[0]);
            executeMessageList.add(executeMessage);
        }
        return executeMessageList;
    }


}
