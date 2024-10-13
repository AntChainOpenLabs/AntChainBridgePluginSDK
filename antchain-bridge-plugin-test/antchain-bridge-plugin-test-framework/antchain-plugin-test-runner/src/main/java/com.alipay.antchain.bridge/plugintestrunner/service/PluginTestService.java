package com.alipay.antchain.bridge.plugintestrunner.service;

import com.alipay.antchain.bridge.EthPluginTestTool;
import com.alipay.antchain.bridge.abstarct.IPluginTestTool;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.IChainManager;
import com.alipay.antchain.bridge.plugintestrunner.config.ChainProduct;
import com.alipay.antchain.bridge.plugintestrunner.exception.ChainManagerException;
import com.alipay.antchain.bridge.plugintestrunner.exception.PluginTestException;
import com.alipay.antchain.bridge.plugintestrunner.testcase.TestCase;
import com.alipay.antchain.bridge.plugintestrunner.util.LogLevel;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import com.alipay.antchain.bridge.plugintestrunner.exception.PluginTestException.*;

import lombok.Setter;
import org.aspectj.weaver.ast.Test;

import java.util.List;

public class PluginTestService extends AbstractService{

    @Setter
    private PluginManagerService pluginManagerService;
    @Setter
    private ChainManagerService chainManagerService;

    private IBBCService bbcService;
    private AbstractBBCContext bbcContext;

    public PluginTestService(PTRLogger logger, PluginManagerService pluginManagerService, ChainManagerService chainManagerService) {
        super(logger);
        this.pluginManagerService = pluginManagerService;
        this.chainManagerService = chainManagerService;
    }

    public String getSupportedInterfaces() {
        return TestOperation.getAllOperationNames();
    }

    @Override
    public void run(TestCase testCase) {
        // 设置 MDC
        logger.putMDC(testCase.getName());

        try {
            // 获取 bbcService 时可能会抛出异常
            runTest(testCase);
            boolean isSucceed = summarizeTestResults(testCase);
            testCase.setPluginInterfaceTestSuccess(isSucceed);
        } catch (Exception e) {
            logger.rlog(LogLevel.ERROR, "Plugin interface test for " + testCase.getName() + ": FAILED");
            logger.rlog(LogLevel.ERROR, e.getMessage());
            if (e.getCause() != null) {
                logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
            }
        }
        // 清除 MDC
        logger.clearMDC();
    }

    @Override
    public void close() {
        // TODO
    }

