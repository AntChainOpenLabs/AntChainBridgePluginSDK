/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.relayer.core.manager.network;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.CrossChainChannelDO;
import com.alipay.antchain.bridge.relayer.commons.constant.CrossChainChannelStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainRouterSyncStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.types.network.IRelayerClientPool;
import com.alipay.antchain.bridge.relayer.core.types.network.RelayerClient;
import com.alipay.antchain.bridge.relayer.core.types.network.request.RelayerRequest;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.IRelayerNetworkRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.ISystemConfigRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Getter
@Component
public class RelayerNetworkManager implements IRelayerNetworkManager {

    @Value("${relayer.network.node.crosschain_cert_path:null}")
    private String relayerCrossChainCertPath;

    @Value("${relayer.network.node.private_key_path}")
    private String relayerPrivateKeyPath;

    @Value("${relayer.network.node.server.mode:https}")
    private String localNodeServerMode;

    @Resource
    private IRelayerNetworkRepository relayerNetworkRepository;

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @Resource
    private IRelayerCredentialManager relayerCredentialManager;

    @Resource
    private IBCDNSManager bcdnsManager;

    @Resource
    private IRelayerClientPool relayerClientPool;

    @Resource
    private Cache<String, RelayerNodeInfo> relayerNodeInfoCache;

    @Override
    public RelayerNodeInfo getRelayerNodeInfo() {
        try {
            if (relayerNodeInfoCache.containsKey(relayerCredentialManager.getLocalNodeId())) {
                return relayerNodeInfoCache.get(relayerCredentialManager.getLocalNodeId(), false);
            }
            RelayerNodeInfo localNodeInfo = new RelayerNodeInfo(
                    relayerCredentialManager.getLocalNodeId(),
                    relayerCredentialManager.getLocalRelayerCertificate(),
                    relayerCredentialManager.getLocalRelayerCredentialSubject(),
                    relayerCredentialManager.getLocalNodeSigAlgo(),
                    systemConfigRepository.getLocalEndpoints(),
                    blockchainRepository.getBlockchainDomainsByState(BlockchainStateEnum.RUNNING)
            );
            localNodeInfo.getProperties().setRelayerReqVersion(RelayerRequest.CURR_REQ_VERSION);
            relayerNodeInfoCache.put(relayerCredentialManager.getLocalNodeId(), localNodeInfo);
            return localNodeInfo;
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "failed to get local relayer node info",
                    e
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RelayerNodeInfo getRelayerNodeInfoWithContent() {
        RelayerNodeInfo nodeInfo = getRelayerNodeInfo();

        Map<String, RelayerBlockchainInfo> blockchainInfoMap = nodeInfo.getDomains().stream()
                .map(this::getRelayerBlockchainInfo)
                .collect(Collectors.toMap(
                        info -> info.getDomainCert().getDomain(),
                        info -> info
                ));
        Map<String, AbstractCrossChainCertificate> trustRootCertChain = blockchainInfoMap.values().stream()
                .map(info -> this.bcdnsManager.getTrustRootCertChain(info.getDomainCert().getDomainSpace()))
                .reduce(
                        (map1, map2) -> {
                            map1.putAll(map2);
                            return map1;
                        }
                ).orElse(MapUtil.newHashMap());

        RelayerBlockchainContent content = new RelayerBlockchainContent(
                blockchainInfoMap,
                trustRootCertChain
        );

        nodeInfo.setRelayerBlockchainContent(content);

        return nodeInfo;
    }

    @Override
    public RelayerBlockchainInfo getRelayerBlockchainInfo(String domain) {
        DomainCertWrapper domainCertWrapper = blockchainRepository.getDomainCert(domain);
        if (ObjectUtil.isNull(domainCertWrapper)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "none domain cert found for {}", domain
            );
        }

        List<String> domainSpaceChain = bcdnsManager.getDomainSpaceChain(domainCertWrapper.getDomainSpace());
        if (ObjectUtil.isEmpty(domainSpaceChain)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "none domain space chain found for {}", domainCertWrapper.getDomainSpace()
            );
        }

        BlockchainMeta blockchainMeta = blockchainRepository.getBlockchainMeta(
                domainCertWrapper.getBlockchainProduct(),
                domainCertWrapper.getBlockchainId()
        );
        if (ObjectUtil.isEmpty(blockchainMeta)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "none blockchain meta found for {}", domain
            );
        }

