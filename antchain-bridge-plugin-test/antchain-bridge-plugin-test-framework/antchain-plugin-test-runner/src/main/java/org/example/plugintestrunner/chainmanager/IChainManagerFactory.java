package org.example.plugintestrunner.chainmanager;

import org.example.plugintestrunner.chainmanager.chainmaker.ChainMakerChainManager;
import org.example.plugintestrunner.chainmanager.eos.EosChainManager;
import org.example.plugintestrunner.chainmanager.eth.EthChainManager;
import org.example.plugintestrunner.chainmanager.fabric.FabricChainManager;
import org.example.plugintestrunner.chainmanager.fiscobcos.FiscoBcosChainManager;
import org.example.plugintestrunner.chainmanager.hyperchain.HyperchainChainManager;
import org.example.plugintestrunner.config.ChainConfig;
import org.example.plugintestrunner.config.ChainProduct;
import org.example.plugintestrunner.exception.ChainManagerException.*;

public class IChainManagerFactory {
    // 根据 product 创建 IChainManager
    public static IChainManager createIChainManager(String chainProduct) throws Exception {
        ChainProduct cp = ChainProduct.fromValue(chainProduct);
        switch (cp) {
            case ETH:
                return new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile);
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
