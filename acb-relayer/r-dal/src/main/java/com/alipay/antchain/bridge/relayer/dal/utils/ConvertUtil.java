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

package com.alipay.antchain.bridge.relayer.dal.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.commons.core.am.*;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.bta.BlockchainTrustAnchorFactory;
import com.alipay.antchain.bridge.commons.core.ptc.*;
import com.alipay.antchain.bridge.commons.core.rcc.IdempotentInfo;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMessage;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMsgProcessStateEnum;
import com.alipay.antchain.bridge.commons.core.sdp.*;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.dal.entities.*;
import lombok.NonNull;

public class ConvertUtil {

    public static BlockchainEntity convertFromBlockchainMeta(BlockchainMeta blockchainMeta) {
        return BlockchainEntity.builder()
                .blockchainId(blockchainMeta.getBlockchainId())
                .product(blockchainMeta.getProduct())
                .properties(blockchainMeta.getProperties().encode())
                .description(blockchainMeta.getDesc())
                .alias(blockchainMeta.getAlias())
                .build();
    }

    public static BlockchainMeta convertFromBlockchainEntity(BlockchainEntity blockchainEntity) {
        return new BlockchainMeta(
                blockchainEntity.getProduct(),
                blockchainEntity.getBlockchainId(),
                blockchainEntity.getAlias(),
                blockchainEntity.getDescription(),
                blockchainEntity.getProperties()
        );
    }

    public static PluginServerObjectsEntity convertFromPluginServerDO(PluginServerDO pluginServerDO) {
        return PluginServerObjectsEntity.builder()
                .psId(pluginServerDO.getPsId())
                .address(pluginServerDO.getAddress())
                .domains(StrUtil.join(",", pluginServerDO.getDomainsServing()))
                .products(StrUtil.join(",", pluginServerDO.getProductsSupported()))
                .properties(pluginServerDO.getProperties().encode())
                .state(pluginServerDO.getState())
                .build();
    }

    public static PluginServerDO convertFromPluginServerObjectsEntity(PluginServerObjectsEntity pluginServerObject) {
        PluginServerDO pluginServerDO = new PluginServerDO();
        pluginServerDO.setId(pluginServerObject.getId().intValue());
        pluginServerDO.setPsId(pluginServerObject.getPsId());
        pluginServerDO.setAddress(pluginServerObject.getAddress());
        pluginServerDO.setState(pluginServerObject.getState());
        pluginServerDO.setProductsSupported(StrUtil.split(pluginServerObject.getProducts(), ","));
        pluginServerDO.setDomainsServing(StrUtil.split(pluginServerObject.getDomains(), ","));
        pluginServerDO.setProperties(
                PluginServerDO.PluginServerProperties.decode(pluginServerObject.getProperties())
        );
        pluginServerDO.setGmtCreate(pluginServerObject.getGmtCreate());
        pluginServerDO.setGmtModified(pluginServerObject.getGmtModified());

        return pluginServerDO;
    }

    public static AuthMsgWrapper convertFromAuthMsgPoolEntity(AuthMsgPoolEntity authMsgPoolEntity) {
        IAuthMessage authMessage;
        if (authMsgPoolEntity.getVersion() == AuthMessageV1.MY_VERSION) {
            AuthMessageV1 authMessageV1 = (AuthMessageV1) AuthMessageFactory.createAuthMessage(authMsgPoolEntity.getVersion());
            authMessageV1.setIdentity(CrossChainIdentity.fromHexStr(authMsgPoolEntity.getMsgSender()));
            authMessageV1.setUpperProtocol(authMsgPoolEntity.getProtocolType().ordinal());
            authMessageV1.setPayload(authMsgPoolEntity.getPayload());
            authMessage = authMessageV1;
        } else if (authMsgPoolEntity.getVersion() == AuthMessageV2.MY_VERSION) {
            AuthMessageV2 authMessageV2 = (AuthMessageV2) AuthMessageFactory.createAuthMessage(authMsgPoolEntity.getVersion());
            ;
            authMessageV2.setIdentity(CrossChainIdentity.fromHexStr(authMsgPoolEntity.getMsgSender()));
            authMessageV2.setUpperProtocol(authMsgPoolEntity.getProtocolType().ordinal());
            authMessageV2.setPayload(authMsgPoolEntity.getPayload());
            authMessageV2.setTrustLevel(
                    AuthMessageTrustLevelEnum.parseFromValue(
                            authMsgPoolEntity.getTrustLevel().getCode()
                    )
            );
            authMessage = authMessageV2;
        } else {
            throw new RuntimeException(
                    String.format(
                            "wrong version %d of am (id: %d) when convert from entity",
                            authMsgPoolEntity.getVersion(), authMsgPoolEntity.getId()
                    )
            );
        }
        return new AuthMsgWrapper(
                authMsgPoolEntity.getId(),
                authMsgPoolEntity.getProduct(),
                authMsgPoolEntity.getBlockchainId(),
                authMsgPoolEntity.getDomain(),
                authMsgPoolEntity.getUcpId(),
                authMsgPoolEntity.getAmClientContractAddress(),
                authMsgPoolEntity.getProcessState(),
                authMsgPoolEntity.getFailCount(),
                authMsgPoolEntity.getExt(),
                authMessage
        );
    }

