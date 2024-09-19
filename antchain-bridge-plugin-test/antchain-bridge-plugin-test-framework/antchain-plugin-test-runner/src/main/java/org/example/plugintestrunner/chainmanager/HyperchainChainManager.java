package org.example.plugintestrunner.chainmanager;

import cn.hyperchain.sdk.account.Account;
import cn.hyperchain.sdk.account.Algo;
import cn.hyperchain.sdk.provider.DefaultHttpProvider;
import cn.hyperchain.sdk.provider.ProviderManager;
import cn.hyperchain.sdk.service.*;
import lombok.Getter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Getter
public class HyperchainChainManager extends IChainManager {

    DefaultHttpProvider defaultHttpProvider;
    ProviderManager providerManager;
    ContractService contractService;
    AccountService accountService;
    TxService txService;
    BlockService blockService;
    MQService mqService;
    Account account;

    private String config;

    public HyperchainChainManager(String url) {
        // 2. build provider manager
        defaultHttpProvider = new DefaultHttpProvider.Builder().setUrl(url).build();
        providerManager = ProviderManager.createManager(defaultHttpProvider);
        // 3. build service
        contractService = ServiceManager.getContractService(providerManager);
        accountService = ServiceManager.getAccountService(providerManager);
        txService = ServiceManager.getTxService(providerManager);
        blockService = ServiceManager.getBlockService(providerManager);
        mqService = ServiceManager.getMQService(providerManager);
        // 4. create account
        String password = "";
        account = accountService.genAccount(Algo.SMRAW, password);

        config = String.format(
                "{\"url\": \"%s\", \"accountJson\": {\"address\": \"%s\", \"publicKey\": \"%s\", \"privateKey\": \"%s\", \"version\": \"%s\", \"algo\": \"%s\"}, \"password\": \"%s\"}",
                url, account.getAddress(), account.getPublicKey(), account.getPrivateKey(), account.getVersion().getV(), account.getAlgo().getAlgo(), password
        );
    }

    @Override
    public boolean isConnected() throws ExecutionException, InterruptedException, IOException {
        try {
            System.out.println(blockService.getLatestBlock().getId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {

    }
}
