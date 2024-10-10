package org.example.plugintestrunner.chainmanager.eth;

import lombok.Getter;
import lombok.Setter;
import org.example.plugintestrunner.chainmanager.IChainManager;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.ChainManagerException.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.io.*;


@Getter
@Setter
public class EthChainManager extends IChainManager {

    private String httpUrl;
    private String privateKey;
    private String gasPrice;
    private String gasLimit;
    private final Web3j web3j;

    public EthChainManager(String httpUrl, String privateKeyFile) throws ChainManagerException {
        this.httpUrl = httpUrl;
        this.web3j = Web3j.build(new HttpService(httpUrl));
        try {
            setPrivateKey(privateKeyFile);
            setGasLimit();
            setGasPrice();
        } catch (Exception e) {
            throw new ChainManagerConstructionException("Failed to set account or gas limit or gas price", e);
        }
        // 构造用于插件测试的配置信息
        this.config = String.format("{\"gasLimit\":%s,\"gasPrice\":%s,\"privateKey\":\"%s\",\"url\":\"%s\"}",
                this.gasLimit, this.gasPrice, this.privateKey, httpUrl);
    }

    // 通过文件获取
    public void setPrivateKey(String privateKeyFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(privateKeyFile));
        this.privateKey = br.readLine();
    }

    // 通过 web3j 获取
    public void setGasPrice() throws IOException {
        this.gasPrice = this.web3j.ethGasPrice().send().getGasPrice().toString();
    }

    // 通过 web3j 获取
    public void setGasLimit() throws IOException {
        EthBlock latestBlock = this.web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
        this.gasLimit = latestBlock.getBlock().getGasLimit().toString();
    }

    @Override
    public void close() {
        this.web3j.shutdown();
    }
}
