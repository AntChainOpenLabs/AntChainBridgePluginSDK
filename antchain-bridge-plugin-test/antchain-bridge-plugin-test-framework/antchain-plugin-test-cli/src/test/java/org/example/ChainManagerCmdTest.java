package org.example;

import org.example.plugintestrunner.chainmanager.EthChainManager;
import org.example.plugintestrunner.exception.TestCaseLoaderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;

public class ChainManagerCmdTest {

    private CommandLine commandLine;

    private final String PRODUCT_NAME = "simple-ethereum";

    private final String HTTP_URL = "http://localhost:8545";

    private EthChainManager ethChainManager;


    @BeforeEach
    public void init() throws IOException, TestCaseLoaderException {
        App app = new App();
        app.init();
        commandLine = new CommandLine(app);
        ethChainManager = new EthChainManager(HTTP_URL);
    }

    @Test
    public void testStartAndStop() throws IOException {
        String[] startArgs = createArgs("chain-manager start -p " + PRODUCT_NAME);
        commandLine.execute(startArgs);
        assert ethChainManager.isConnected();
        String[] stopArgs = createArgs("chain-manager stop -p " + PRODUCT_NAME);
        commandLine.execute(stopArgs);
        try {
            ethChainManager.isConnected();
        } catch (IOException ignored) {
        }
    }

    // 将 String 转换为 String[]
    private String[] createArgs(String command) {
        return command.split(" ");
    }
}