    public static AuthMsgPoolEntity convertFromAuthMsgWrapper(AuthMsgWrapper authMsgWrapper) {
        AuthMsgPoolEntity entity = new AuthMsgPoolEntity();
        entity.setId(authMsgWrapper.getAuthMsgId());
        entity.setUcpId(authMsgWrapper.getUcpId());
        entity.setProduct(authMsgWrapper.getProduct());
        entity.setBlockchainId(authMsgWrapper.getBlockchainId());
        entity.setDomain(authMsgWrapper.getDomain());
        entity.setAmClientContractAddress(authMsgWrapper.getAmClientContractAddress());
        entity.setVersion(authMsgWrapper.getVersion());
        entity.setMsgSender(authMsgWrapper.getMsgSender());
        entity.setProtocolType(authMsgWrapper.getProtocolType());
        entity.setTrustLevel(authMsgWrapper.getTrustLevel());
        entity.setPayload(authMsgWrapper.getPayload());
        entity.setProcessState(authMsgWrapper.getProcessState());
        entity.setFailCount(authMsgWrapper.getFailCount());
        entity.setExt(authMsgWrapper.getRawLedgerInfo());

        return entity;
    }

    public static UniformCrosschainPacketContext convertFromUCPPoolEntity(UCPPoolEntity ucpPoolEntity) {
        UniformCrosschainPacket packet = new UniformCrosschainPacket();
        if (StrUtil.isNotEmpty(ucpPoolEntity.getPtcOid())) {
            packet.setPtcId(ObjectIdentity.decode(HexUtil.decodeHex(ucpPoolEntity.getPtcOid())));
        }
        packet.setSrcDomain(new CrossChainDomain(ucpPoolEntity.getSrcDomain()));
        packet.setVersion(ucpPoolEntity.getVersion());
        if (ObjectUtil.isNotEmpty(ucpPoolEntity.getTpProof())) {
            packet.setTpProof(ThirdPartyProof.decode(ucpPoolEntity.getTpProof()));
        }
        if (JSONUtil.isTypeJSONObject(new String(ucpPoolEntity.getRawMessage()))) {
            packet.setSrcMessage(JSON.parseObject(ucpPoolEntity.getRawMessage(), CrossChainMessage.class));
        } else {
            packet.setSrcMessage(CrossChainMessage.decode(ucpPoolEntity.getRawMessage()));
        }

        UniformCrosschainPacketContext context = new UniformCrosschainPacketContext();
        context.setUcp(packet);
        context.setId(ucpPoolEntity.getId());
        context.setUcpId(ucpPoolEntity.getUcpId());
        context.setProduct(ucpPoolEntity.getProduct());
        context.setBlockchainId(ucpPoolEntity.getBlockchainId());
        context.setUdagPath(ucpPoolEntity.getUdagPath());
        context.setProcessState(ucpPoolEntity.getProcessState());
        context.setFromNetwork(ucpPoolEntity.getFromNetwork());
        context.setRelayerId(ucpPoolEntity.getRelayerId());
        context.setTpbtaLaneKey(ucpPoolEntity.getTpbtaLaneKey());
        context.setTpbtaVersion(ucpPoolEntity.getTpbtaVersion());

        return context;
    }