    // 命令行的调用接口
    public void runCmd(String jarPath, String product, List<String> functionList) throws PluginTestException {
        // 1. 判断插件是否启动过
        String domain = product + "-domain";
        logger.rlog(LogLevel.INFO, "Create BBC Service for: " + product);
        if (pluginManagerService.hasPlugin(product)) {
            try {
                pluginManagerService.testStopPlugin(product);
                pluginManagerService.testStartPluginFromStop(product);
                pluginManagerService.testCreateBBCService(product, domain);
            } catch (Exception e) {
                throw new PluginTestException("Failed to stop plugin: " + product, e);
            }
        } else {
            try {
                pluginManagerService.testLoadPlugin(jarPath);
                pluginManagerService.testStartPlugin(jarPath);
                pluginManagerService.testCreateBBCService(product, domain);
            } catch (Exception e) {
                throw new PluginTestException("Failed to create BBC service for: " + product, e);
            }
        }
        IBBCService bbcService = pluginManagerService.getBBCService(product, domain);
        IChainManager chainManager;
        logger.rlog(LogLevel.INFO, "Create ChainManager for: " + product);
        // 2. 判断链是否启动过
        try {
            chainManager = chainManagerService.getChainManager(product);
        } catch (Exception e1) {
            try {
                chainManagerService.startup(product);
                chainManager = chainManagerService.getChainManager(product);
            } catch (Exception e2) {
                throw new PluginTestException("Failed to start chain: " + product, e2);
            }
        }
        AbstractBBCContext bbcContext = chainManager.getBBCContext();
        IPluginTestTool pluginTestTool = getPluginTester(product, bbcContext, bbcService);

        // 执行测试
        int totalTests = 0;
        int successfulTests = 0;
        int failedTests = 0;

        for (String func : functionList) {
            TestOperation op = null;
            totalTests++;
            try {
                op = TestOperation.fromString(func);
            } catch (Exception e) {
                logger.rlog(LogLevel.ERROR, "Unknown operation: " + func);
                failedTests++;
                continue;
            }

            try {
                switch (op) {
                    case STARTUP:
                        pluginTestTool.startupTest();
                        successfulTests++;
                        break;

                    case SHUTDOWN:
                        pluginTestTool.shutdownTest();
                        successfulTests++;
                        break;

                    case GET_CONTEXT:
                        pluginTestTool.getContextTest();
                        successfulTests++;
                        break;

                    // case QUERY_LATEST_HEIGHT:
                    //     pluginTestTool.queryLatestHeightTest();
                    //     successfulTests++;
                    //     break;

                    case SETUP_AUTH_MESSAGE_CONTRACT:
                        pluginTestTool.setupAmContractTest();
                        successfulTests++;
                        break;

                    case SETUP_SDP_MESSAGE_CONTRACT:
                        pluginTestTool.setupSdpContractTest();
                        successfulTests++;
                        break;

                    case SET_LOCAL_DOMAIN:
                        pluginTestTool.setAmContractAndLocalDomainTest();
                        successfulTests++;
                        break;

                    case QUERY_SDP_MESSAGE_SEQ:
                        pluginTestTool.querySdpMessageSeqTest();
                        successfulTests++;
                        break;

                    case SET_PROTOCOL:
                        pluginTestTool.setProtocolTest();
                        successfulTests++;
                        break;

                    case SET_AM_CONTRACT:
                        pluginTestTool.setAmContractAndLocalDomainTest();
                        successfulTests++;
                        break;

                    case READ_CROSS_CHAIN_MESSAGES_BY_HEIGHT:
                        pluginTestTool.readCrossChainMessageByHeightTest();
                        successfulTests++;
                        break;

                    case RELAY_AUTH_MESSAGE:
                        pluginTestTool.relayAuthMessageTest();
                        successfulTests++;
                        break;

                    case READ_CROSS_CHAIN_MESSAGE_RECEIPT:
                        pluginTestTool.readCrossChainMessageReceiptTest();
                        successfulTests++;
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown operation: " + op);
                }
            } catch (Exception e) {
                logger.rlog(LogLevel.ERROR, "Error during operation: " + op.getOperationName() + " - " + e.getMessage());
                logger.rlog(LogLevel.ERROR, e.getMessage());
                if (e.getCause() != null) {
                    logger.rlog(LogLevel.ERROR, e.getCause().getMessage());
                }
                failedTests++;  // 捕获操作异常，计入失败数量
            }
        }

        // 输出测试结果
        logger.rlog(LogLevel.INFO, "Total tests: " + totalTests + ", Successful tests: " + successfulTests + ", Failed tests: " + failedTests);
    }


