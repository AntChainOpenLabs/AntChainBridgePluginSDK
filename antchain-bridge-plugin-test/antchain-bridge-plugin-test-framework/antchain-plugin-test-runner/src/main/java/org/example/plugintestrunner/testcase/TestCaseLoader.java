package org.example.plugintestrunner.testcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.plugintestrunner.exception.TestCaseLoaderException;

import java.io.File;
import java.io.InputStream;

public class TestCaseLoader {

    public static TestCaseContainer loadTestCasesFromFile(String filePath) throws TestCaseLoaderException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 读取文件并将其转换为 Plugin 列表
            TestCaseContainer testCaseContainer = mapper.readValue(new File(filePath), TestCaseContainer.class);
            // 初始化
            testCaseContainer.init();
            return testCaseContainer;
        } catch (Exception e) {
            throw new TestCaseLoaderException("Failed to load test cases from file " + filePath, e);
        }
    }

    public static TestCaseContainer loadTestCasesFromResource(String resourcePath) throws TestCaseLoaderException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 通过 ClassLoader 获取资源的输入流
            InputStream resourceStream = TestCaseLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                throw new TestCaseLoaderException("Resource not found: " + resourcePath);
            }

            // 将输入流转换为 TestCaseContainer 对象
            TestCaseContainer testCaseContainer = mapper.readValue(resourceStream, TestCaseContainer.class);
            // 初始化
            testCaseContainer.init();
            return testCaseContainer;
        } catch (Exception e) {
            throw new TestCaseLoaderException("Failed to load test cases from resource " + resourcePath, e);
        }
    }
}