    public static SDPMsgWrapper convertFromSDPMsgPoolEntity(SDPMsgPoolEntity sdpMsgPoolEntity) {
        SDPMsgWrapper wrapper = new SDPMsgWrapper();
        wrapper.setId(sdpMsgPoolEntity.getId());
        wrapper.setReceiverBlockchainProduct(sdpMsgPoolEntity.getReceiverBlockchainProduct());
        wrapper.setReceiverBlockchainId(sdpMsgPoolEntity.getReceiverBlockchainId());
        wrapper.setReceiverAMClientContract(sdpMsgPoolEntity.getReceiverAMClientContract());
        wrapper.setProcessState(sdpMsgPoolEntity.getProcessState());
        wrapper.setTxHash(sdpMsgPoolEntity.getTxHash());
        wrapper.setTxSuccess(sdpMsgPoolEntity.getTxSuccess());
        wrapper.setTxFailReason(sdpMsgPoolEntity.getTxFailReason());

        if (sdpMsgPoolEntity.getVersion() == SDPMessageV1.MY_VERSION) {
            SDPMessageV1 message = new SDPMessageV1();
            message.setSequence(sdpMsgPoolEntity.getMsgSequence().intValue());
            message.setTargetDomain(new CrossChainDomain(sdpMsgPoolEntity.getReceiverDomainName()));
            message.setTargetIdentity(new CrossChainIdentity(HexUtil.decodeHex(sdpMsgPoolEntity.getReceiverId())));
            wrapper.setSdpMessage(message);
        } else if (sdpMsgPoolEntity.getVersion() == SDPMessageV2.MY_VERSION) {
            SDPMessageV2 message = new SDPMessageV2();
            message.setSequence(sdpMsgPoolEntity.getMsgSequence().intValue());
            message.setTargetDomain(new CrossChainDomain(sdpMsgPoolEntity.getReceiverDomainName()));
            message.setTargetIdentity(new CrossChainIdentity(HexUtil.decodeHex(sdpMsgPoolEntity.getReceiverId())));
            message.setAtomicFlag(AtomicFlagEnum.parseFrom(sdpMsgPoolEntity.getAtomicFlag().byteValue()));
            message.setMessageId(new SDPMessageId(sdpMsgPoolEntity.getMessageId()));
            message.setNonce(new BigInteger(sdpMsgPoolEntity.getNonce()).longValueExact());
            wrapper.setSdpMessage(message);
        } else if (sdpMsgPoolEntity.getVersion() == SDPMessageV3.MY_VERSION) {
            SDPMessageV3 message = new SDPMessageV3();
            message.setSequence(sdpMsgPoolEntity.getMsgSequence().intValue());
            message.setTargetDomain(new CrossChainDomain(sdpMsgPoolEntity.getReceiverDomainName()));
            message.setTargetIdentity(new CrossChainIdentity(HexUtil.decodeHex(sdpMsgPoolEntity.getReceiverId())));
            message.setAtomicFlag(AtomicFlagEnum.parseFrom(sdpMsgPoolEntity.getAtomicFlag().byteValue()));
            message.setMessageId(new SDPMessageId(sdpMsgPoolEntity.getMessageId()));
            message.setNonce(new BigInteger(sdpMsgPoolEntity.getNonce()).longValueExact());
            message.setTimeoutMeasure(TimeoutMeasureEnum.parseFrom(sdpMsgPoolEntity.getTimeoutMeasureEnum().byteValue()));
            message.setTimeout(sdpMsgPoolEntity.getTimeout());
            wrapper.setSdpMessage(message);
        }else {
            throw new RuntimeException("Invalid version of sdp message: " + sdpMsgPoolEntity.getVersion());
        }

        AuthMsgWrapper authMsgWrapper = new AuthMsgWrapper();
        authMsgWrapper.setAuthMsgId(sdpMsgPoolEntity.getAuthMsgId());
        authMsgWrapper.setProduct(sdpMsgPoolEntity.getSenderBlockchainProduct());
        authMsgWrapper.setBlockchainId(sdpMsgPoolEntity.getSenderBlockchainId());
        authMsgWrapper.setDomain(sdpMsgPoolEntity.getSenderDomainName());
        authMsgWrapper.setMsgSender(sdpMsgPoolEntity.getSenderId());
        authMsgWrapper.setAmClientContractAddress(sdpMsgPoolEntity.getSenderAMClientContract());

        wrapper.setAuthMsgWrapper(authMsgWrapper);
        wrapper.setCreateTime(ObjectUtil.isNotNull(sdpMsgPoolEntity.getGmtCreate()) ? sdpMsgPoolEntity.getGmtCreate().getTime() : Long.MAX_VALUE);
        wrapper.setLastUpdateTime(ObjectUtil.isNotNull(sdpMsgPoolEntity.getGmtModified()) ? sdpMsgPoolEntity.getGmtModified().getTime() : Long.MAX_VALUE);

        return wrapper;
    }

    public static SDPMsgPoolEntity convertFromSDPMsgWrapper(SDPMsgWrapper wrapper) {
        SDPMsgPoolEntity entity = SDPMsgPoolEntity.builder().build();
        entity.setId(wrapper.getId());
        entity.setVersion(wrapper.getVersion());
        entity.setAtomicFlag(Integer.valueOf(wrapper.getAtomicFlag()));
        entity.setMessageId(wrapper.getSdpMessage().getMessageId().toHexStr());
        entity.setNonce(BigInteger.valueOf(wrapper.getSdpMessage().getNonce()).toString());

        entity.setSenderBlockchainProduct(wrapper.getSenderBlockchainProduct());
        entity.setSenderBlockchainId(wrapper.getSenderBlockchainId());
        entity.setSenderDomainName(wrapper.getSenderBlockchainDomain());
        entity.setSenderId(wrapper.getMsgSender());
        entity.setSenderAMClientContract(wrapper.getSenderAMClientContract());

        entity.setReceiverBlockchainProduct(wrapper.getReceiverBlockchainProduct());
        entity.setReceiverBlockchainId(wrapper.getReceiverBlockchainId());
        entity.setReceiverDomainName(wrapper.getReceiverBlockchainDomain());
        entity.setReceiverId(wrapper.getMsgReceiver());
        entity.setReceiverAMClientContract(wrapper.getReceiverAMClientContract());

        entity.setMsgSequence((long) wrapper.getMsgSequence());
        entity.setProcessState(wrapper.getProcessState());
        entity.setTxHash(wrapper.getTxHash());
        entity.setTxSuccess(wrapper.isTxSuccess());
        entity.setTxFailReason(wrapper.getTxFailReason());

        entity.setTimeoutMeasureEnum(Integer.valueOf(wrapper.getSdpMessage().getTimeoutMeasure().getValue()));
        entity.setTimeout(wrapper.getSdpMessage().getTimeout());

        return entity;
    }

