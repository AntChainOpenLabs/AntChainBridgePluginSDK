package com.alipay.antchain.bridge.plugintestrunner.chainmanager.eth;

import com.alipay.antchain.bridge.plugintestrunner.chainmanager.IChainManager;
import com.alipay.antchain.bridge.plugintestrunner.exception.ChainManagerException;
import com.alipay.antchain.bridge.plugintestrunner.exception.ChainManagerException.*;
import lombok.Getter;
import lombok.Setter;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.io.*;


@Getter
@Setter
public class EthChainManager extends IChainManager {

    private String httpUrl;
    private String privateKey;
    private String gasPrice;
    private String gasLimit;
    private final Web3j web3j;

    public EthChainManager(String httpUrl, String privateKeyFile, String gasPrice, String gasLimit) throws ChainManagerException {
        this.httpUrl = httpUrl;
        this.web3j = Web3j.build(new HttpService(httpUrl));
        try {
            setPrivateKey(privateKeyFile);
            this.gasPrice = gasPrice;
            this.gasLimit = gasLimit;
        } catch (Exception e) {
            throw new ChainManagerConstructionException("Failed to set account or gas limit or gas price", e);
        }
        this.config = String.format("{\"gasLimit\":%s,\"gasPrice\":%s,\"privateKey\":\"%s\",\"url\":\"%s\"}",
                this.gasLimit, this.gasPrice, this.privateKey, httpUrl);
    }

    public void setPrivateKey(String privateKeyFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(privateKeyFile));
        this.privateKey = br.readLine();
    }

    @Override
    public void close() {
        this.web3j.shutdown();
    }
}