        return new RelayerBlockchainInfo(
                domainCertWrapper,
                domainSpaceChain,
                blockchainMeta.getProperties().getAmClientContractAddress()
        );
    }

    @Override
    public void addRelayerNode(RelayerNodeInfo nodeInfo) {
        log.info("add relayer node {} with endpoints {}", nodeInfo.getNodeId(), StrUtil.join(StrUtil.COMMA, nodeInfo.getEndpoints()));

        try {
            if (relayerNetworkRepository.hasRelayerNode(nodeInfo.getNodeId())) {
                log.warn("relayer node {} already exists", nodeInfo.getNodeId());
                return;
            }
            if (ObjectUtil.isEmpty(nodeInfo.getEndpoints())) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                        "relayer info not enough"
                );
            }

            // 如果公钥以及domain信息没设置，则远程请求补充
            if (ObjectUtil.isNull(nodeInfo.getRelayerCrossChainCertificate())) {
                RelayerClient relayerClient = relayerClientPool.getRelayerClient(nodeInfo, null);
                if (ObjectUtil.isNull(relayerClient)) {
                    throw new AntChainBridgeRelayerException(
                            RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                            "failed to create relayer client for relayer {} with endpoint {}",
                            nodeInfo.getNodeId(), StrUtil.join(StrUtil.COMMA, nodeInfo.getEndpoints())
                    );
                }

                RelayerNodeInfo relayerNodeInfo = relayerClient.getRelayerNodeInfo();
                if (ObjectUtil.isNull(relayerNodeInfo)) {
                    throw new RuntimeException("null relayer node info from remote relayer");
                }

                nodeInfo.setRelayerCrossChainCertificate(relayerNodeInfo.getRelayerCrossChainCertificate());
                nodeInfo.setRelayerCertId(relayerNodeInfo.getRelayerCrossChainCertificate().getId());
                nodeInfo.setRelayerCredentialSubject(relayerNodeInfo.getRelayerCredentialSubject());
                relayerNodeInfo.getDomains().forEach(
                        domain -> {
                            if (!nodeInfo.getDomains().contains(domain)) {
                                nodeInfo.addDomainIfNotExist(domain);
                            }
                        }
                );
            }

            relayerNetworkRepository.addRelayerNode(nodeInfo);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to add relayer {} with endpoint {}",
                    nodeInfo.getNodeId(), StrUtil.join(StrUtil.COMMA, nodeInfo.getEndpoints())
            );
        }
    }

    @Override
    public void addRelayerNodeWithoutDomainInfo(RelayerNodeInfo nodeInfo) {
        try {
            if (relayerNetworkRepository.hasRelayerNode(nodeInfo.getNodeId())) {
                log.warn("relayer node {} already exist", nodeInfo.getNodeId());
                return;
            }
            List<String> domains = nodeInfo.getDomains();
            RelayerBlockchainContent relayerBlockchainContent = nodeInfo.getRelayerBlockchainContent();

            nodeInfo.setDomains(ListUtil.toList());
            nodeInfo.setRelayerBlockchainContent(null);

            relayerNetworkRepository.addRelayerNode(nodeInfo);

            nodeInfo.setDomains(domains);
            nodeInfo.setRelayerBlockchainContent(relayerBlockchainContent);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to add relayer {} without domain stuff",
                    nodeInfo.getNodeId()
            );
        }
    }

    @Override
    public void addRelayerNodeProperty(String nodeId, String key, String value) {
        try {
            relayerNetworkRepository.updateRelayerNodeProperty(nodeId, key, value);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to add relayer property {} - {}",
                    nodeId, key, value
            );
        }
    }

    @Override
    public RelayerNodeInfo getRelayerNode(String nodeId, boolean lock) {
        return relayerNetworkRepository.getRelayerNode(nodeId, lock);
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void syncRelayerNode(String networkId, String nodeId) {
        log.info("begin sync relayer node {} in network {}", nodeId, networkId);

        try {
            RelayerNodeInfo relayerNode = relayerNetworkRepository.getRelayerNode(nodeId, true);
            if (null == relayerNode) {
                throw new RuntimeException(StrUtil.format("relayer {} not exist", nodeId));
            }

            log.info("relayer node {} has {} domain", nodeId, relayerNode.getDomains().size());

            validateAndSaveBlockchainContent(
                    networkId,
                    relayerNode,
                    Assert.notNull(
                            relayerClientPool.getRelayerClient(relayerNode, null).getRelayerBlockchainContent(),
                            "null blockchain content from relayer {}",
                            nodeId
                    ),
                    true
            );
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to sync relayer node {}",
                    nodeId
            );
        }
    }

    @Override
    public void validateAndSaveBlockchainContent(
            String networkId,
            RelayerNodeInfo relayerNodeInfo,
            RelayerBlockchainContent relayerBlockchainContent,
            boolean ifNewContent
    ) {
        RelayerBlockchainContent.ValidationResult validationResult = relayerBlockchainContent.validate(
                bcdnsManager.getTrustRootCertForRootDomain()
        );
        validationResult.getBlockchainInfoMapValidated().forEach(
                (key, value) -> {
                    try {
                        processRelayerBlockchainInfo(
                                networkId,
                                key,
                                relayerNodeInfo,
                                value
                        );
                    } catch (Exception e) {
                        log.error("failed process blockchain info for {} from relayer {}", key, relayerNodeInfo.getNodeId(), e);
                    }
                    log.info("sync domain {} success from relayer {}", key, relayerNodeInfo.getNodeId());
                }
        );
        if (ifNewContent || ObjectUtil.isNull(relayerNodeInfo.getRelayerBlockchainContent())) {
            relayerNodeInfo.setRelayerBlockchainContent(relayerBlockchainContent);
        } else {
            relayerNodeInfo.getRelayerBlockchainContent().addRelayerBlockchainContent(relayerBlockchainContent);
        }
        relayerNetworkRepository.updateRelayerNode(relayerNodeInfo);
        bcdnsManager.saveDomainSpaceCerts(validationResult.getDomainSpaceValidated());
    }

    @Override
    public RelayerNetwork findNetworkByDomainName(String domainName) {
        return relayerNetworkRepository.getRelayerNetworkByDomain(domainName);
    }

    @Override
    public RelayerNetwork.DomainRouterItem findNetworkItemByDomainName(String domainName) {
        return relayerNetworkRepository.getNetworkItem(domainName);
    }

    @Override
    public String findRemoteRelayer(String receiverDomain) {
        RelayerNetwork.DomainRouterItem relayerNetworkItem = findNetworkItemByDomainName(receiverDomain);
        if (ObjectUtil.isNull(relayerNetworkItem)) {
            log.debug("can't find receiver domain {} in all network from local data", receiverDomain);
            return null;
        }
        if (relayerNetworkItem.getSyncState() != DomainRouterSyncStateEnum.SYNC) {
            log.warn("receiver domain {} router existed but not on state SYNC.", receiverDomain);
            return null;
        }
        return relayerNetworkItem.getNodeId();
    }

    @Override
    public void addRelayerNetworkItem(String networkId, String domain, String nodeId, DomainRouterSyncStateEnum syncState) {
        try {
            relayerNetworkRepository.addNetworkItem(
                    networkId,
                    domain,
                    nodeId,
                    syncState
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_ADD_DOMAIN_ROUTER_ERROR,
                    e,
                    "failed to add network item (network_id: {}, domain: {}, node_id: {}, state: {})",
                    networkId, domain, nodeId, syncState.getCode()
            );
        }
    }

    @Override
    public boolean deleteRelayerNetworkItem(String domain, String nodeId) {
        try {
            relayerNetworkRepository.deleteNetworkItem(domain, nodeId);
        } catch (Exception e) {
            log.error("failed to delete relayer network (domain: {}, node_id: {})", domain, nodeId, e);
            return false;
        }
        return true;
    }

    @Override
    public RelayerNetwork getRelayerNetwork(String networkId) {
        return relayerNetworkRepository.getRelayerNetwork(networkId);
    }

    @Override
    public RelayerNodeInfo getRelayerNodeInfoForDomain(String domain) {
        String remoteNodeId = relayerNetworkRepository.getRelayerNodeIdForDomain(domain);
        if (StrUtil.isEmpty(remoteNodeId)) {
            return relayerNetworkRepository.getRelayerNode(remoteNodeId, false);
        }

        RelayerNodeInfo localRelayerNodeInfo = getRelayerNodeInfo();
        return localRelayerNodeInfo.getDomains().contains(domain) ? localRelayerNodeInfo : null;
    }

    @Override
    public boolean hasRemoteRelayerNodeInfoByCertId(String relayerCertId) {
        return relayerNetworkRepository.hasRelayerNodeByCertId(relayerCertId);
    }

    @Override
    public boolean hasRemoteRelayerNode(String relayerNodeId) {
        return relayerNetworkRepository.hasRelayerNode(relayerNodeId);
    }

    @Override
    public RelayerNodeInfo getRemoteRelayerNodeInfoByCertId(String relayerCertId) {
        return relayerNetworkRepository.getRelayerNodeByCertId(relayerCertId, true);
    }

    @Override
    public void updateRelayerNode(RelayerNodeInfo nodeInfo) {
        relayerNetworkRepository.updateRelayerNode(nodeInfo);
    }

    @Override
    public List<RelayerHealthInfo> healthCheckRelayers() {
        return relayerNetworkRepository.getAllRelayerHealthInfo();
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void createNewCrossChainChannel(String localDomain, String remoteDomain, String relayerNodeId) {
        try {
            if (relayerNetworkRepository.hasCrossChainChannel(localDomain, remoteDomain)) {
                relayerNetworkRepository.updateCrossChainChannel(
                        new CrossChainChannelDO(localDomain, remoteDomain, relayerNodeId, CrossChainChannelStateEnum.CONNECTED)
                );
            } else {
                relayerNetworkRepository.addCrossChainChannel(
                        new CrossChainChannelDO(localDomain, remoteDomain, relayerNodeId, CrossChainChannelStateEnum.CONNECTED)
                );
            }
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_CREATE_NEW_CROSSCHAIN_CHANNEL_FAILED,
                    e,
                    "failed to create new crosschain channel ( local: {}, remote: {}, relayer: {} )",
                    localDomain, remoteDomain, relayerNodeId
            );
        }
    }

    @Override
    public CrossChainChannelDO getCrossChainChannel(String localDomain, String remoteDomain) {
        return relayerNetworkRepository.getCrossChainChannel(localDomain, remoteDomain);
    }

    @Override
    public boolean hasCrossChainChannel(String localDomain, String remoteDomain) {
        return relayerNetworkRepository.hasCrossChainChannel(localDomain, remoteDomain);
    }

    private void processRelayerBlockchainInfo(String networkId, String domain, RelayerNodeInfo relayerNode,
                                              RelayerBlockchainInfo relayerBlockchainInfo) {
//        if (
//                !bcdnsManager.validateDomainCertificate(
//                        relayerBlockchainInfo.getDomainCert().getCrossChainCertificate(),
//                        relayerBlockchainInfo.getDomainSpaceChain()
//                )
//        ) {
//            throw new RuntimeException("Invalid domain certificate for domain " + domain);
//        }
        //TODO: validate ptc certificate
        //TODO: validate domain tpbta

        if (relayerNetworkRepository.hasNetworkItem(networkId, domain, relayerNode.getNodeId())) {
            relayerNetworkRepository.updateNetworkItem(networkId, domain, relayerNode.getNodeId(), DomainRouterSyncStateEnum.SYNC);
        } else {
            addRelayerNetworkItem(networkId, domain, relayerNode.getNodeId(), DomainRouterSyncStateEnum.SYNC);
        }
        relayerNode.addDomainIfNotExist(domain);
    }
}