    public static RelayerNetworkEntity convertFromRelayerNetworkItem(String networkId, String domain, RelayerNetwork.DomainRouterItem relayerNetworkItem) {
        RelayerNetworkEntity entity = new RelayerNetworkEntity();
        entity.setNetworkId(networkId);
        entity.setDomain(domain);
        entity.setNodeId(relayerNetworkItem.getNodeId());
        entity.setSyncState(relayerNetworkItem.getSyncState());

        return entity;
    }

    public static RelayerNetwork.DomainRouterItem convertFromRelayerNetworkEntity(RelayerNetworkEntity entity) {
        return new RelayerNetwork.DomainRouterItem(entity.getNodeId(), entity.getSyncState());
    }

    public static RelayerNodeInfo convertFromRelayerNodeEntity(RelayerNodeEntity entity) {
        RelayerNodeInfo nodeInfo = new RelayerNodeInfo(
                entity.getNodeId(),
                CrossChainCertificateFactory.createCrossChainCertificate(
                        entity.getNodeCrossChainCert()
                ),
                SignAlgoEnum.getByName(entity.getNodeSigAlgo()),
                stringToList(entity.getEndpoints()),
                stringToList(entity.getDomains())
        );
        if (ObjectUtil.isNotEmpty(entity.getBlockchainContent())) {
            nodeInfo.setRelayerBlockchainContent(
                    RelayerBlockchainContent.decodeFromJson(new String(entity.getBlockchainContent()))
            );
        }
        if (ObjectUtil.isNotNull(entity.getProperties())) {
            nodeInfo.setProperties(
                    RelayerNodeInfo.RelayerNodeProperties.decodeFromJson(
                            new String(entity.getProperties())
                    )
            );
        }
        return nodeInfo;
    }

    public static RelayerNodeEntity convertFromRelayerNodeInfo(RelayerNodeInfo nodeInfo) throws IOException {
        RelayerNodeEntity entity = new RelayerNodeEntity();
        entity.setNodeId(nodeInfo.getNodeId());
        entity.setRelayerCertId(nodeInfo.getRelayerCertId());
        entity.setDomains(
                listToString(nodeInfo.getDomains())
        );
        entity.setNodeSigAlgo(nodeInfo.getSigAlgo().getName());
        entity.setNodeCrossChainCert(nodeInfo.getRelayerCrossChainCertificate().encode());
        entity.setEndpoints(
                listToString(nodeInfo.getEndpoints())
        );

        entity.setBlockchainContent(
                ObjectUtil.isNull(nodeInfo.getRelayerBlockchainContent()) ?
                        StrUtil.EMPTY.getBytes() : nodeInfo.getRelayerBlockchainContent().encodeToJson().getBytes()
        );
        if (ObjectUtil.isNotNull(nodeInfo.getProperties())) {
            entity.setProperties(nodeInfo.marshalProperties().getBytes());
        }

        return entity;
    }

    public static String listToString(List<String> list) {
        return list.stream().reduce((s1, s2) -> StrUtil.join("^", s1, s2)).orElse("");
    }

    public static List<String> stringToList(String str) {
        return StrUtil.split(str, "^");
    }

    public static RelayerHealthInfo convertFromDTActiveNodeEntity(int port, long activateLength, DTActiveNodeEntity entity) {
        return new RelayerHealthInfo(
                entity.getNodeIp(),
                port,
                entity.getLastActiveTime().getTime(),
                activateLength
        );
    }

    public static BlockchainDistributedTask convertFromBlockchainDTTaskEntity(BlockchainDTTaskEntity entity) {
        BlockchainDistributedTask blockchainDistributedTask = new BlockchainDistributedTask();
        blockchainDistributedTask.setNodeId(entity.getNodeId());
        blockchainDistributedTask.setTaskType(entity.getTaskType());
        blockchainDistributedTask.setBlockchainId(entity.getBlockchainId());
        blockchainDistributedTask.setBlockchainProduct(entity.getProduct());
        blockchainDistributedTask.setStartTime(entity.getTimeSlice().getTime());
        blockchainDistributedTask.setExt(entity.getExt());
        return blockchainDistributedTask;
    }

