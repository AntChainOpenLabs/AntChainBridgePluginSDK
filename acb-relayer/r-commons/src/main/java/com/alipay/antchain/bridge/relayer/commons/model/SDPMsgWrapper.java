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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.math.BigInteger;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.sdp.*;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SDPMsgWrapper {

    public final static int UNORDERED_SDP_MSG_SEQ = -1;

    public final static String UNORDERED_SDP_MSG_SESSION = "UNORDERED";

    public static SDPMsgWrapper buildFrom(AuthMsgWrapper authMsgWrapper) {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(authMsgWrapper.getAuthMessage().getPayload());
        SDPMsgWrapper sdpMsgWrapper = new SDPMsgWrapper();
        sdpMsgWrapper.setAuthMsgWrapper(authMsgWrapper);
        sdpMsgWrapper.setSdpMessage((AbstractSDPMessage) sdpMessage);

        return sdpMsgWrapper;
    }

    private Long id;

    private AuthMsgWrapper authMsgWrapper;

    private String receiverBlockchainProduct;

    private String receiverBlockchainId;

    private String receiverAMClientContract;

    private SDPMsgProcessStateEnum processState;

    private String txHash;

    private boolean txSuccess;

    private String txFailReason;

    private AbstractSDPMessage sdpMessage;

    private long createTime;

    private long lastUpdateTime;

    public SDPMsgWrapper(
            String receiverBlockchainProduct,
            String receiverBlockchainId,
            String receiverAMClientContract,
            SDPMsgProcessStateEnum processState,
            String txHash,
            boolean txSuccess,
            String txFailReason,
            AuthMsgWrapper authMsgWrapper,
            AbstractSDPMessage sdpMessage
    ) {
        this(
                null,
                authMsgWrapper,
                receiverBlockchainProduct,
                receiverBlockchainId,
                receiverAMClientContract,
                processState,
                txHash,
                txSuccess,
                txFailReason,
                sdpMessage,
                Long.MAX_VALUE,
                Long.MAX_VALUE
        );
    }

    public int getVersion() {
        return this.sdpMessage.getVersion();
    }

    public boolean getAtomic() {
        return this.sdpMessage.getAtomic();
    }

    public boolean isUnorderedMsg() {
        return getMsgSequence() == UNORDERED_SDP_MSG_SEQ;
    }

    public byte getAtomicFlag() {
        return this.sdpMessage.getAtomicFlag().getValue();
    }

    public String getSenderBlockchainProduct() {
        return this.authMsgWrapper.getProduct();
    }

    public String getSenderBlockchainId() {
        return this.authMsgWrapper.getBlockchainId();
    }

    public String getSenderBlockchainDomain() {
        return this.authMsgWrapper.getDomain();
    }

    public String getMsgSender() {
        return this.authMsgWrapper.getMsgSender();
    }

    public String getSenderAMClientContract() {
        return this.authMsgWrapper.getAmClientContractAddress();
    }

    public String getReceiverBlockchainDomain() {
        return this.sdpMessage.getTargetDomain().getDomain();
    }

    public String getMsgReceiver() {
        return this.sdpMessage.getTargetIdentity().toHex();
    }

    public int getMsgSequence() {
        return this.sdpMessage.getSequence();
    }

    public boolean isBlockchainSelfCall() {
        return StrUtil.equals(getSenderBlockchainDomain(), getReceiverBlockchainDomain())
                && StrUtil.equalsIgnoreCase(getMsgSender(), getMsgReceiver());
    }

    public boolean isBlockStateMsg() {
        return getSdpMessage().getAtomicFlag() == AtomicFlagEnum.ACK_RECEIVE_TX_FAILED
               && StrUtil.equals(getMsgSender(), CrossChainIdentity.ZERO_ID.toHex())
               && StrUtil.equals(getMsgReceiver(), CrossChainIdentity.ZERO_ID.toHex());
    }

    /**
     * getSessionKey returns session key e.g: "domainA.idA:domainB.idB"
     */
    public String getSessionKey() {
        String key = String.format(
                "%s.%s:%s.%s",
                getSenderBlockchainDomain(),
                getMsgSender(),
                getReceiverBlockchainDomain(),
                getMsgReceiver()
        );
        if (UNORDERED_SDP_MSG_SEQ == getMsgSequence()) {
            // 将无序消息单拎出来，完全异步发送
            return String.format("%s-%s", UNORDERED_SDP_MSG_SESSION, key);
        }
        return key;
    }

    public CrossChainLane getLane() {
        return new CrossChainLane(
                new CrossChainDomain(getSenderBlockchainDomain()),
                new CrossChainDomain(getReceiverBlockchainDomain()),
                CrossChainIdentity.fromHexStr(getMsgSender()),
                CrossChainIdentity.fromHexStr(getMsgReceiver())
        );
    }

    public String getMessageId() {
        return this.sdpMessage.getMessageId().toHexStr();
    }

    public BigInteger getNonce() {
        return BigInteger.valueOf(this.sdpMessage.getNonce());
    }

    public byte[] getPayload() {
        if (ObjectUtil.isAllNotEmpty(this.sdpMessage, this.sdpMessage.getPayload())) {
            return this.sdpMessage.getPayload();
        }
        if (ObjectUtil.isAllNotEmpty(this.authMsgWrapper.getAuthMessage(), this.authMsgWrapper.getAuthMessage().getPayload())) {
            this.sdpMessage = (AbstractSDPMessage) SDPMessageFactory.createSDPMessage(this.authMsgWrapper.getAuthMessage().getPayload());
            return this.sdpMessage.getPayload();
        }
        return null;
    }

    public boolean isAck() {
        return this.getAtomicFlag() >= AtomicFlagEnum.ACK_SUCCESS.getValue();
    }

    public byte getTimeoutMeasure() {
        return this.sdpMessage.getTimeoutMeasure().getValue();
    }

    public BigInteger getTimeout() {
        return this.sdpMessage.getTimeout();
    }

    public boolean isCommitDelayed(long thresholdMs) {
        if (processState != SDPMsgProcessStateEnum.PENDING) {
            return false;
        }
        if (createTime != Long.MAX_VALUE) {
            return System.currentTimeMillis() > thresholdMs + createTime;
        }
        return false;
    }

    public boolean isTxPendingDelayed(long thresholdMs) {
        if (processState != SDPMsgProcessStateEnum.TX_PENDING) {
            return false;
        }
        if (lastUpdateTime != Long.MAX_VALUE) {
            return System.currentTimeMillis() > thresholdMs + lastUpdateTime;
        }
        return false;
    }
}
