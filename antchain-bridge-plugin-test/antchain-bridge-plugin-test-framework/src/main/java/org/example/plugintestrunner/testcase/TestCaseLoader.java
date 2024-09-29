package org.example.plugintestrunner.testcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.plugintestrunner.exception.TestCaseLoaderException;

import java.io.File;
import java.io.IOException;

public class TestCaseLoader {

    public static TestCaseContainer loadTestCasesFromFile(String filename) throws TestCaseLoaderException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 读取文件并将其转换为 Plugin 列表
            TestCaseContainer testCaseContainer = mapper.readValue(new File(filename), TestCaseContainer.class);
            // 初始化
            testCaseContainer.init();
            return testCaseContainer;
        } catch (Exception e) {
            throw new TestCaseLoaderException("Failed to load test cases from file " + filename, e);
        }
    }
}