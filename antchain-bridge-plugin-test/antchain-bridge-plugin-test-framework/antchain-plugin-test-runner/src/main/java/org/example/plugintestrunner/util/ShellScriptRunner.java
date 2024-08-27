package org.example.plugintestrunner.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ShellScriptRunner {

    private final String logDirectory;
    private final String scriptDirectory;
//    private final String baseDir = new File(".").getCanonicalPath();

    public ShellScriptRunner(String logDirectory, String scriptDirectory) throws IOException {
        this.logDirectory = logDirectory;
        this.scriptDirectory = scriptDirectory;
    }

    public void runScript(String scriptName) throws IOException, InterruptedException {
        // 构造脚本完整路径
        String scriptPath = Paths.get(scriptDirectory, scriptName).toAbsolutePath().toString();
        // 设置日志文件路径
        Path logFilePath = Paths.get(logDirectory, scriptName.split("\\.")[0] + ".log").toAbsolutePath();

        // 创建ProcessBuilder，指定脚本路径和重定向输出
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", scriptPath)
                .redirectErrorStream(true) // 将错误输出和标准输出合并
                .redirectOutput(logFilePath.toFile()); // 将输出重定向到日志文件

        // 启动进程
        Process process = processBuilder.start();

        // 等待进程结束并获取退出代码
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Script exited with code: " + exitCode);
        }
    }


    public boolean scriptExists(String scriptName) {
        Path path = Paths.get(scriptDirectory, scriptName).toAbsolutePath();
        return path.toFile().exists();
    }
}