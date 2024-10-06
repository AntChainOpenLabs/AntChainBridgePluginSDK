package org.example.plugintestrunner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.example.plugintestrunner.exception.TestCaseException;
import org.example.plugintestrunner.testcase.TestCase;
import org.example.plugintestrunner.testcase.TestCaseContainer;
import org.example.plugintestrunner.testcase.TestCaseLoader;
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
        assertTrue(startup instanceof Boolean);
        assertTrue((Boolean) startup);
    }
}
