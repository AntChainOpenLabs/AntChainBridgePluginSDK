package org.example.plugintestrunner;

import lombok.Getter;
import org.example.plugintestrunner.config.ChainConfigManager;
import org.example.plugintestrunner.exception.TestCaseLoaderException;
import org.example.plugintestrunner.service.ChainManagerService;
import org.example.plugintestrunner.service.PluginManagerService;
import org.example.plugintestrunner.service.PluginTestService;
import org.example.plugintestrunner.testcase.TestCaseContainer;
import org.example.plugintestrunner.testcase.TestCaseLoader;
import org.example.plugintestrunner.util.LogLevel;
import org.example.plugintestrunner.util.PTRLogger;
import org.example.plugintestrunner.util.ShellScriptRunner;
import org.example.plugintestrunner.testcase.TestCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

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

    public static void main(String[] args) throws IOException, TestCaseLoaderException {
        PluginTestRunner pluginTestRunner = init();
        pluginTestRunner.run();
    }

    public void run() throws IOException {
        for (TestCase testCase : testCaseContainer.getTestCases()) {
            logger.rlog(LogLevel.INFO, "Running " + testCase.getName());
            chainManagerService.run(testCase);
            pluginManagerService.run(testCase);
//            pluginTestService.run(testCase);
        }
//        pluginTestService.close();
        pluginManagerService.close();
        chainManagerService.close();

        printTestResult();
    }

    public static PluginTestRunner init() throws IOException, TestCaseLoaderException {
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
