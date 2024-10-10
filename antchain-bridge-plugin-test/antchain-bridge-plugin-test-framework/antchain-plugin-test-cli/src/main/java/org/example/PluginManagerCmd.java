package org.example;

import java.util.List;

import org.example.plugintestrunner.exception.PluginManagerException;
import org.example.plugintestrunner.service.PluginManagerService;
import org.example.plugintestrunner.util.LogLevel;
import org.example.plugintestrunner.util.PTRLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;


@Command(name = "plugin-manager", mixinStandardHelpOptions = true, description = "Test plugin loading and starting.", subcommands = {
        PluginManagerCmd.PluginLoad.class,
        PluginManagerCmd.PluginStart.class,
        PluginManagerCmd.PluginStartFromStop.class,
        PluginManagerCmd.PluginStop.class,
        PluginManagerCmd.PluginReload.class,
        PluginManagerCmd.PluginCreateBBC.class
})
public class PluginManagerCmd implements Runnable {

    @ParentCommand
    private App parentCommand;

    @Override
    public void run() {
    }

    // 加载插件
    @Command(name = "load", mixinStandardHelpOptions = true, description = "Load a plugin")
    public static class PluginLoad implements Runnable {

        @ParentCommand
        private PluginManagerCmd parentCommand;

        @Option(names = {"-j", "--jar"}, description = "Path to jar", required = true, split = ",")
        private List<String> jarPathList;

        @Override
        public void run() {
            PluginManagerService pluginManagerService = parentCommand.parentCommand.pluginTestRunner.getPluginManagerService();
            PTRLogger logger = pluginManagerService.getLogger();
            for (String jarPath : jarPathList) {
                try {
                    pluginManagerService.testLoadPlugin(jarPath);
                    logger.rlog(LogLevel.INFO, "Successfully loaded plugin: " + jarPath);
                } catch (PluginManagerException e) {
                    logger.rlog(LogLevel.ERROR, e.getMessage());
                    if (e.getCause() != null) {
                        logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                    }
                }
            }
        }
    }


    // 启动插件
    @Command(name = "start", mixinStandardHelpOptions = true, description = "Start a plugin")
    public static class PluginStart implements Runnable {

        @ParentCommand
        private PluginManagerCmd parentCommand;

        @Option(names = {"-j", "--jar"}, description = "Path to jar", required = true, split = ",")
        private List<String> jarPathList;


        @Override
        public void run() {
            PluginManagerService pluginManagerService = parentCommand.parentCommand.pluginTestRunner.getPluginManagerService();
            PTRLogger logger = pluginManagerService.getLogger();
            for (String jarPath : jarPathList) {
                try {
                    pluginManagerService.testStartPlugin(jarPath);
                    logger.rlog(LogLevel.INFO, "Successfully started plugin: " + jarPath);
                } catch (PluginManagerException e) {
                    logger.rlog(LogLevel.ERROR, e.getMessage());
                    if (e.getCause() != null) {
                        logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                    }
                }
            }
        }
    }
    
    // 关闭插件
    @Command(name = "stop", mixinStandardHelpOptions = true, description = "Stop a plugin")
    public static class PluginStop implements Runnable {

        @ParentCommand
        private PluginManagerCmd parentCommand;

        @Option(names = {"-p", "--product"}, description = "Plugin product", required = true, split = ",")
        private List<String> pluginProductList;

        @Override
        public void run() {
            PluginManagerService pluginManagerService = parentCommand.parentCommand.pluginTestRunner.getPluginManagerService();
            PTRLogger logger = pluginManagerService.getLogger();
            for (String pluginProduct : pluginProductList) {
                try {
                    pluginManagerService.testStopPlugin(pluginProduct);
                    logger.rlog(LogLevel.INFO, "Successfully stopped plugin: " + pluginProduct);
                } catch (PluginManagerException e) {
                    logger.rlog(LogLevel.ERROR, e.getMessage());
                    if (e.getCause() != null) {
                        logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                    }
                }
            }
        }
    }

    // 启动已经关闭的插件
    @Command(name = "start-from-stop", mixinStandardHelpOptions = true, description = "Start a stopped plugin")
    public static class PluginStartFromStop implements Runnable {

