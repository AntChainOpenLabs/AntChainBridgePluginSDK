package org.example.plugintestrunner.service;

import org.example.plugintestrunner.exception.PluginTestException;
import org.example.plugintestrunner.exception.PluginTestException.*;
import org.example.plugintestrunner.testcase.TestCase;
import org.example.plugintestrunner.util.LogLevel;
import org.example.plugintestrunner.util.PTRLogger;

import java.util.HashMap;
import java.util.List;

public class PluginTestService extends AbstractService{

    public PluginTestService(PTRLogger logger) {
        super(logger);
    }

    @Override
    public void run(TestCase testCase) {
        // 设置 MDC
        logger.putMDC(testCase.getName());

        // TODO
        try {
            runTest(testCase);
            logger.rlog(LogLevel.INFO, "Plugin interface test for " + testCase.getName() + ": PASSED");
        } catch (PluginTestException e) {
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

    }


    private void runTest(TestCase testCase) throws PluginTestException {
        if (testCase.isStartup()) {
            checkDependency("startup", testCase);
            testStartUp();
        }
        if (testCase.isShutdown()) {
            checkDependency("shutdown", testCase);
            testShutDown();
        }
        if (testCase.isGetContext()) {
            checkDependency("getContext", testCase);
            testGetContext();
        }
        if (testCase.isQueryLatestHeight()) {
            checkDependency("queryLatestHeight", testCase);
            testQueryLatestHeight();
        }
        if (testCase.isSetupAuthMessageContract()) {
            checkDependency("setupAuthMessageContract", testCase);
            testSetupAuthMessageContract();
        }
        if (testCase.isSetupSDPMessageContract()) {
            checkDependency("setupSDPMessageContract", testCase);
            testSetupSDPMessageContract();
        }
        if (testCase.isSetLocalDomain()) {
            checkDependency("setLocalDomain", testCase);
            testSetLocalDomain();
        }
        if (testCase.isQuerySDPMessageSeq()) {
            checkDependency("querySDPMessageSeq", testCase);
            testQuerySDPMessageSeq();
        }
        if (testCase.isSetProtocol()) {
            checkDependency("setProtocol", testCase);
            testSetProtocol();
        }
        if (testCase.isSetAmContract()) {
            checkDependency("setAmContract", testCase);
            testSetAmContract();
        }
        if (testCase.isReadCrossChainMessagesByHeight()) {
            checkDependency("readCrossChainMessagesByHeight", testCase);
            testReadCrossChainMessagesByHeight();
        }
        if (testCase.isRelayAuthMessage()) {
            checkDependency("relayAuthMessage", testCase);
            testRelayAuthMessage();
        }
        if (testCase.isReadCrossChainMessageReceipt()) {
            checkDependency("readCrossChainMessageReceipt", testCase);
            testReadCrossChainMessageReceipt();
        }
    }

    // 检查每个接口的依赖项，如果依赖项没有执行，则抛出异常
    private void checkDependency(String item, TestCase testCase) throws PluginTestException{
        HashMap<String, List<String>> map = testCase.getPluginInterfaceTestDependency();
        List<String> dependencies = map.get(item);
        if (dependencies == null) {
            return;
        }
        for (String dependency : dependencies) {
            try {
                boolean flag = (Boolean) testCase.getFieldValue(dependency);
                if (!flag) {
                    throw new DependencyException(dependency + " test must be executed before " + item);
                }
            } catch (Exception e) {
                throw new DependencyException("Failed to check dependency for: " + item, e);
            }
        }
    }

    // dependency: none
    public void testStartUp() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new StartupException("Failed to run startup test.", e);
        }
    }

    // dependency: testStartUp
    public void testShutDown() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new ShutdownException("Failed to run shutdown test.", e);
        }
    }

    // dependency: testStartUp
    public void testGetContext() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new GetContextException("Failed to run get context test.", e);
        }
    }

    // dependency: testStartUp
    public void testQueryLatestHeight() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new QueryLatestHeightException("Failed to run query latest height test.", e);
        }
    }

    // dependency: testStartUp, testGetContext
    public void testSetupAuthMessageContract() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new SetupAuthMessageContractException("Failed to run setup auth message contract test.", e);
        }
    }

    // dependency: testStartUp, testGetContext
    public void testSetupSDPMessageContract() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new SetupSDPMessageContractException("Failed to run setup sdp message contract test.", e);
        }
    }

    // dependency: testStartUp, testGetContext, testSetupSDPMessageContract
    public void testSetLocalDomain() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new SetLocalDomainException("Failed to run set local domain test.", e);
        }
    }

    // dependency: testStartUp, testGetContext, testSetupSDPMessageContract, testSetLocalDomain
    public void testQuerySDPMessageSeq() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new QuerySDPMessageSeqException("Failed to run query sdp message seq test.", e);
        }
    }

    // dependency: testStartUp, testGetContext, testSetupAuthMessageContract, testSetupSDPMessageContract
    public void testSetProtocol() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new SetProtocolException("Failed to run set protocol test.", e);
        }
    }

    // dependency: testStartUp, testGetContext, testSetupAuthMessageContract, testSetupSDPMessageContract
    public void testSetAmContract() throws PluginTestException {
        // TODO
        try {
            // TODO
        } catch (Exception e) {
            throw new SetAmContractException("Failed to run set am contract test.", e);
        }
    }

    // dependency: testStartUp, testGetContext, testSetupAuthMessageContract,
    //              testSetupSDPMessageContract, testSetProtocol, testSetAmContract, testSetLocalDomain
    public void testReadCrossChainMessagesByHeight() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new ReadCrossChainMessagesByHeightException("Failed to run read cross chain messages by height test.", e);
        }
    }

    // dependency: testStartUp, testGetContext, testSetupAuthMessageContract,
    //              testSetupSDPMessageContract, testSetProtocol, testSetAmContract, testSetLocalDomain
    public void testRelayAuthMessage() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new RelayAuthMessageException("Failed to run relay auth message test.", e);
        }
    }

    // dependency: testStartUp, testGetContext, testSetupAuthMessageContract,
    //              testSetupSDPMessageContract, testSetProtocol, testRelayAuthMessage, testSetAmContract, testSetLocalDomain
    public void testReadCrossChainMessageReceipt() throws PluginTestException {
        try {
            // TODO
        } catch (Exception e) {
            throw new ReadCrossChainMessageReceiptException("Failed to run read cross chain message receipt test.", e);
        }
    }

}