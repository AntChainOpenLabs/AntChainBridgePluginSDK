package com.alipay.antchain.bridge.plugintestrunner.chainmanager.eos;

import com.alipay.antchain.bridge.plugintestrunner.chainmanager.IChainManager;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Getter
@Setter
public class EosChainManager extends IChainManager {

    private String url;
    private String userPriKey;
    private String amContractAddressDeployed;
    private String sdpContractAddressDeployed;
    private String userName;
    private boolean waitUtilTxIrreversible;

    public EosChainManager(String url, String private_key_file) throws IOException {
        this.url = url;
        BufferedReader br = new BufferedReader(new FileReader(private_key_file));
        this.userPriKey = br.readLine();
        br.close();
        this.amContractAddressDeployed = "am";
        this.sdpContractAddressDeployed = "sdp";
        this.userName = "test";
        this.waitUtilTxIrreversible = true;
        this.config = String.format("{\"url\":\"%s\",\"userPriKey\":\"%s\",\"amContractAddressDeployed\":\"%s\",\"sdpContractAddressDeployed\":\"%s\",\"userName\":\"%s\",\"waitUtilTxIrreversible\":%b}",
                url, userPriKey, amContractAddressDeployed, sdpContractAddressDeployed, userName, waitUtilTxIrreversible);
    }

    @Override
    public void close() {

    }
}