    public static BlockchainDTTaskEntity convertFromBlockchainDistributedTask(BlockchainDistributedTask task) {
        BlockchainDTTaskEntity entity = new BlockchainDTTaskEntity();
        entity.setTaskType(task.getTaskType());
        entity.setBlockchainId(task.getBlockchainId());
        entity.setProduct(task.getBlockchainProduct());
        entity.setNodeId(task.getNodeId());
        entity.setTimeSlice(new Date(task.getStartTime()));
        entity.setExt(task.getExt());
        return entity;
    }

    public static BizDistributedTask convertFromBizDTTaskEntity(BizDTTaskEntity entity) {
        BizDistributedTask bizDistributedTask = new BizDistributedTask();
        bizDistributedTask.setNodeId(entity.getNodeId());
        bizDistributedTask.setTaskType(entity.getTaskType());
        bizDistributedTask.setUniqueKey(entity.getUniqueKey());
        bizDistributedTask.setStartTime(entity.getTimeSlice().getTime());
        bizDistributedTask.setExt(entity.getExt());
        return bizDistributedTask;
    }

    public static BizDTTaskEntity convertFromBizDistributedTask(BizDistributedTask task) {
        BizDTTaskEntity entity = new BizDTTaskEntity();
        entity.setTaskType(task.getTaskType());
        entity.setUniqueKey(task.getUniqueKey());
        entity.setNodeId(task.getNodeId());
        entity.setTimeSlice(new Date(task.getStartTime()));
        entity.setExt(task.getExt());
        return entity;
    }

    public static ActiveNode convertFromDTActiveNodeEntityActiveNode(DTActiveNodeEntity entity) {
        ActiveNode node = new ActiveNode();
        node.setNodeIp(entity.getNodeIp());
        node.setNodeId(entity.getNodeId());
        node.setLastActiveTime(entity.getLastActiveTime().getTime());
        return node;
    }

    public static CrossChainMsgACLEntity convertFromCrossChainMsgACLEntity(CrossChainMsgACLItem item) {
        CrossChainMsgACLEntity entity = new CrossChainMsgACLEntity();
        entity.setBizId(item.getBizId());

        entity.setOwnerDomain(item.getOwnerDomain());
        entity.setOwnerId(item.getOwnerIdentity());
        entity.setOwnerIdHex(
                ObjectUtil.isNull(item.getOwnerIdentityHex()) ?
                        null : item.getOwnerIdentityHex().toLowerCase()
        );

        entity.setGrantDomain(item.getGrantDomain());
        entity.setGrantId(item.getGrantIdentity());
        entity.setGrantIdHex(
                ObjectUtil.isNull(item.getGrantIdentityHex()) ?
                        null : item.getGrantIdentityHex().toLowerCase()
        );

        entity.setIsDeleted(item.getIsDeleted());

        return entity;
    }

    public static CrossChainMsgACLItem convertFromCrossChainMsgACLEntity(CrossChainMsgACLEntity entity) {
        CrossChainMsgACLItem item = new CrossChainMsgACLItem();

        item.setBizId(entity.getBizId());

        item.setOwnerDomain(entity.getOwnerDomain());
        item.setOwnerIdentity(entity.getOwnerId());
        item.setOwnerIdentityHex(entity.getOwnerIdHex());

        item.setGrantDomain(entity.getGrantDomain());
        item.setGrantIdentity(entity.getGrantId());
        item.setGrantIdentityHex(entity.getGrantIdHex());

        item.setIsDeleted(entity.getIsDeleted());

        return item;
    }

    public static DomainCertWrapper convertFromDomainCertEntity(DomainCertEntity entity) {
        AbstractCrossChainCertificate crossChainCertificate = CrossChainCertificateFactory.createCrossChainCertificate(
                entity.getDomainCert()
        );
        Assert.equals(
                CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE,
                crossChainCertificate.getType()
        );
        DomainNameCredentialSubject domainNameCredentialSubject = DomainNameCredentialSubject.decode(
                crossChainCertificate.getCredentialSubject()
        );

        return new DomainCertWrapper(
                crossChainCertificate,
                domainNameCredentialSubject,
                entity.getProduct(),
                entity.getBlockchainId(),
                entity.getDomain(),
                entity.getDomainSpace()
        );
    }

    public static DomainCertEntity convertFromDomainCertWrapper(DomainCertWrapper wrapper) {
        DomainCertEntity entity = new DomainCertEntity();
        entity.setDomainCert(wrapper.getCrossChainCertificate().encode());
        entity.setDomain(wrapper.getDomain());
        entity.setProduct(wrapper.getBlockchainProduct());
        entity.setBlockchainId(wrapper.getBlockchainId());
        entity.setSubjectOid(
                wrapper.getDomainNameCredentialSubject().getApplicant().encode()
        );
        entity.setIssuerOid(
                wrapper.getCrossChainCertificate().getIssuer().encode()
        );
        entity.setDomainSpace(wrapper.getDomainSpace());
        return entity;
    }

