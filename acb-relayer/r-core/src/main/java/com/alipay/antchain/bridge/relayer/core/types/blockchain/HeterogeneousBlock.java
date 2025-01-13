package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageTrustLevelEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgTrustLevelEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class HeterogeneousBlock extends AbstractBlock {

    public static List<AuthMsgWrapper> toAuthMsgWrappers(
            String product,
            String blockchainId,
            String domain,
            BigInteger blockHeight,
            List<UniformCrosschainPacketContext> uniformCrosschainPacketContexts
    ) {
        return uniformCrosschainPacketContexts.stream()
                .filter(ucpContext -> ucpContext.getUcp().getSrcMessage().getType() == CrossChainMessage.CrossChainMessageType.AUTH_MSG)
                .map(
                        ucpContext -> {
                            CrossChainMessage crossChainMessage = ucpContext.getUcp().getSrcMessage();
                            AuthMsgWrapper wrapper = AuthMsgWrapper.buildFrom(
                                    product,
                                    blockchainId,
                                    domain,
                                    ucpContext.getUcpId(),
                                    crossChainMessage
                            );

                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_BLOCK_HEIGHT,
                                    blockHeight.toString()
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_BLOCK_HASH,
                                    HexUtil.encodeHexStr(crossChainMessage.getProvableData().getBlockHash())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_BLOCK_TIMESTAMP,
                                    String.valueOf(crossChainMessage.getProvableData().getTimestamp())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_CAPTURE_TIMESTAMP,
                                    String.valueOf(System.currentTimeMillis())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_SENDER_GAS_USED,
                                    "0"
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_HINTS,
                                    StrUtil.EMPTY
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_TX_ID,
                                    HexUtil.encodeHexStr(crossChainMessage.getProvableData().getTxHash())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_HETEROGENEOUS_RANDOM_UUID,
                                    UUID.randomUUID().toString()
                            );
                            wrapper.setProcessState(
                                    wrapper.getTrustLevel() == AuthMsgTrustLevelEnum.ZERO_TRUST ?
                                            AuthMsgProcessStateEnum.PROVED : AuthMsgProcessStateEnum.PENDING
                            );

                            return wrapper;
                        }
                ).collect(Collectors.toList());
    }

    @JSONField
    private List<UniformCrosschainPacketContext> uniformCrosschainPacketContexts;

    @JSONField
    private ConsensusState consensusState;

    @JSONField
    private String domain;

    public HeterogeneousBlock(String product, String domain, String blockchainId, Long height, List<CrossChainMessage> crossChainMessages, ConsensusState consensusState) {
        super(
                product,
                blockchainId,
                height
        );
        this.domain = domain;
        this.uniformCrosschainPacketContexts = crossChainMessages.stream()
                .map(
                        crossChainMessage -> {
                            UniformCrosschainPacketStateEnum stateEnum = UniformCrosschainPacketStateEnum.PENDING;
                            if (crossChainMessage.getType() == CrossChainMessage.CrossChainMessageType.AUTH_MSG) {
                                IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(crossChainMessage.getMessage());
                                if (!isValidAuthMessage(domain, crossChainMessage, authMessage)) {
                                    return null;
                                }
                                if (AuthMessageV2.MY_VERSION == authMessage.getVersion() || AuthMessageV2.MY_VERSION == authMessage.getVersion()) {
                                    stateEnum = ((AuthMessageV2) authMessage).getTrustLevel() == AuthMessageTrustLevelEnum.ZERO_TRUST ?
                                            UniformCrosschainPacketStateEnum.PROVED : UniformCrosschainPacketStateEnum.PENDING;
                                }
                            }
                            return buildUniformCrosschainPacketContext(domain, crossChainMessage, stateEnum);
                        }
                ).filter(ObjectUtil::isNotEmpty)
                .collect(Collectors.toList());
        this.consensusState = consensusState;
    }

    @Override
    public byte[] encode() {
        return JSON.toJSONBytes(this);
    }

    @Override
    public void decode(byte[] data) {
        BeanUtil.copyProperties(JSON.parseObject(data, HeterogeneousBlock.class), this);
    }

    public List<UniformCrosschainPacketContext> getUcpsByTpBta(@NonNull TpBtaDO tpBtaDO, String localRelayerId) {
        if (ObjectUtil.isEmpty(this.uniformCrosschainPacketContexts)) {
            return ListUtil.empty();
        }
        return this.uniformCrosschainPacketContexts.stream()
                .filter(ucp -> tpBtaDO.getTpbta().isMatched(ucp.getCrossChainLane()))
                .peek(ucp -> {
                    ucp.setTpbtaVersion(tpBtaDO.getTpBtaVersion());
                    ucp.setTpbtaLaneKey(tpBtaDO.getCrossChainLane().getLaneKey());
                    ucp.setRelayerId(localRelayerId);
                }).collect(Collectors.toList());
    }

    public List<AuthMsgWrapper> toAuthMsgWrappers() {
        return HeterogeneousBlock.toAuthMsgWrappers(
                getProduct(),
                getBlockchainId(),
                getDomain(),
                BigInteger.valueOf(getHeight()),
                this.getUniformCrosschainPacketContexts()
        );
    }

    private @NonNull UniformCrosschainPacketContext buildUniformCrosschainPacketContext(String domain, CrossChainMessage crossChainMessage, UniformCrosschainPacketStateEnum stateEnum) {
        UniformCrosschainPacketContext ucpContext = new UniformCrosschainPacketContext();
        ucpContext.setUcp(new UniformCrosschainPacket(new CrossChainDomain(domain), crossChainMessage, null));
        ucpContext.setFromNetwork(false);
        ucpContext.setProduct(getProduct());
        ucpContext.setBlockchainId(getBlockchainId());
        ucpContext.setProcessState(stateEnum);
        return ucpContext;
    }

    private boolean isValidAuthMessage(String domain, CrossChainMessage crossChainMessage, IAuthMessage authMessage) {
        if (authMessage.getUpperProtocol() == UpperProtocolTypeBeyondAMEnum.SDP.getCode()) {
            try {
                SDPMessageFactory.createSDPMessage(authMessage.getPayload());
            } catch (Throwable t) {
                log.warn("catch an invalid sdp msg inside auth message and skip it, sdp msg locator: (domain: {}, height: {}, block: {}, tx: {})",
                        domain,
                        crossChainMessage.getProvableData().getHeightVal().toString(),
                        crossChainMessage.getProvableData().getBlockHashHex(),
                        HexUtil.encodeHexStr(crossChainMessage.getProvableData().getTxHash()),
                        t
                );
                return false;
            }
        }
        return true;
    }
}
