package com.le.leojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.dfa.WordTree;
import com.le.leojcodesandbox.model.ExecuteCodeRequest;
import com.le.leojcodesandbox.model.ExecuteCodeResponse;
import com.le.leojcodesandbox.model.ExecuteMessage;
import com.le.leojcodesandbox.model.JudgeInfo;
import com.le.leojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaDockerCodeSandBox implements CodeSandBox {

    public static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 10000L;

    public static final String SECURITY_MANAGER_PATH = "D:\\work\\project\\leoj-code-sandbox\\src\\main\\resources\\security";

    public static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

    private static final List<String> blackList = Arrays.asList("Files", "exec");

    private static final WordTree WORD_TREE;

    static {
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    public static void main(String[] args) {
        JavaDockerCodeSandBox javaNativeCodeSandBox = new JavaDockerCodeSandBox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));

        String code = ResourceUtil.readStr("testCode/unsafeCode/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandBox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);

    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
//        System.setSecurityManager(new DenySecurityManager());

        //1. 把用户的代码保存为文件

        List<String> inputList = request.getInputList();
        String code = request.getCode();
        String language = request.getLanguage();

        //黑白名单限制，测试时需要注释掉
//        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if ( foundWord != null ) {
//            System.out.println(foundWord.getFoundWord());
//            return null;
//        }


        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 将用户代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

//        2. 编译代码，得到 class 文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            // 等待程序执行获取错误码
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e);
        }

       // 3. 创建容器，把文件复制到容器内




        return null;
    }

    /**
     * 获取错误响应
     * @param e
     * @return
     */
    //6. 错误处理，提升程序健壮性
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
