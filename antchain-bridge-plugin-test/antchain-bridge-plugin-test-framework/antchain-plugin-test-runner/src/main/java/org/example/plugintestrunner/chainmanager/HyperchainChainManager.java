package org.example.plugintestrunner.chainmanager;

import cn.hutool.core.util.StrUtil;
import cn.hyperchain.sdk.account.Account;
import cn.hyperchain.sdk.account.Algo;
import cn.hyperchain.sdk.provider.DefaultHttpProvider;
import cn.hyperchain.sdk.provider.ProviderManager;
import cn.hyperchain.sdk.service.*;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class HyperchainChainManager extends IChainManager {

    DefaultHttpProvider defaultHttpProvider;
    ProviderManager providerManager;
    ContractService contractService;
    AccountService accountService;
    TxService txService;
    BlockService blockService;
    MQService mqService;
    Account account;

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
//        if (StrUtil.isNotEmpty(config.getAccountJson()) && config.getPassword() != null) {
//            account = Account.fromAccountJson(config.getAccountJson(), config.getPassword());
//        } else {
//            account = accountService.genAccount(Algo.SMRAW);
//        }
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