    public static DomainSpaceCertEntity convertFromDomainSpaceCertWrapper(DomainSpaceCertWrapper wrapper) {
        DomainSpaceCertEntity entity = new DomainSpaceCertEntity();
        entity.setDomainSpace(wrapper.getDomainSpace());
        entity.setParentSpace(wrapper.getParentDomainSpace());
        entity.setOwnerOidHex(HexUtil.encodeHexStr(wrapper.getOwnerOid().encode()));
        entity.setDescription(wrapper.getDesc());
        entity.setDomainSpaceCert(wrapper.getDomainSpaceCert().encode());
        return entity;
    }

    public static DomainSpaceCertWrapper convertFromDomainSpaceCertEntity(DomainSpaceCertEntity entity) {
        AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(entity.getDomainSpaceCert());
        DomainSpaceCertWrapper wrapper = new DomainSpaceCertWrapper(certificate);
        wrapper.setDesc(entity.getDescription());
        return wrapper;
    }

    public static BCDNSServiceDO convertFromBCDNSServiceEntity(@NonNull BCDNSServiceEntity entity, @NonNull DomainSpaceCertWrapper domainSpaceCertWrapper) {
        return new BCDNSServiceDO(
                entity.getDomainSpace(),
                ObjectIdentity.decode(HexUtil.decodeHex(entity.getOwnerOid())),
                domainSpaceCertWrapper,
                BCDNSTypeEnum.parseFromValue(entity.getType()),
                entity.getState(),
                entity.getProperties()
        );
    }

    public static BCDNSServiceEntity convertFromBCDNSServiceDO(BCDNSServiceDO bcdnsServiceDO) {
        BCDNSServiceEntity entity = new BCDNSServiceEntity();
        entity.setDomainSpace(bcdnsServiceDO.getDomainSpace());
        entity.setOwnerOid(HexUtil.encodeHexStr(bcdnsServiceDO.getOwnerOid().encode()));
        entity.setType(bcdnsServiceDO.getType().getCode());
        entity.setState(bcdnsServiceDO.getState());
        entity.setProperties(bcdnsServiceDO.getProperties());
        return entity;
    }

    public static DomainCertApplicationDO convertFromDomainCertApplicationEntity(DomainCertApplicationEntity entity) {
        return BeanUtil.copyProperties(entity, DomainCertApplicationDO.class);
    }

    public static DomainCertApplicationEntity convertFromDomainCertApplicationDO(DomainCertApplicationDO domainCertApplicationDO) {
        return BeanUtil.copyProperties(domainCertApplicationDO, DomainCertApplicationEntity.class);
    }

    public static MarkDTTaskEntity convertFromMarkDTTask(MarkDTTask markDTTask) {
        MarkDTTaskEntity entity = new MarkDTTaskEntity();
        entity.setNodeId(StrUtil.emptyToDefault(markDTTask.getNodeId(), null));
        entity.setUniqueKey(StrUtil.emptyToDefault(markDTTask.getUniqueKey(), null));
        entity.setTaskType(markDTTask.getTaskType());
        entity.setEndTime(ObjectUtil.isNotNull(markDTTask.getEndTime()) ? new Date(markDTTask.getEndTime()) : null);
        entity.setState(markDTTask.getState());
        return entity;
    }

    public static MarkDTTask convertFromMarkDTTaskEntity(MarkDTTaskEntity entity) {
        MarkDTTask task = BeanUtil.copyProperties(entity, MarkDTTask.class);
        if (ObjectUtil.isNotNull(entity.getEndTime())) {
            task.setEndTime(entity.getEndTime().getTime());
        }
        return task;
    }

    public static CrossChainChannelDO convertFromCrossChainChannelEntity(CrossChainChannelEntity entity) {
        return BeanUtil.copyProperties(entity, CrossChainChannelDO.class);
    }

    public static CrossChainChannelEntity convertFromCrossChainChannelDO(CrossChainChannelDO crossChainChannelDO) {
        return BeanUtil.copyProperties(crossChainChannelDO, CrossChainChannelEntity.class);
    }

    public static PtcServiceEntity convertFromPtcServiceDO(PtcServiceDO ptcServiceDO) {
        return new PtcServiceEntity(
                ptcServiceDO.getServiceId(),
                ptcServiceDO.getType().name(),
                HexUtil.encodeHexStr(ptcServiceDO.getPtcCert().getCredentialSubjectInstance().getApplicant().encode()),
                ptcServiceDO.getIssuerBcdnsDomainSpace(),
                ptcServiceDO.getState(),
                ptcServiceDO.getPtcCert().encode(),
                ptcServiceDO.getClientConfig()
        );
    }

