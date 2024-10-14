package com.alipay.antchain.bridge.plugintestrunner.chainmanager;


import com.alipay.antchain.bridge.plugintestrunner.chainmanager.chainmaker.ChainMakerChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.eos.EosChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.eth.EthChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.fabric.FabricChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.fiscobcos.FiscoBcosChainManager;
import com.alipay.antchain.bridge.plugintestrunner.chainmanager.hyperchain.HyperchainChainManager;
import com.alipay.antchain.bridge.plugintestrunner.config.ChainConfig;
import com.alipay.antchain.bridge.plugintestrunner.config.ChainProduct;
import com.alipay.antchain.bridge.plugintestrunner.exception.ChainManagerException.*;

public class IChainManagerFactory {
    // 根据 product 创建 IChainManager
    public static IChainManager createIChainManager(String chainProduct) throws Exception {
        ChainProduct cp = ChainProduct.fromValue(chainProduct);
        switch (cp) {
            case ETH:
                return new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile, ChainConfig.EthChainConfig.gasPrice, ChainConfig.EthChainConfig.gasLimit);
            case EOS:
                return new EosChainManager(ChainConfig.EosChainConfig.getHttpUrl(), ChainConfig.EosChainConfig.privateKeyFile);
            case BCOS:
                return new FiscoBcosChainManager(ChainConfig.FiscoBcosChainConfig.confDir);
            case FABRIC:
                  return new FabricChainManager(ChainConfig.FabricChainConfig.confFile);
            case CHAINMAKER:
                return new ChainMakerChainManager(ChainConfig.ChainMakerChainConfig.confFile);
            case HYPERCHAIN:
                return new HyperchainChainManager(ChainConfig.HyperChainChainConfig.getHttpUrl());
            default:
                throw new ChainNotSupportedException("Unsupported chain product: " + chainProduct);
        }
    }
}
