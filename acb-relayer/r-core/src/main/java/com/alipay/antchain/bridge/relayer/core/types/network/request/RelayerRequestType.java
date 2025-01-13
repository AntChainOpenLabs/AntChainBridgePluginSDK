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

package com.alipay.antchain.bridge.relayer.core.types.network.request;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Relayer请求类型
 */
@Getter
@AllArgsConstructor
public enum RelayerRequestType {

    GET_RELAYER_NODE_INFO("getRelayerNodeInfo"),

    GET_RELAYER_BLOCKCHAIN_INFO("getBlockchainInfo"),

    GET_RELAYER_BLOCKCHAIN_CONTENT("getRelayerBlockChainContent"),

    PROPAGATE_CROSSCHAIN_MESSAGE("propagateCrossChainMsg"),

    QUERY_CROSSCHAIN_MSG_RECEIPT("queryCrossChainMsgReceipt"),

    HELLO_START("helloStart"),

    HELLO_COMPLETE("helloComplete"),

    CROSSCHAIN_CHANNEL_START("crosschainChannelStart"),

    CROSSCHAIN_CHANNEL_COMPLETE("crosschainChannelComplete");

    private final String code;

    public static RelayerRequestType parseFromValue(String value) {
        if (StrUtil.equals(value, GET_RELAYER_NODE_INFO.code)) {
            return GET_RELAYER_NODE_INFO;
        } else if (StrUtil.equals(value, GET_RELAYER_BLOCKCHAIN_INFO.code)) {
            return GET_RELAYER_BLOCKCHAIN_INFO;
        } else if (StrUtil.equals(value, PROPAGATE_CROSSCHAIN_MESSAGE.code)) {
            return PROPAGATE_CROSSCHAIN_MESSAGE;
        } else if (StrUtil.equals(value, GET_RELAYER_BLOCKCHAIN_CONTENT.code)) {
            return GET_RELAYER_BLOCKCHAIN_CONTENT;
        } else if (StrUtil.equals(value, HELLO_START.code)) {
            return HELLO_START;
        } else if (StrUtil.equals(value, HELLO_COMPLETE.code)) {
            return HELLO_COMPLETE;
        } else if (StrUtil.equals(value, CROSSCHAIN_CHANNEL_START.code)) {
            return CROSSCHAIN_CHANNEL_START;
        } else if (StrUtil.equals(value, CROSSCHAIN_CHANNEL_COMPLETE.code)) {
            return CROSSCHAIN_CHANNEL_COMPLETE;
        } else if (StrUtil.equals(value, QUERY_CROSSCHAIN_MSG_RECEIPT.code)) {
            return QUERY_CROSSCHAIN_MSG_RECEIPT;
        }
        throw new AntChainBridgeRelayerException(
                RelayerErrorCodeEnum.UNKNOWN_INTERNAL_ERROR,
                "Invalid value for relayer request type: " + value
        );
    }

    public static RelayerRequestType valueOf(Byte value) {
        switch (value) {
            case 0:
                return GET_RELAYER_NODE_INFO;
            case 1:
                return GET_RELAYER_BLOCKCHAIN_INFO;
            case 2:
                return GET_RELAYER_BLOCKCHAIN_CONTENT;
            case 3:
                return PROPAGATE_CROSSCHAIN_MESSAGE;
            case 4:
                return QUERY_CROSSCHAIN_MSG_RECEIPT;
            case 5:
                return HELLO_START;
            case 6:
                return HELLO_COMPLETE;
            case 7:
                return CROSSCHAIN_CHANNEL_START;
            case 8:
                return CROSSCHAIN_CHANNEL_COMPLETE;
            default:
                return null;
        }
    }
}
