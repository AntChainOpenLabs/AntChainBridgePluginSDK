package org.example.plugintestrunner;

import org.example.plugintestrunner.exception.TestCaseLoaderException;
import org.example.plugintestrunner.service.PluginTestService;
import org.example.plugintestrunner.testcase.TestCase;
import org.example.plugintestrunner.testcase.TestCaseContainer;
import org.example.plugintestrunner.testcase.TestCaseLoader;
import org.example.plugintestrunner.util.PTRLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PluginTestServiceTest {

    private PluginTestService pluginTestService;

    private TestCaseContainer testCaseContainer;

    private final String testCasesPath = "src/main/resources/testcase.json";

    // 创建 PluginTestService，加载测试用例
    @BeforeEach
    public void init() throws TestCaseLoaderException {
        PTRLogger logger = new PTRLogger();
        pluginTestService = new PluginTestService(logger);
        testCaseContainer = TestCaseLoader.loadTestCasesFromFile(testCasesPath);
    }

    @Test
    public void testRun() {
        for (TestCase testCase : testCaseContainer.getTestCases()) {
            pluginTestService.run(testCase);
        }
    }
}
