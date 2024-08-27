package org.example;

import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "plugin", mixinStandardHelpOptions = true,description = "Test plugin functionality.", subcommands = {
    PluginTestCmd.SinglePluginTest.class,
    PluginTestCmd.MultiPluginTest.class,
    PluginTestCmd.ConfigBasedPluginTest.class
})
public class PluginTestCmd implements Runnable {

    @ParentCommand
    private App parentCommand;

    @Override
    public void run() {
    }

    @Command(name = "test-single-plugin", mixinStandardHelpOptions = true,description = "Test sinlge plugin.")
    public static class SinglePluginTest implements Runnable {

        @ParentCommand
        private PluginTestCmd parentCommand;

        // 指定 jar 包的路径
        @Option(names = {"-p", "--path"}, description = "Path to jar", required = true)
        private String path;

        // 链配置的 json 文件
        @Option(names = {"-c", "--config"}, description = "Path to chain config", required = false)
        private String chainConf;

        // 插件的具体函数列表
        @Option(names = {"-f", "--function"}, description = "Function list", required = false, split = ",")
        private List<String> functionList;

        // 是否启用详细日志
        @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging", required = false)
        private boolean verbose;

        @Override
        public void run() {
            // 实现测试单个插件的逻辑
        }
    } 


    @Command(name = "test-multi-plugin", mixinStandardHelpOptions = true,description = "Test multiple plugins.")
    public static class MultiPluginTest implements Runnable {

        @ParentCommand
        private PluginTestCmd parentCommand;

        // 指定 jar 包的路径
        @Option(names = {"-p", "--path"}, description = "Path to jar", required = true, split = ",")
        private List<String> pathList;

        // 链配置的 json 文件
        @Option(names = {"-c", "--config"}, description = "Path to chain config", required = false, split = ",")
        private List<String> chainConfList;

        // 是否启用详细日志
        @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging", required = false)
        private boolean verbose;

        @Override
        public void run() {
            // 实现测试多个插件的逻辑
        }
    } 


    @Command(name = "test-by-conf", mixinStandardHelpOptions = true,description = "Test plugins by config.")
    public static class ConfigBasedPluginTest implements Runnable {

        @ParentCommand
        private PluginTestCmd parentCommand;


        // 测试配置的 json 文件
        @Option(names = {"-c", "--config"}, description = "Path to test config", required = true)
        private String config;

        // 是否启用详细日志
        @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging", required = false)
        private boolean verbose;

        @Override
        public void run() {
            // 实现基于配置文件的测试的逻辑
        }
    } 
}