    public static PtcServiceDO convertFromPtcServiceEntity(PtcServiceEntity entity) {
        return PtcServiceDO.builder()
                .type(PTCTypeEnum.valueOf(entity.getType()))
                .serviceId(entity.getServiceId())
                .state(entity.getState())
                .ownerId(ObjectIdentity.decode(HexUtil.decodeHex(entity.getOwnerIdHex())))
                .issuerBcdnsDomainSpace(entity.getIssuerBcdnsDomainSpace())
                .clientConfig(entity.getClientConfig())
                .ptcCert(CrossChainCertificateFactory.createCrossChainCertificate(entity.getPtcCert()))
                .build();
    }

    public static BtaEntity convertFromBtaDO(BtaDO btaDO) {
        return new BtaEntity(
                btaDO.getBlockchainProduct(),
                btaDO.getBlockchainId(),
                btaDO.getDomain().getDomain(),
                btaDO.getSubjectVersion(),
                HexUtil.encodeHexStr(btaDO.getPtcOid().encode()),
                btaDO.getBta().encode()
        );
    }

    public static BtaDO convertFromBtaEntity(BtaEntity entity) {
        return BtaDO.builder()
                .bta(BlockchainTrustAnchorFactory.createBTA(entity.getRawBta()))
                .blockchainProduct(entity.getBlockchainProduct())
                .blockchainId(entity.getBlockchainId())
                .build();
    }

    public static TpBtaEntity convertFromTpBtaDO(TpBtaDO tpBtaDO) {
        return new TpBtaEntity(
                tpBtaDO.getBlockchainProduct(),
                tpBtaDO.getBlockchainId(),
                tpBtaDO.getPtcServiceId(),
                tpBtaDO.getVersion(),
                tpBtaDO.getBtaSubjectVersion(),
                tpBtaDO.getCrossChainLane().getSenderDomain().getDomain(),
                ObjectUtil.defaultIfNull(tpBtaDO.getCrossChainLane().getSenderIdHex(), ""),
                ObjectUtil.isNull(tpBtaDO.getCrossChainLane().getReceiverDomain()) ? "" : tpBtaDO.getCrossChainLane().getReceiverDomain().getDomain(),
                ObjectUtil.defaultIfNull(tpBtaDO.getCrossChainLane().getReceiverIdHex(), ""),
                tpBtaDO.getTpBtaVersion(),
                tpBtaDO.getVerifyAnchorVersion().toString(),
                tpBtaDO.getTpbta().encode()
        );
    }

    public static TpBtaDO convertFromTpBtaEntity(TpBtaEntity entity) {
        return TpBtaDO.builder()
                .blockchainProduct(entity.getBlockchainProduct())
                .blockchainId(entity.getBlockchainId())
                .ptcServiceId(entity.getPtcServiceId())
                .tpbta(ThirdPartyBlockchainTrustAnchor.decode(entity.getRawTpbta()))
                .build();
    }

    public static ValidatedConsensusStateEntity convertFromValidatedConsensusStateDO(ValidatedConsensusStateDO validatedConsensusStateDO) {
        return new ValidatedConsensusStateEntity(
                validatedConsensusStateDO.getBlockchainProduct(),
                validatedConsensusStateDO.getBlockchainId(),
                validatedConsensusStateDO.getDomain().getDomain(),
                validatedConsensusStateDO.getTpbtaLane().getLaneKey(),
                validatedConsensusStateDO.getPtcServiceId(),
                validatedConsensusStateDO.getHeight().toString(),
                validatedConsensusStateDO.getHashHex(),
                validatedConsensusStateDO.getParentHashHex(),
                new Date(validatedConsensusStateDO.getStateTime()),
                validatedConsensusStateDO.getValidatedConsensusState().encode()
        );
    }

    public static ValidatedConsensusStateDO convertFromValidatedConsensusStateEntity(ValidatedConsensusStateEntity entity) {
        return ValidatedConsensusStateDO.builder()
                .blockchainProduct(entity.getBlockchainProduct())
                .blockchainId(entity.getBlockchainId())
                .ptcServiceId(entity.getPtcServiceId())
                .validatedConsensusState(ValidatedConsensusState.decode(entity.getRawVcs()))
                .build();
    }

    public static PtcTrustRootEntity convertFromPtcTrustRootDO(PtcTrustRootDO ptcTrustRootDO) {
        return new PtcTrustRootEntity(
                ptcTrustRootDO.getPtcServiceId(),
                ptcTrustRootDO.getOwnerOidHex(),
                ptcTrustRootDO.getLatestVerifyAnchor().getVersion().toString(),
                ptcTrustRootDO.getIssuerBcdnsDomainSpace().getDomain(),
                ptcTrustRootDO.getNetworkInfo(),
                ptcTrustRootDO.getPtcCrossChainCert().encode(),
                ptcTrustRootDO.getTrustRootSignAlgo().getName(),
                ptcTrustRootDO.getTrustRootSignature()
        );
    }

