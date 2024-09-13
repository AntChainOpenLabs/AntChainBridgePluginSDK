package org.example.plugintestrunner.chainmanager;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.BlockNumber;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FiscoBcosChainManager extends IChainManager{

    private final BcosSDK sdk;
    private final Client client;
    private static final String GROUP_ID = "group0";

    public FiscoBcosChainManager(String confFile) {
        Path path = Paths.get(confFile).toAbsolutePath();
        sdk =  BcosSDK.build(path.toString());
        client = sdk.getClient(GROUP_ID);
    }

    @Override
    public boolean isConnected() {
        try {
            client.getBlockNumber();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {

    }
}
