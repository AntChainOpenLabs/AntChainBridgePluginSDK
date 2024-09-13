package org.example.plugintestrunner.chainmanager;

import one.block.eosiojavarpcprovider.error.EosioJavaRpcProviderInitializerError;
import org.chainmaker.sdk.ChainClientException;
import org.chainmaker.sdk.RpcServiceClientException;
import org.chainmaker.sdk.crypto.ChainMakerCryptoSuiteException;
import org.chainmaker.sdk.utils.UtilsException;
import org.example.plugintestrunner.config.ChainConfig;
import org.example.plugintestrunner.config.ChainProduct;
import org.example.plugintestrunner.exception.ChainManagerException;
import org.example.plugintestrunner.exception.ChainManagerException.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

public class IChainManagerFactory {
    // 根据 product 创建 IChainManager
    public static IChainManager createIChainManager(String chainProduct) throws IOException, ChainManagerException, EosioJavaRpcProviderInitializerError, UtilsException, ChainClientException, RpcServiceClientException, ChainMakerCryptoSuiteException, InvalidArgumentException, NoSuchAlgorithmException, InvalidKeySpecException, CryptoException, NoSuchProviderException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, TransactionException, ProposalException {
        ChainProduct cp = ChainProduct.fromValue(chainProduct);
        switch (cp) {
            case ETH:
            case TESTCHAIN:
                return new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile);
            // TODO add more chain products
            case EOS:
                return new EosChainManager(ChainConfig.EosChainConfig.getHttpUrl());
            case BCOS:
                return new FiscoBcosChainManager(ChainConfig.FiscoBcosChainConfig.confFile);
            case FABRIC:
                return new FabricChainManager(ChainConfig.FabricChainConfig.privateKeyFile, ChainConfig.FabricChainConfig.certFile, ChainConfig.FabricChainConfig.peerTlsCertFile, ChainConfig.FabricChainConfig.ordererTlsCertFile);
            case CHAINMAKER:
                return new ChainMakerChainManager(ChainConfig.ChainMakerChainConfig.confFile);
            case HYPERCHAIN:
                return new HyperchainChainManager(ChainConfig.HyperChainChainConfig.getHttpUrl());
            default:
                throw new ChainNotSupportedException("Unsupported chain product: " + chainProduct);
        }
    }
}
