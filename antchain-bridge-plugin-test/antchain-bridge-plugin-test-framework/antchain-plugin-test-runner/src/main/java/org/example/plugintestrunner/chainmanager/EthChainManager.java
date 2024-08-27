package org.example.plugintestrunner.chainmanager;

import lombok.Getter;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.ChainManagerException.*;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlock;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.io.File;


@Getter
public class EthChainManager extends IChainManager {
    private final Web3j web3j;


    public EthChainManager(String httpUrl) {
        super(httpUrl);
        this.web3j = Web3j.build(new HttpService(httpUrl));
    }

    public EthChainManager(String httpUrl, String password, String walletDirectory) throws IOException, ChainManagerException {
        super(httpUrl);
        this.web3j = Web3j.build(new HttpService(httpUrl));
        if (!isConnected()) {
            throw new ChainManagerConstructionException("Max attempts to connect to the Ethereum node exceeded");
        }
        try {
            setAccount(password, walletDirectory);
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

    public void setAccount(String password, String walletDirectory) throws InvalidAlgorithmParameterException, CipherException, IOException, NoSuchAlgorithmException, NoSuchProviderException {
        String walletFileName = WalletUtils.generateNewWalletFile(password, new File(walletDirectory), false);
        Credentials credentials =  WalletUtils.loadCredentials(password, Paths.get(walletDirectory, walletFileName).toFile());
        privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);
    }

    public void setGasLimit() throws IOException {
        gasLimit = web3j.ethGasPrice().send().getGasPrice().toString();
    }

    public void setGasPrice() throws IOException {
        EthBlock latestBlock = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
        gasPrice = latestBlock.getBlock().getGasLimit().toString();
    }

    public void close() {
        web3j.shutdown();
    }

}
