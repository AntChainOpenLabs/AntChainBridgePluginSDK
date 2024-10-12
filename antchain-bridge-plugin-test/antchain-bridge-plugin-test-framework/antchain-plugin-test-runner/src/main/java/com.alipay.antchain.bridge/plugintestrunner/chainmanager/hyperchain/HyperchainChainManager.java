package com.alipay.antchain.bridge.plugintestrunner.chainmanager.hyperchain;

import cn.hyperchain.sdk.account.Account;
import cn.hyperchain.sdk.account.Algo;
import cn.hyperchain.sdk.provider.DefaultHttpProvider;
import cn.hyperchain.sdk.provider.ProviderManager;
import cn.hyperchain.sdk.service.*;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.IChainManager;
import lombok.Getter;

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

    public HyperchainChainManager(String url) {
        // 2. build provider manager
        this.defaultHttpProvider = new DefaultHttpProvider.Builder().setUrl(url).build();
        this.providerManager = ProviderManager.createManager(this.defaultHttpProvider);
        // 3. build service
        this.contractService = ServiceManager.getContractService(this.providerManager);
        this.accountService = ServiceManager.getAccountService(this.providerManager);
        this.txService = ServiceManager.getTxService(this.providerManager);
        this.blockService = ServiceManager.getBlockService(this.providerManager);
        this.mqService = ServiceManager.getMQService(this.providerManager);
        // 4. create account
        String password = "";
        this.account = this.accountService.genAccount(Algo.SMRAW, password);

        this.config = String.format(
                "{\"url\": \"%s\", \"accountJson\": {\"address\": \"%s\", \"publicKey\": \"%s\", \"privateKey\": \"%s\", \"version\": \"%s\", \"algo\": \"%s\"}, \"password\": \"%s\"}",
                url, this.account.getAddress(), this.account.getPublicKey(), this.account.getPrivateKey(), this.account.getVersion().getV(), this.account.getAlgo().getAlgo(), password
        );
    }

    @Override
    public void close() {
    }
}
