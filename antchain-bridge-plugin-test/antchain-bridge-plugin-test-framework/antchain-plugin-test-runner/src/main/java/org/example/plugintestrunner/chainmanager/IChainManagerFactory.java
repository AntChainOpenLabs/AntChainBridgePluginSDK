package org.example.plugintestrunner.chainmanager;

import org.example.plugintestrunner.config.ChainConfig;
import org.example.plugintestrunner.config.ChainProduct;
import org.example.plugintestrunner.exception.ChainManagerException.*;

public class IChainManagerFactory {
    // 根据 product 创建 IChainManager
    public static IChainManager createIChainManager(String chainProduct) throws Exception {
        ChainProduct cp = ChainProduct.fromValue(chainProduct);
        switch (cp) {
            case ETH:
            case TESTCHAIN:
                return new EthChainManager(ChainConfig.EthChainConfig.getHttpUrl(), ChainConfig.EthChainConfig.privateKeyFile);
            // TODO add more chain products
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
