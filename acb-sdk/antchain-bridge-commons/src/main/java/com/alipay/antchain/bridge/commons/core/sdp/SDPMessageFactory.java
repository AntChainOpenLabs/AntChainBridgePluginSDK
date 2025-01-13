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

package com.alipay.antchain.bridge.commons.core.sdp;

import java.math.BigInteger;

import com.alipay.antchain.bridge.commons.core.base.BlockState;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;

public class SDPMessageFactory {

    public static ISDPMessage createSDPMessage(byte[] rawMessage) {
        ISDPMessage sdpMessage = createSDPMessage(AbstractSDPMessage.decodeVersionFromBytes(rawMessage));
        sdpMessage.decode(rawMessage);
        return sdpMessage;
    }

    public static ISDPMessage createSDPMessage(int version, byte[] messageId, String targetDomain, byte[] targetIdentity, int sequence, byte[] payload) {
        return createSDPMessage(version, messageId, targetDomain, targetIdentity, AtomicFlagEnum.NONE_ATOMIC, -1, sequence, payload, null);
    }

    public static ISDPMessage createSDPMessage(
            int version,
            byte[] messageId,
            String targetDomain,
            byte[] targetIdentity,
            AtomicFlagEnum atomicFlag,
            long nonce,
            int sequence,
            byte[] payload,
            String errorMsg
    ) {
        return createSDPMessage(
                version,
                messageId,
                targetDomain,
                targetIdentity,
                atomicFlag,
                TimeoutMeasureEnum.NO_TIMEOUT,
                BigInteger.ZERO,
                nonce,
                sequence,
                payload,
                errorMsg
        );
    }

    public static ISDPMessage createSDPMessage(
            int version,
            byte[] messageId,
            String targetDomain,
            byte[] targetIdentity,
            AtomicFlagEnum atomicFlag,
            TimeoutMeasureEnum timeoutMeasureEnum,
            BigInteger timeout,
            long nonce,
            int sequence,
            byte[] payload,
            String errorMsg
    ) {
        AbstractSDPMessage sdpMessage = createAbstractSDPMessage(version);

        sdpMessage.setTargetDomain(new CrossChainDomain(targetDomain));
        sdpMessage.setTargetIdentity(new CrossChainIdentity(targetIdentity));
        sdpMessage.setSequence(sequence);
        sdpMessage.setPayload(payload);

        if (version == SDPMessageV2.MY_VERSION) {
            ((SDPMessageV2) sdpMessage).setMessageId(new SDPMessageId(messageId));
            ((SDPMessageV2) sdpMessage).setNonce(nonce);
            ((SDPMessageV2) sdpMessage).setAtomicFlag(atomicFlag);
            if (AtomicFlagEnum.withErrorMsg(atomicFlag)) {
                ((SDPMessageV2) sdpMessage).setErrorMsg(errorMsg);
            }
        } else if (version == SDPMessageV3.MY_VERSION) {
            ((SDPMessageV3) sdpMessage).setMessageId(new SDPMessageId(messageId));
            ((SDPMessageV3) sdpMessage).setNonce(nonce);
            ((SDPMessageV3) sdpMessage).setAtomicFlag(atomicFlag);
            ((SDPMessageV3) sdpMessage).setTimeoutMeasure(timeoutMeasureEnum);
            ((SDPMessageV3) sdpMessage).setTimeout(timeout);
            if (AtomicFlagEnum.withErrorMsg(atomicFlag)) {
                ((SDPMessageV3) sdpMessage).setErrorMsg(errorMsg);
            }
        }

        return sdpMessage;
    }

    public static ISDPMessage createValidatedBlockStateSDPMsg(CrossChainDomain receiverDomain, BlockState validatedBlockState) {
        SDPMessageV3 sdpMessage = new SDPMessageV3();
        sdpMessage.setTargetDomain(receiverDomain);
        sdpMessage.setTargetIdentity(CrossChainIdentity.ZERO_ID);
        sdpMessage.setSequence(-1);
        sdpMessage.setTimeoutMeasure(TimeoutMeasureEnum.NO_TIMEOUT);
        sdpMessage.setTimeout(BigInteger.ZERO);
        sdpMessage.setMessageId(SDPMessageId.ZERO_MESSAGE_ID);
        sdpMessage.setNonce(-1);
        sdpMessage.setAtomicFlag(AtomicFlagEnum.ACK_RECEIVE_TX_FAILED);
        sdpMessage.setPayload(validatedBlockState.encode());
        sdpMessage.setErrorMsg("");
        return sdpMessage;
    }

    public static ISDPMessage createSDPMessage(int version) {
        return createAbstractSDPMessage(version);
    }

    private static AbstractSDPMessage createAbstractSDPMessage(int version) {
        AbstractSDPMessage sdpMessage;
        switch (version) {
            case SDPMessageV1.MY_VERSION:
                sdpMessage = new SDPMessageV1();
                break;
            case SDPMessageV2.MY_VERSION:
                sdpMessage = new SDPMessageV2();
                break;
            case SDPMessageV3.MY_VERSION:
                sdpMessage = new SDPMessageV3();
                break;
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_SDP_MESSAGE_ERROR,
                        String.format("wrong version: %d", version)
                );
        }

        return sdpMessage;
    }
}
