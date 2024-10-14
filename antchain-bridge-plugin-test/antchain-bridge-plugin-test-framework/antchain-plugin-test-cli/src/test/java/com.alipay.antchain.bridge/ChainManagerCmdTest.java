package com.alipay.antchain.bridge;

import com.alipay.antchain.bridge.plugintestrunner.exception.TestCaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;

public class ChainManagerCmdTest {

    private CommandLine commandLine;

    private final String PRODUCT_NAME = "simple-ethereum";

    @BeforeEach
    public void init() throws IOException, TestCaseException {
        App app = new App();
        app.init();
        commandLine = new CommandLine(app);
    }

    @Test
    public void testStartAndStop() throws IOException {
        String[] startArgs = createArgs("chain-manager start -p " + PRODUCT_NAME);
        String[] stopArgs = createArgs("chain-manager stop -p " + PRODUCT_NAME);
        commandLine.execute(startArgs);
        commandLine.execute(stopArgs);
    }

    // 将 String 转换为 String[]
    private String[] createArgs(String command) {
        return command.split(" ");
    }
}