    // 运行每个测试用例的接口测试
    private void runTest(TestCase testCase) throws PluginTestException, ChainManagerException {
        // 从 pluginManagerService 中获取 bbcService
        bbcService = pluginManagerService.getBBCService(testCase.getProduct(), testCase.getDomain());
        // 从 chainManagerService 中获取 abstractBBCContext
        bbcContext = chainManagerService.getChainManager(testCase.getProduct()).getBBCContext();
        IPluginTestTool pluginTestTool = getPluginTester(testCase.getProduct(), bbcContext, bbcService);
        if (testCase.isStartup()) {
            try {
                pluginTestTool.startupTest();
                pluginTestTool.shutdownTest();
                testCase.setStartupSuccess(true);
            } catch (Exception e) {
                testCase.setStartupSuccess(false);
                logger.plog(LogLevel.ERROR, "Startup test failed: " + e.getMessage());
            }
        }

        if (testCase.isShutdown()) {
            try {
                pluginTestTool.shutdownTest();
                testCase.setShutdownSuccess(true);
            } catch (Exception e) {
                testCase.setShutdownSuccess(false);
                logger.plog(LogLevel.ERROR, "Shutdown test failed: " + e.getMessage());
            }
        }

        if (testCase.isGetContext()) {
            try {
                pluginTestTool.getContextTest();
                pluginTestTool.shutdownTest();
                testCase.setGetContextSuccess(true);
            } catch (Exception e) {
                testCase.setGetContextSuccess(false);
                logger.plog(LogLevel.ERROR, "GetContext test failed: " + e.getMessage());
            }
        }

        // TODO: 测试工具还没实现
        // if (testCase.isQueryLatestHeight()) {
        //
        // }

        if (testCase.isSetupAuthMessageContract()) {
            try {
                pluginTestTool.setupAmContractTest();
                pluginTestTool.shutdownTest();
                testCase.setSetupAuthMessageContractSuccess(true);
            } catch (Exception e) {
                testCase.setSetupAuthMessageContractSuccess(false);
                logger.plog(LogLevel.ERROR, "SetupAuthMessageContract test failed: " + e.getMessage());
            }
        }

        if (testCase.isSetupSDPMessageContract()) {
            try {
                pluginTestTool.setupSdpContractTest();
                pluginTestTool.shutdownTest();
                testCase.setSetupSDPMessageContractSuccess(true);
            } catch (Exception e) {
                testCase.setSetupSDPMessageContractSuccess(false);
                logger.plog(LogLevel.ERROR, "SetupSDPMessageContract test failed: " + e.getMessage());
            }
        }

        if (testCase.isSetLocalDomain()) {
            try {
                pluginTestTool.setAmContractAndLocalDomainTest();
                pluginTestTool.shutdownTest();
                testCase.setSetLocalDomainSuccess(true);
            } catch (Exception e) {
                testCase.setSetLocalDomainSuccess(false);
                logger.plog(LogLevel.ERROR, "SetLocalDomain test failed: " + e.getMessage());
            }
        }

        if (testCase.isQuerySDPMessageSeq()) {
            try {
                pluginTestTool.querySdpMessageSeqTest();
                pluginTestTool.shutdownTest();
                testCase.setQuerySDPMessageSeqSuccess(true);
            } catch (Exception e) {
                testCase.setQuerySDPMessageSeqSuccess(false);
                logger.plog(LogLevel.ERROR, "QuerySDPMessageSeq test failed: " + e.getMessage());
            }
        }

        if (testCase.isSetProtocol()) {
            try {
                pluginTestTool.setProtocolTest();
                pluginTestTool.shutdownTest();
                testCase.setSetProtocolSuccess(true);
            } catch (Exception e) {
                testCase.setSetProtocolSuccess(false);
                logger.plog(LogLevel.ERROR, "SetProtocol test failed: " + e.getMessage());
            }
        }

        if (testCase.isSetAmContract()) {
            try {
                pluginTestTool.setAmContractAndLocalDomainTest();
                pluginTestTool.shutdownTest();
                testCase.setSetAmContractSuccess(true);
            } catch (Exception e) {
                testCase.setSetAmContractSuccess(false);
                logger.plog(LogLevel.ERROR, "SetAmContract test failed: " + e.getMessage());
            }
        }

        if (testCase.isReadCrossChainMessagesByHeight()) {
            try {
                pluginTestTool.readCrossChainMessageByHeightTest();
                pluginTestTool.shutdownTest();
                testCase.setReadCrossChainMessagesByHeightSuccess(true);
            } catch (Exception e) {
                testCase.setReadCrossChainMessagesByHeightSuccess(false);
                logger.plog(LogLevel.ERROR, "ReadCrossChainMessagesByHeight test failed: " + e.getMessage());
            }
        }

        if (testCase.isRelayAuthMessage()) {
            try {
                pluginTestTool.relayAuthMessageTest();
                pluginTestTool.shutdownTest();
                testCase.setRelayAuthMessageSuccess(true);
            } catch (Exception e) {
                testCase.setRelayAuthMessageSuccess(false);
                logger.plog(LogLevel.ERROR, "RelayAuthMessage test failed: " + e.getMessage());
            }
        }

        if (testCase.isReadCrossChainMessageReceipt()) {
            try {
                pluginTestTool.readCrossChainMessageReceiptTest();
                pluginTestTool.shutdownTest();
                testCase.setReadCrossChainMessageReceiptSuccess(true);
            } catch (Exception e) {
                testCase.setReadCrossChainMessageReceiptSuccess(false);
                logger.plog(LogLevel.ERROR, "ReadCrossChainMessageReceipt test failed: " + e.getMessage());
            }
        }
    }

