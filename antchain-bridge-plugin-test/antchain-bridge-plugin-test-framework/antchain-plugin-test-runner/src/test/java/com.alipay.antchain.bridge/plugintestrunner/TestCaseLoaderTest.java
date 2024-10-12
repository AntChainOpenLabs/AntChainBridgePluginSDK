package com.alipay.antchain.bridge.plugintestrunner;

import com.alipay.antchain.bridge.plugintestrunner.exception.TestCaseException;
import com.alipay.antchain.bridge.plugintestrunner.testcase.TestCase;
import com.alipay.antchain.bridge.plugintestrunner.testcase.TestCaseContainer;
import com.alipay.antchain.bridge.plugintestrunner.testcase.TestCaseLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCaseLoaderTest {


    private final String testCasesPath = "src/test/resources/testcase.json";

    private TestCaseContainer testCaseContainer;

    @BeforeEach
    public void init() throws TestCaseException {
        testCaseContainer = TestCaseLoader.loadTestCasesFromFile(testCasesPath);
    }

    @Test
    public void testGetFieldValue() throws NoSuchFieldException, IllegalAccessException {
        TestCase testCase = testCaseContainer.getTestCases().get(0);
        Object startup = testCase.getFieldValue("startup");
    }
}
