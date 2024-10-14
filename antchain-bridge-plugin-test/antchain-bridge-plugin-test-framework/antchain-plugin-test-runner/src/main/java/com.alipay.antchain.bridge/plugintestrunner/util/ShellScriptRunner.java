package com.alipay.antchain.bridge.plugintestrunner.util;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.Comparator;


public class ShellScriptRunner {

    private final String logDirectory;
    private final String scriptDirectory;
    private boolean isShutdownHookRegistered = false;
    private String tmpScriptDirectory;

    public ShellScriptRunner(String logDirectory, String scriptDirectory) throws IOException {
        this.logDirectory = logDirectory;
        this.scriptDirectory = scriptDirectory;

        Path logDirectoryPath = Paths.get(logDirectory);
        if (!Files.exists(logDirectoryPath)) {
            Files.createDirectories(logDirectoryPath);
        }

        // 复制资源文件到临时目录
        try {
            copyResourcesToTempDir(scriptDirectory);
            copyResourcesToTempDir("config.properties");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void runScript(String childDir, String scriptName) throws IOException, InterruptedException {
        // 构造脚本完整路径
        String scriptPath = Paths.get(tmpScriptDirectory, scriptDirectory, childDir, scriptName).toAbsolutePath().toString();
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

    public boolean scriptExists(String childDir, String scriptName) {
        // 构造资源路径
        String resourcePath = Paths.get(scriptDirectory, childDir, scriptName).toString();

        // 使用ClassLoader来检查资源是否存在
        URL resource = getClass().getClassLoader().getResource(resourcePath);

        // 如果资源存在，resource 不为null
        return resource != null;
    }

    public void copyResourcesToTempDir(String resourceName) throws IOException, URISyntaxException {
        Path tempDir;

        // 检查 tmpScriptDirectory 是否已存在且目录存在
        if (tmpScriptDirectory != null) {
            tempDir = Paths.get(tmpScriptDirectory);
            if (!Files.exists(tempDir)) {
                // tmpScriptDirectory 已设置但目录不存在，重新创建
                tempDir = createTempDirectory();
                this.tmpScriptDirectory = tempDir.toAbsolutePath().toString();
            }
        } else {
            // tmpScriptDirectory 未设置，创建新的临时目录
            tempDir = createTempDirectory();
            this.tmpScriptDirectory = tempDir.toAbsolutePath().toString();
        }

        // 获取资源目录 URL
        ClassLoader classLoader = ShellScriptRunner.class.getClassLoader();
        URL resourceUrl = classLoader.getResource(resourceName);

        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }

        URI uri = resourceUrl.toURI();

        if ("jar".equals(uri.getScheme())) {
            // 处理 JAR 文件中的资源
            try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                // 确保路径以 / 开头，避免相对路径问题
                Path resourcePath = fileSystem.getPath("/" + resourceName);
                if (Files.isDirectory(resourcePath)) {
                    copyDirectoryRecursively(resourcePath, tempDir.resolve(resourceName));
                } else {
                    copyFile(resourcePath, tempDir.resolve(resourceName));
                }
            }
        } else {
            // 处理普通文件系统中的资源
            Path resourcePath = Paths.get(uri);
            if (Files.isDirectory(resourcePath)) {
                copyDirectoryRecursively(resourcePath, tempDir.resolve(resourceName));
            } else {
                copyFile(resourcePath, tempDir.resolve(resourceName));
            }
        }

        // 注册 Shutdown Hook 以删除临时目录（仅在新创建临时目录时注册）
        if (!isShutdownHookRegistered) {
            Path finalTempDir = tempDir;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    deleteDirectoryRecursively(finalTempDir);
//                    System.out.println("Temporary directory deleted successfully.");
                } catch (IOException e) {
                    System.err.println("Failed to delete temporary directory: " + e.getMessage());
                }
            }));

            isShutdownHookRegistered = true;
        }
    }

    /**
     * 创建一个新的临时目录，并返回其路径
     *
     * @return 新创建的临时目录路径
     * @throws IOException 如果创建临时目录时发生IO错误
     */
    private Path createTempDirectory() throws IOException {
        // 指定临时目录的父路径为 /tmp，并使用有效的前缀 "tmpScripts"
        Path tmpBase = Paths.get("/tmp");

        // 确保 /tmp 目录存在
        if (!Files.exists(tmpBase)) {
            throw new IOException("/tmp does not exist.");
        }

        // 创建临时目录，指定父路径为 /tmp
        Path tempDir = Files.createTempDirectory(tmpBase, "tmpScripts");
//        System.out.println("Created new temporary directory: " + tempDir.toString());
        return tempDir;
    }

    /**
     * 递归地将目录及其内容复制到目标路径
     *
     * @param sourceDir 源目录路径
     * @param targetDir 目标目录路径
     * @throws IOException 如果复制过程中发生IO错误
     */
    private void copyDirectoryRecursively(Path sourceDir, Path targetDir) throws IOException {
        if (sourceDir.getFileSystem().provider().getScheme().equals("jar")) {
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.forEach(sourcePath -> {
                    try {
                        // 计算相对路径
                        Path relativePath = sourceDir.relativize(sourcePath);

                        // 使用相对路径的字符串表示在默认文件系统中创建新的 Path
                        Path targetPath = targetDir.resolve(relativePath.toString());

                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(targetPath);
                        } else {
                            // 复制文件时，确保父目录存在
                            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                                Files.createDirectories(targetPath.getParent());
                            }
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } else {
            Files.walk(sourceDir).forEach(sourcePath -> {
                Path targetPath = targetDir.resolve(sourceDir.relativize(sourcePath).toString());
                try {
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        // 复制文件时，确保父目录存在
                        if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                            Files.createDirectories(targetPath.getParent());
                        }
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    /**
     * 将单个文件复制到目标路径
     *
     * @param sourceFile 源文件路径
     * @param targetFile 目标文件路径
     * @throws IOException 如果复制过程中发生IO错误
     */
    private void copyFile(Path sourceFile, Path targetFile) throws IOException {
        if (sourceFile.getFileSystem().provider().getScheme().equals("jar")) {
            // 在 JAR 文件系统中，直接复制文件
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // 在普通文件系统中，直接复制文件
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * 递归地删除指定的目录及其所有内容
     *
     * @param path 要删除的目录路径
     * @throws IOException 如果删除过程中发生IO错误
     */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            // 使用 Files.walk 递归遍历目录
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder()) // 先删除子文件/目录，再删除父目录
                        .forEach(p -> {
                            try {
                                Files.delete(p);
//                                System.out.println("Deleted: " + p);
                            } catch (IOException e) {
                                System.err.println("Failed to delete " + p + ": " + e.getMessage());
                            }
                        });
            }
        }
    }
}