    // 根据 product 生成 IPluginTestTool
    private IPluginTestTool getPluginTester(String product, AbstractBBCContext bbcContext, IBBCService bbcService) throws PluginTestToolNotSupportException {
        ChainProduct cp = ChainProduct.fromValue(product);
        switch (cp) {
            case ETH:
                return new EthPluginTestTool(bbcContext, (AbstractBBCService)bbcService);
            // TODO
            default:
                throw new PluginTestToolNotSupportException("Plugin test tool for " + product + " is not supported.");
        }
    }

    // 统计并打印测试结果
    public boolean summarizeTestResults(TestCase testCase) {
        int totalTests = 0;
        int successfulTests = 0;
        int failedTests = 0;
        StringBuilder failedTestNames = new StringBuilder();

        if (testCase.isStartup()) {
            totalTests++;
            if (testCase.isStartupSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("Startup, ");
            }
        }

        if (testCase.isShutdown()) {
            totalTests++;
            if (testCase.isShutdownSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("Shutdown, ");
            }
        }

        if (testCase.isGetContext()) {
            totalTests++;
            if (testCase.isGetContextSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("GetContext, ");
            }
        }

        if (testCase.isSetupAuthMessageContract()) {
            totalTests++;
            if (testCase.isSetupAuthMessageContractSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("SetupAuthMessageContract, ");
            }
        }

        if (testCase.isSetupSDPMessageContract()) {
            totalTests++;
            if (testCase.isSetupSDPMessageContractSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("SetupSDPMessageContract, ");
            }
        }

        if (testCase.isSetLocalDomain()) {
            totalTests++;
            if (testCase.isSetLocalDomainSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("SetLocalDomain, ");
            }
        }

        if (testCase.isQuerySDPMessageSeq()) {
            totalTests++;
            if (testCase.isQuerySDPMessageSeqSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("QuerySDPMessageSeq, ");
            }
        }

        if (testCase.isSetProtocol()) {
            totalTests++;
            if (testCase.isSetProtocolSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("SetProtocol, ");
            }
        }

        if (testCase.isSetAmContract()) {
            totalTests++;
            if (testCase.isSetAmContractSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("SetAmContract, ");
            }
        }

        if (testCase.isReadCrossChainMessagesByHeight()) {
            totalTests++;
            if (testCase.isReadCrossChainMessagesByHeightSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("ReadCrossChainMessagesByHeight, ");
            }
        }

        if (testCase.isRelayAuthMessage()) {
            totalTests++;
            if (testCase.isRelayAuthMessageSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("RelayAuthMessage, ");
            }
        }

        if (testCase.isReadCrossChainMessageReceipt()) {
            totalTests++;
            if (testCase.isReadCrossChainMessageReceiptSuccess()) {
                successfulTests++;
            } else {
                failedTests++;
                failedTestNames.append("ReadCrossChainMessageReceipt, ");
            }
        }

        // 去掉最后一个多余的逗号和空格
        if (failedTestNames.length() > 0) {
            failedTestNames.setLength(failedTestNames.length() - 2);
        }

        // 打印测试结果
        logger.rlog(LogLevel.INFO, "Total tests: " + totalTests + ", " + successfulTests + " succeeded, " + failedTests + " failed.");

        if (failedTests > 0) {
            logger.rlog(LogLevel.INFO, "Failed tests: " + failedTestNames.toString());
            return false;  // There are failed tests, return false
        }

        return true;  // 所有测试成功，返回 true
    }


//    private void runTest(TestCase testCase) throws PluginTestException, ChainManagerException {
//        this.testCase = testCase;
//        // 从 pluginManagerService 中获取 bbcService
//        bbcService = pluginManagerService.getBBCService(testCase.getProduct(), testCase.getDomain());
//        // 从 chainManagerService 中获取 abstractBBCContext
//        bbcContext = chainManagerService.getChainManager(testCase.getProduct()).getBBCContext();
//        if (testCase.isStartup()) {
//            checkDependency("startup", testCase);
//            testStartUp();
//        }
//        if (testCase.isShutdown()) {
//            checkDependency("shutdown", testCase);
//            testShutDown();
//        }
//        if (testCase.isGetContext()) {
//            checkDependency("getContext", testCase);
//            testGetContext();
//        }
//        if (testCase.isQueryLatestHeight()) {
//            checkDependency("queryLatestHeight", testCase);
//            testQueryLatestHeight();
//        }
//        if (testCase.isSetupAuthMessageContract()) {
//            checkDependency("setupAuthMessageContract", testCase);
//            testSetupAuthMessageContract();
//        }
//        if (testCase.isSetupSDPMessageContract()) {
//            checkDependency("setupSDPMessageContract", testCase);
//            testSetupSDPMessageContract();
//        }
//        if (testCase.isSetLocalDomain()) {
//            checkDependency("setLocalDomain", testCase);
//            testSetLocalDomain();
//        }
//        if (testCase.isQuerySDPMessageSeq()) {
//            checkDependency("querySDPMessageSeq", testCase);
//            testQuerySDPMessageSeq();
//        }
//        if (testCase.isSetProtocol()) {
//            checkDependency("setProtocol", testCase);
//            testSetProtocol();
//        }
//        if (testCase.isSetAmContract()) {
//            checkDependency("setAmContract", testCase);
//            testSetAmContract();
//        }
//        if (testCase.isReadCrossChainMessagesByHeight()) {
//            checkDependency("readCrossChainMessagesByHeight", testCase);
//            testReadCrossChainMessagesByHeight();
//        }
//        if (testCase.isRelayAuthMessage()) {
//            checkDependency("relayAuthMessage", testCase);
//            testRelayAuthMessage();
//        }
//        if (testCase.isReadCrossChainMessageReceipt()) {
//            checkDependency("readCrossChainMessageReceipt", testCase);
//            testReadCrossChainMessageReceipt();
//        }
//    }

