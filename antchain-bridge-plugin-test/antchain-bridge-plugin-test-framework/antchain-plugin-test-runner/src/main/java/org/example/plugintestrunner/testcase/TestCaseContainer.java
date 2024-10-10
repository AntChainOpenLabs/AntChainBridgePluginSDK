package org.example.plugintestrunner.testcase;

import lombok.Getter;
import lombok.Setter;
import org.example.plugintestrunner.exception.TestCaseException;

import java.util.HashMap;
import java.util.List;

@Setter
@Getter
public class TestCaseContainer {
    private List<TestCase> testCases;

    public void init() throws TestCaseException {
        for (TestCase testCase : testCases) {
            testCase.setPluginLoadAndStartTestFlag();
            testCase.setPluginInterfaceTestFlag();
        }
    }

    public HashMap<String, Boolean> getPluginLoadAndStartTestResult() {
        HashMap<String, Boolean> testResult = new HashMap<>();
        for (TestCase testCase : testCases) {
            testResult.put(testCase.getName(), testCase.isPluginLoadAndStartTestSuccess());
        }
        return testResult;
    }

    public HashMap<String, Boolean> getPluginInterfaceTestResult() {
        HashMap<String, Boolean> testResult = new HashMap<>();
        for (TestCase testCase : testCases) {
            testResult.put(testCase.getName(), testCase.isPluginInterfaceTestSuccess());
        }
        return testResult;
    }

}