    public static PtcTrustRootDO convertFromPtcTrustRootEntity(PtcTrustRootEntity entity) {
        return PtcTrustRootDO.builder()
                .ptcServiceId(entity.getPtcServiceId())
                .ptcTrustRoot(
                        PTCTrustRoot.builder()
                                .ptcCrossChainCert(CrossChainCertificateFactory.createCrossChainCertificate(entity.getPtcCrossChainCert()))
                                .sigAlgo(SignAlgoEnum.getByName(entity.getSignAlgo()))
                                .issuerBcdnsDomainSpace(new CrossChainDomain(entity.getIssuerBcdnsDomainSpace()))
                                .networkInfo(entity.getNetworkInfo())
                                .sig(entity.getSig())
                                .build()
                ).build();
    }

    public static PtcVerifyAnchorEntity convertFromPtcVerifyAnchorDO(PtcVerifyAnchorDO ptcVerifyAnchorDO) {
        return new PtcVerifyAnchorEntity(
                ptcVerifyAnchorDO.getPtcServiceId(),
                HexUtil.encodeHexStr(ptcVerifyAnchorDO.getOwnerOid().encode()),
                ptcVerifyAnchorDO.getPtcVerifyAnchor().getVersion().toString(),
                ptcVerifyAnchorDO.getPtcVerifyAnchor().getAnchor()
        );
    }

    public static PtcVerifyAnchorDO convertFromPtcVerifyAnchorEntity(PtcVerifyAnchorEntity entity) {
        return PtcVerifyAnchorDO.builder()
                .ownerOid(ObjectIdentity.decode(HexUtil.decodeHex(entity.getOwnerIdHex())))
                .ptcServiceId(entity.getPtcServiceId())
                .ptcVerifyAnchor(new PTCVerifyAnchor(
                        new BigInteger(entity.getVersionNum()),
                        entity.getAnchor()
                )).build();
    }

    public static String getHeightKey(String heightType, CrossChainLane tpbtaLane) {
        return ObjectUtil.isNull(tpbtaLane) ? heightType : StrUtil.join("-", heightType, tpbtaLane.getLaneKey());
    }

    public static String getHeightKey(String heightType, String tpbtaLaneKey) {
        return StrUtil.isEmpty(tpbtaLaneKey) ? heightType : StrUtil.join("-", heightType, tpbtaLaneKey);
    }

    public static CrossChainLane getTpBtaLaneFromHeightKey(String heightKey) {
        List<String> arr = StrUtil.split(heightKey, "-");
        if (arr.size() <= 1) {
            return null;
        }
        return CrossChainLane.fromLaneKey(arr.get(1));
    }

    public static String getHeightTypeFromHeightKey(String heightKey) {
        return StrUtil.split(heightKey, "-").get(0);
    }

    public static ReliableCrossChainMessage convertFromReliableCrossChainMsgEntity(ReliableCrossChainMsgEntity entity) {
        return new ReliableCrossChainMessage(
                new IdempotentInfo(
                        entity.getSenderDomainName(),
                        HexUtil.decodeHex(entity.getSenderIdentity()),
                        entity.getReceiverDomainName(),
                        HexUtil.decodeHex(entity.getReceiverIdentity()),
                        entity.getNonce()
                ),
                ReliableCrossChainMsgProcessStateEnum.getByCode(entity.getStatus()),
                entity.getOriginalHash(),
                entity.getCurrentHash(),
                entity.getRetryTime(),
                entity.getTxTimestamp(),
                entity.getErrorMsg(),
                entity.getRawTx()
        );
    }

    public static SDPNonceRecordDO convertFromSDPNonceRecordEntity(SDPNonceRecordEntity entity) {
        return SDPNonceRecordDO.builder()
                .senderDomain(entity.getSenderDomain())
                .senderIdentity(entity.getSenderIdentity())
                .receiverDomain(entity.getReceiverDomain())
                .receiverIdentity(entity.getReceiverIdentity())
                .messageId(entity.getMessageId())
                .hashVal(entity.getHashVal())
                .nonce(new BigInteger(entity.getNonce()))
                .build();
    }

    public static SDPNonceRecordEntity convertFromSDPNonceRecordDO(SDPNonceRecordDO entity) {
        return new SDPNonceRecordEntity(
                entity.getMessageId(),
                entity.getSenderDomain(),
                entity.getSenderIdentity(),
                entity.getReceiverDomain(),
                entity.getReceiverIdentity(),
                entity.getNonce().toString(),
                entity.getHashVal()
        );
    }
}