    // 检查每个接口的依赖项，如果依赖项没有执行，则抛出异常
//    private void checkDependency(String item, TestCase testCase) throws PluginTestException{
//        HashMap<String, List<String>> map = testCase.getPluginInterfaceTestDependency();
//        List<String> dependencies = map.get(item);
//        if (dependencies == null) {
//            return;
//        }
//        for (String dependency : dependencies) {
//            try {
//                boolean flag = (Boolean) testCase.getFieldValue(dependency);
//                if (!flag) {
//                    throw new DependencyException(dependency + " test must be executed before " + item);
//                }
//            } catch (Exception e) {
//                throw new DependencyException("Failed to check dependency for: " + item, e);
//            }
//        }
//    }

    public enum TestOperation {
        STARTUP("startup"),
        SHUTDOWN("shutdown"),
        GET_CONTEXT("getContext"),
        QUERY_LATEST_HEIGHT("queryLatestHeight"),
        SETUP_AUTH_MESSAGE_CONTRACT("setupAuthMessageContract"),
        SETUP_SDP_MESSAGE_CONTRACT("setupSDPMessageContract"),
        SET_LOCAL_DOMAIN("setLocalDomain"),
        QUERY_SDP_MESSAGE_SEQ("querySDPMessageSeq"),
        SET_PROTOCOL("setProtocol"),
        SET_AM_CONTRACT("setAmContract"),
        READ_CROSS_CHAIN_MESSAGES_BY_HEIGHT("readCrossChainMessagesByHeight"),
        RELAY_AUTH_MESSAGE("relayAuthMessage"),
        READ_CROSS_CHAIN_MESSAGE_RECEIPT("readCrossChainMessageReceipt");

        private final String operationName;

        TestOperation(String operationName) {
            this.operationName = operationName;
        }

        public String getOperationName() {
            return operationName;
        }

        public static TestOperation fromString(String operationName) {
            for (TestOperation operation : TestOperation.values()) {
                if (operation.getOperationName().equalsIgnoreCase(operationName)) {
                    return operation;
                }
            }
            // 如果没有匹配项，可以抛出异常或返回 null
            throw new IllegalArgumentException("No enum constant for operation: " + operationName);
        }

        public static String getAllOperationNames() {
            StringBuilder operationNames = new StringBuilder();
            for (TestOperation operation : TestOperation.values()) {
                if (operationNames.length() > 0) {
                    operationNames.append(", ");
                }
                operationNames.append(operation.getOperationName());
            }
            return operationNames.toString();
        }

    }
}