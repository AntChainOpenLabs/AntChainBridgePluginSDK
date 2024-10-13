package com.alipay.antchain.bridge.plugintestrunner;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.alipay.antchain.bridge.plugintestrunner.config.ChainConfigManager;
import com.alipay.antchain.bridge.plugintestrunner.exception.TestCaseException;
import com.alipay.antchain.bridge.plugintestrunner.service.ChainManagerService;
import com.alipay.antchain.bridge.plugintestrunner.service.PluginManagerService;
import com.alipay.antchain.bridge.plugintestrunner.service.PluginTestService;
import com.alipay.antchain.bridge.plugintestrunner.testcase.TestCase;
import com.alipay.antchain.bridge.plugintestrunner.testcase.TestCaseContainer;
import com.alipay.antchain.bridge.plugintestrunner.testcase.TestCaseLoader;
import com.alipay.antchain.bridge.plugintestrunner.util.LogLevel;
import com.alipay.antchain.bridge.plugintestrunner.util.PTRLogger;
import com.alipay.antchain.bridge.plugintestrunner.util.ShellScriptRunner;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import lombok.Getter;

public class PluginTestRunner {

    @Getter
    private final PluginManagerService pluginManagerService;
    @Getter
    private final PluginTestService pluginTestService;
    @Getter
    private final ChainManagerService chainManagerService;
    @Getter
    private final PTRLogger logger;
    private final TestCaseContainer testCaseContainer;

    public PluginTestRunner(PTRLogger logger, PluginManagerService pluginManagerService, PluginTestService pluginTestService, ChainManagerService chainManagerService, TestCaseContainer testCaseContainer) {
        this.logger = logger;
        this.pluginManagerService = pluginManagerService;
        this.pluginTestService = pluginTestService;
        this.chainManagerService = chainManagerService;
        this.testCaseContainer = testCaseContainer;
    }

    public static void main(String[] args) throws IOException, TestCaseException {
        PluginTestRunner pluginTestRunner = init();
        pluginTestRunner.run();
    }

    public void run() throws IOException {
        for (TestCase testCase : testCaseContainer.getTestCases()) {
            logger.rlog(LogLevel.INFO, "Running " + testCase.getName());
            chainManagerService.run(testCase);
            pluginManagerService.run(testCase);
            pluginTestService.run(testCase);
        }
        pluginTestService.close();
        pluginManagerService.close();
        chainManagerService.close();

        printTestResult();
    }

    public static PluginTestRunner init() throws IOException, TestCaseException {
        ChainConfigManager configManager = ChainConfigManager.getInstance();
        PTRLogger logger = PTRLogger.getInstance();
        ShellScriptRunner shellScriptRunner = new ShellScriptRunner(configManager.getProperty("log.directory"),
                configManager.getProperty("script.directory"));
        ChainManagerService chainManagerService = new ChainManagerService(logger, shellScriptRunner);
        PluginManagerService pluginManagerService = new PluginManagerService(logger, configManager.getProperty("plugin.directory"));
        PluginTestService pluginTestService = new PluginTestService(logger, pluginManagerService, chainManagerService);
        TestCaseContainer testCaseContainer = TestCaseLoader.loadTestCasesFromFile(configManager.getProperty("testcase.path"));
        return new PluginTestRunner(logger, pluginManagerService, pluginTestService, chainManagerService, testCaseContainer);
    }

    public void close() {
        pluginTestService.close();
        pluginManagerService.close();
        chainManagerService.close();
    }

    private void printTestResult() {
        HashMap<String, Boolean> pluginLoadAndStartTestResult = testCaseContainer.getPluginLoadAndStartTestResult();
        HashMap<String, Boolean> pluginInterfaceTestResult = testCaseContainer.getPluginInterfaceTestResult();

        AsciiTable table = new AsciiTable();

        table.addRule();
        table.addRow("Case", "Test Item", "Result");

        for (Map.Entry<String, Boolean> entry : pluginLoadAndStartTestResult.entrySet()) {
            table.addRule();
            table.addRow(entry.getKey(), "LoadAndStartTest", entry.getValue() ? "PASSED" : "FAILED");
            table.addRow("", "InterfaceTest", pluginInterfaceTestResult.get(entry.getKey()) ? "PASSED" : "FAILED");
        }
        table.addRule();
        table.setTextAlignment(TextAlignment.CENTER);

        // 格式化输出
        String resultTable = table.render();
        logger.rlog(LogLevel.INFO, "\n" + resultTable);
    }
}
