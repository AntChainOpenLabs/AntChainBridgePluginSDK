package org.example.plugintestrunner.chainmanager;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

@Getter
public class ChainMakerChainManager extends IChainManager{

//    @Getter
//    private ChainClient chainClient;
//    private ChainManager chainManager;
    private String config;


    public ChainMakerChainManager(String sdk_config) throws Exception {
        this.config = ChainMakerConfigUtil.parseChainConfig(sdk_config);
    }

    @Override
    public boolean isConnected() throws ExecutionException, InterruptedException, IOException {
        return true;
    }

    @Override
    public void close() {

    }
}