        @ParentCommand
        private PluginManagerCmd parentCommand;

        @Option(names = {"-p", "--product"}, description = "Plugin product", required = true, split = ",")
        private List<String> pluginProductList;

        @Override
        public void run() {
            PluginManagerService pluginManagerService = parentCommand.parentCommand.pluginTestRunner.getPluginManagerService();
            PTRLogger logger = pluginManagerService.getLogger();
            for (String pluginProduct : pluginProductList) {
                try {
                    pluginManagerService.testStartPluginFromStop(pluginProduct);
                    logger.rlog(LogLevel.INFO, "Successfully started plugin: " + pluginProduct + " from stop");
                } catch (PluginManagerException e) {
                    logger.rlog(LogLevel.ERROR, e.getMessage());
                    if (e.getCause() != null) {
                        logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                    }
                }
            }
        }
    }

    // 重新加载插件
    @Command(name = "reload", mixinStandardHelpOptions = true, description = "Reload a plugin")
    public static class PluginReload implements Runnable {

        @ParentCommand
        private PluginManagerCmd parentCommand;

        @Option(names = {"-p", "--product"}, description = "Plugin product", required = true, split = ",")
        private List<String> pluginProductList;

        @Option(names = {"-j", "--jar"}, description = "Path to jar", required = true, split = ",")
        private List<String> jarPathList;
        
        @Override
        public void run() {
            PluginManagerService pluginManagerService = parentCommand.parentCommand.pluginTestRunner.getPluginManagerService();
            PTRLogger logger = pluginManagerService.getLogger();
            if (!areListsEqualSize(pluginProductList, jarPathList)) {
                logger.rlog(LogLevel.ERROR, "The number of product, name and path should be the same.");
            } else {
                for (int i = 0; i < pluginProductList.size(); i++) {
                    String pluginProduct = pluginProductList.get(i);
                    String jarPath = jarPathList.get(i);
                    try {
                        pluginManagerService.testReloadPlugin(jarPath, pluginProduct);
                        logger.rlog(LogLevel.INFO, "Successfully reloaded plugin: " + jarPath);
                    } catch (PluginManagerException e) {
                        logger.rlog(LogLevel.ERROR, e.getMessage());
                        if (e.getCause() != null) {
                            logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                        }
                    }
                }
            }
        }
    }

    // 创建 BBC 服务
    @Command(name = "create-bbc", mixinStandardHelpOptions = true, description = "Create BBC service")
    public static class PluginCreateBBC implements Runnable {

        @ParentCommand
        private PluginManagerCmd parentCommand;

        @Option(names = {"-p", "--product"}, description = "Plugin product", required = true, split = ",")
        private List<String> pluginProductList;

        @Option(names = {"-d", "--domain"}, description = "Domain name", required = true, split = ",")
        private List<String> domainNameList;


        @Override
        public void run() {
            // 实现显示已加载插件的逻辑
            PluginManagerService pluginManagerService = parentCommand.parentCommand.pluginTestRunner.getPluginManagerService();
            PTRLogger logger = pluginManagerService.getLogger();
            if (pluginProductList.size() != domainNameList.size()) {
                logger.rlog(LogLevel.ERROR, "The number of product and domain name should be the same.");
            } else {
                for (int i = 0; i < pluginProductList.size(); i++) {
                    String pluginProduct = pluginProductList.get(i);
                    String domainName = domainNameList.get(i);
                    try {
                        pluginManagerService.testCreateBBCService(pluginProduct, domainName);
                        logger.rlog(LogLevel.INFO, "Successfully created BBC service for product: " + pluginProduct + "with domain " + domainName);
                    } catch (PluginManagerException e) {
                        logger.rlog(LogLevel.ERROR, e.getMessage());
                        if (e.getCause() != null) {
                            logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                        }
                    }
                }
            }
        }
    }

    // 判断多个 List 是否大小相等
    private static boolean areListsEqualSize(List<?>... lists) {
        int size = lists[0].size();
        for (List<?> list : lists) {
            if (list != null && list.size() != size) {
                return false;
            }
        }
        return true;
    }
}
