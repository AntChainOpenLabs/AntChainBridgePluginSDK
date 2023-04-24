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

package com.alipay.antchain.bridge.commons.core.base;

import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrossChainMessage {

    public enum CrossChainMessageType {
        AUTH_MSG,
        DEVELOPER_DESIGN;

        public static CrossChainMessageType parseFromValue(int value) {
            if (value == AUTH_MSG.ordinal()) {
                return AUTH_MSG;
            } else if (value == DEVELOPER_DESIGN.ordinal()) {
                return DEVELOPER_DESIGN;
            }
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.UNSUPPORTED_CROSS_CHAIN_MESSAGE_TYPE_ERROR,
                    "Invalid value for CrossChainMessageType: " + value
            );
        }
    }

    @Getter
    @Setter
    public static class ProvableLedgerData {

        private long height;

        private byte[] blockHash;

        private long timestamp;

        private byte[] ledgerData;

        private byte[] proof;
    }

    public static CrossChainMessage createCrossChainMessage(
            CrossChainMessageType type,
            long height,
            long timestamp,
            byte[] blockHash,
            byte[] message,
            byte[] ledgerData,
            byte[] proof
    ) {
        CrossChainMessage msg = new CrossChainMessage();
        msg.setType(type);
        msg.setMessage(message);
        ProvableLedgerData provableLedgerData = new ProvableLedgerData();
        provableLedgerData.setLedgerData(ledgerData);
        provableLedgerData.setProof(proof);
        provableLedgerData.setBlockHash(blockHash);
        provableLedgerData.setHeight(height);
        provableLedgerData.setTimestamp(timestamp);
        msg.setProvableData(provableLedgerData);

        return msg;
    }

    private CrossChainMessageType type;

    private byte[] message;

    private ProvableLedgerData provableData;
}
