package org.example.plugintestrunner.chainmanager;

import lombok.Getter;
import lombok.Setter;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.ExecutionException;

@Getter
public abstract class IChainManager {
    String httpUrl;
    String privateKey;
    String gasPrice;
    String gasLimit;

    public IChainManager(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public IChainManager(String httpUrl, String privateKey, String gasPrice, String gasLimit) {
        this.httpUrl = httpUrl;
        this.privateKey = privateKey;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
    }

    // 测试连接
    public abstract boolean isConnected() throws ExecutionException, InterruptedException, IOException;

    // 设置新账户
    public abstract void setPrivateKey(String privateKeyFile) throws IOException;

    // 设置 gasLimit
    public abstract void setGasLimit() throws IOException;

    // 设置 gasPrice
    public abstract void setGasPrice() throws IOException;

    public abstract void close();

}
