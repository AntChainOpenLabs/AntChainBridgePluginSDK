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
        }

        return sdpMessage;
    }

    private static ISDPMessage createSDPMessage(int version) {
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
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_SDP_MESSAGE_ERROR,
                        String.format("wrong version: %d", version)
                );
        }

        return sdpMessage;
    }
}
