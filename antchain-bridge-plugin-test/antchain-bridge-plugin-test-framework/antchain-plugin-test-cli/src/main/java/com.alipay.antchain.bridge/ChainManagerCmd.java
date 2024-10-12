package com.alipay.antchain.bridge;

import com.alipay.antchain.bridge.plugintestrunner.service.ChainManagerService;
import com.alipay.antchain.bridge.plugintestrunner.util.LogLevel;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.List;

@Command(name = "chain-manager", mixinStandardHelpOptions = true,description = "Start or close a test chain.",subcommands={
    ChainManagerCmd.ChainStart.class,
    ChainManagerCmd.ChainStop.class,
    ChainManagerCmd.ChainShow.class
})
public class ChainManagerCmd implements Runnable{
    @ParentCommand
    private App parentCommand;

    @Override
    public void run() {
    }

    @Command(name = "start", mixinStandardHelpOptions = true, description = "Start a test chain.")
    public static class ChainStart implements Runnable {

        @ParentCommand
        private ChainManagerCmd parentCommand;

        // 指定链的类型
        @Option(names = {"-p", "--product"}, description = "Product of chain", required = true, split = ",")
        private List<String> productList;

        @Override
        public void run() {
            ChainManagerService chainManagerService = parentCommand.parentCommand.pluginTestRunner.getChainManagerService();
            PTRLogger logger = chainManagerService.getLogger();
            for (String product : productList) {
                try {
                    chainManagerService.startup(product);
                    logger.rlog(LogLevel.INFO, "Successfully started chain: " + product);
                } catch (Exception e) {
                    logger.rlog(LogLevel.ERROR, "Failed to start chain: " + product);
                    logger.rlog(LogLevel.ERROR, e.getMessage());
                }
            }
        }
    } 


    @Command(name = "stop", mixinStandardHelpOptions = true, description = "Stop a test chain.")
    public static class ChainStop implements Runnable {

        @ParentCommand
        private ChainManagerCmd parentCommand;

        // 指定链的类型
        @Option(names = {"-p", "--product"}, description = "Product of chain", required = true, split = ",")
        private List<String> productList;

        @Override
        public void run() {
            ChainManagerService chainManagerService = parentCommand.parentCommand.pluginTestRunner.getChainManagerService();
            PTRLogger logger = chainManagerService.getLogger();
            for (String product : productList) {
                try {
                    chainManagerService.shutdown(product);
                    logger.rlog(LogLevel.INFO, "Successfully stopped chain: " + product);
                } catch (Exception e) {
                    logger.rlog(LogLevel.ERROR, "Failed to stop chain: " + product);
                    logger.rlog(LogLevel.ERROR, e.getMessage());
                }
            }
        }
    } 

    @Command(name = "show", mixinStandardHelpOptions = true, description = "Show all test chains.")
    public static class ChainShow implements Runnable {

        @ParentCommand
        private ChainManagerCmd parentCommand;

        @Override
        public void run() {
            ChainManagerService chainManagerService = parentCommand.parentCommand.pluginTestRunner.getChainManagerService();
            chainManagerService.showChainManagers();
        }
    }
}
