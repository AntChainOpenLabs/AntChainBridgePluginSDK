package org.example.plugintestrunner.testcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.plugintestrunner.exception.TestCaseException;

import java.io.File;
import java.io.InputStream;

public class TestCaseLoader {

    public static TestCaseContainer loadTestCasesFromFile(String filePath) throws TestCaseException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 读取文件并将其转换为 Plugin 列表
            TestCaseContainer testCaseContainer = mapper.readValue(new File(filePath), TestCaseContainer.class);
            // 初始化
            testCaseContainer.init();
            return testCaseContainer;
        } catch (Exception e) {
            throw new TestCaseException("Failed to load test cases from file " + filePath, e);
        }
    }
}