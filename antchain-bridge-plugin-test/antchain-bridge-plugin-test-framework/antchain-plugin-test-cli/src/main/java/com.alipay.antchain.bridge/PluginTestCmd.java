package com.alipay.antchain.bridge;

import java.util.List;

import com.alipay.antchain.bridge.plugintestrunner.exception.PluginTestException;
import com.alipay.antchain.bridge.plugintestrunner.service.PluginTestService;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import com.alipay.antchain.bridge.plugintestrunner.util.LogLevel;
import picocli.CommandLine.ParentCommand;

@Command(name = "plugin", mixinStandardHelpOptions = true,description = "Test plugin functionality.", subcommands = {
    PluginTestCmd.SinglePluginTest.class,
//    PluginTestCmd.MultiPluginTest.class,
//    PluginTestCmd.ConfigBasedPluginTest.class
})
public class PluginTestCmd implements Runnable {

    @ParentCommand
    private App parentCommand;

    @Override
    public void run() {
    }

    @Command(name = "test", mixinStandardHelpOptions = true,description = "Test sinlge plugin.")
    public static class SinglePluginTest implements Runnable {

        @ParentCommand
        private PluginTestCmd parentCommand;

        // 指定 jar 包的路径
        @Option(names = {"-j", "--jar"}, description = "Path to jar", required = true)
        private String jarPath;

        // 指定 jar 包的路径
        @Option(names = {"-p", "--product"}, description = "Plugin Product", required = true)
        private String product;

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
            PluginTestService pluginTestService = parentCommand.parentCommand.pluginTestRunner.getPluginTestService();
            PTRLogger logger = pluginTestService.getLogger();
            if (chainConf != null) {
               logger.rlog(LogLevel.ERROR, "Test with chain config is not supported yet.");
            } else {
                try {
                    pluginTestService.runCmd(jarPath, product, functionList);
                } catch (PluginTestException e) {
                    logger.rlog(LogLevel.ERROR, e.getMessage());
                    if (e.getCause() != null) {
                        logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                    }
                }
            }
        }
    } 


//    @Command(name = "test-multi-plugin", mixinStandardHelpOptions = true,description = "Test multiple plugins.")
//    public static class MultiPluginTest implements Runnable {
//
//        @ParentCommand
//        private PluginTestCmd parentCommand;
//
//        // 指定 jar 包的路径
//        @Option(names = {"-p", "--path"}, description = "Path to jar", required = true, split = ",")
//        private List<String> pathList;
//
//        // 链配置的 json 文件
//        @Option(names = {"-c", "--config"}, description = "Path to chain config", required = false, split = ",")
//        private List<String> chainConfList;
//
//        // 是否启用详细日志
//        @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging", required = false)
//        private boolean verbose;
//
//        @Override
//        public void run() {
//            // 实现测试多个插件的逻辑
//        }
//    }


//    @Command(name = "test-by-conf", mixinStandardHelpOptions = true,description = "Test plugins by config.")
//    public static class ConfigBasedPluginTest implements Runnable {
//
//        @ParentCommand
//        private PluginTestCmd parentCommand;
//
//
//        // 测试配置的 json 文件
//        @Option(names = {"-c", "--config"}, description = "Path to test config", required = true)
//        private String config;
//
//        // 是否启用详细日志
//        @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging", required = false)
//        private boolean verbose;
//
//        @Override
//        public void run() {
//            // 实现基于配置文件的测试的逻辑
//        }
//    }
}
