package org.example.plugintestrunner.chainmanager;

import lombok.Getter;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.ChainManagerException.*;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthCoinbase;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;


@Getter
public class EthChainManager extends IChainManager {

    private final Web3j web3j;

    public EthChainManager(String httpUrl) {
        super(httpUrl);
        this.web3j = Web3j.build(new HttpService(httpUrl));
    }

    public EthChainManager(String httpUrl, String privateKeyFile) throws IOException, ChainManagerException {
        super(httpUrl);
        this.web3j = Web3j.build(new HttpService(httpUrl));
        if (!isConnected()) {
            throw new ChainManagerConstructionException("Max attempts to connect to the Ethereum node exceeded");
        }
        try {
            setPrivateKey(privateKeyFile);
            setGasLimit();
            setGasPrice();
        } catch (Exception e) {
            throw new ChainManagerConstructionException("Failed to set account or gas limit or gas price", e);
        }
    }

    public EthChainManager(String httpUrl, String privateKey, String gasPrice, String gasLimit) throws IOException, ChainManagerException {
        super(httpUrl, privateKey, gasPrice, gasLimit);
        this.web3j = Web3j.build(new HttpService(httpUrl));
        if (!isConnected()) {
            throw new ChainManagerConstructionException("Max attempts to connect to the Ethereum node exceeded");
        }
    }


    public boolean isConnected() throws IOException {
        Web3ClientVersion web3ClientVersion;
        try {
            web3ClientVersion = web3j.web3ClientVersion().send();
        } catch (IOException e) {
            throw new IOException("Failed to connect to the Ethereum node", e);
        }
        return web3ClientVersion.getWeb3ClientVersion() != null;
    }

    // 通过文件获取
    public void setPrivateKey(String privateKeyFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(privateKeyFile));
        privateKey = br.readLine();
    }

    // 通过 web3j 获取
    public void setGasLimit() throws IOException {
        gasLimit = web3j.ethGasPrice().send().getGasPrice().toString();
    }

    public long getGasLimitLong() throws IOException {
        BigInteger gasPrice1 = web3j.ethGasPrice().send().getGasPrice();
        return gasPrice1.longValue();
    }

    // 通过 web3j 获取
    public void setGasPrice() throws IOException {
        EthBlock latestBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
        gasPrice = latestBlock.getBlock().getGasLimit().toString();
    }

    public long getGasPriceLong() throws IOException {
        EthBlock latestBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
        return latestBlock.getBlock().getGasLimit().longValue();
    }

    public void close() {
        web3j.shutdown();
    }

}
