package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.IBBCServiceClient;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import lombok.Getter;
import lombok.Synchronized;
import org.springframework.stereotype.Component;

@Component
@Getter
public class BlockchainClientPool {

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private IBBCPluginManager bbcPluginManager;

    private final Map<String, AbstractBlockchainClient> clients = MapUtil.newConcurrentHashMap();

    public AbstractBlockchainClient createClient(String blockchainProduct, String blockchainId) {

        String key = blockchainProduct + "_" + blockchainId;

        if (clients.containsKey(key)) {
            return clients.get(key);
        }

        BlockchainMeta blockchainMeta = blockchainRepository.getBlockchainMeta(blockchainProduct, blockchainId);
        if (ObjectUtil.isNull(blockchainMeta)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    "none meta found for blockchain {}-{}",
                    blockchainProduct, blockchainId
            );
        }

        return createClient(blockchainMeta);
    }

    @Synchronized
    public AbstractBlockchainClient createClient(BlockchainMeta chainMeta) {
        String clientKey = chainMeta.getMetaKey();
        if (clients.containsKey(clientKey)) {
            AbstractBlockchainClient client = clients.get(clientKey);
            client.setBlockchainMeta(chainMeta);
            return client;
        }

        String domain = blockchainRepository.getBlockchainDomain(
                chainMeta.getProduct(),
                chainMeta.getBlockchainId()
        );
        if (StrUtil.isEmpty(domain)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_CLIENT_INIT_ERROR,
                    "none domain found for {}",
                    clientKey
            );
        }

        IBBCServiceClient bbcClient = bbcPluginManager.createBBCClient(
                chainMeta.getProperties().getPluginServerId(),
                chainMeta.getProduct(),
                domain
        );
        HeteroBlockchainClient heteroBcClient = new HeteroBlockchainClient(
                bbcClient,
                chainMeta
        );
        if (!heteroBcClient.start()) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_CLIENT_INIT_ERROR,
                    "Blockchain client start failed for product {} and domain {}",
                    heteroBcClient.getBlockchainMeta().getProduct(),
                    heteroBcClient.getDomain()
            );
        }
        clients.put(clientKey, heteroBcClient);

        return heteroBcClient;
    }

    public AbstractBlockchainClient getClient(String product, String blockchainId) {
        String key = BlockchainMeta.createMetaKey(product, blockchainId);
        if (clients.containsKey(key)) {
            return clients.get(key);
        }
        return null;
    }

    public void deleteClient(String product, String blockchainId) {
        clients.remove(BlockchainMeta.createMetaKey(product, blockchainId));
    }

    public synchronized List<String> getAllClient() {
        return ListUtil.toList(clients.keySet());
    }

    @Synchronized
    public boolean hasClient(String blockchainProduct, String blockchainId) {
        String key = blockchainProduct + "_" + blockchainId;
        return clients.containsKey(key);
    }

    @Synchronized
    public void shutdownClient(String blockchainProduct, String blockchainId) {

        String key = blockchainProduct + "_" + blockchainId;

        if (!clients.containsKey(key)) {
            return;
        }

        clients.get(key).shutdown();
        clients.remove(key);
    }
}
