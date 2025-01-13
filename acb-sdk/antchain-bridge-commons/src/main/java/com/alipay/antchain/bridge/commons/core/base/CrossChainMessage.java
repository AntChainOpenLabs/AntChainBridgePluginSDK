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

import java.math.BigInteger;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrossChainMessage {

    private static final short TLV_MESSAGE_TYPE = 0;

    private static final short TLV_MESSAGE = 1;

    private static final short TLV_PROVABLE_LEDGER_DATA = 2;

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

        @TLVCreator
        public static CrossChainMessageType parseFromValue(byte value) {
            return parseFromValue((int) value);
        }
    }

    @Getter
    @Setter
    public static class ProvableLedgerData {

        private static final short TLV_HEIGHT = 0;

        private static final short TLV_BLOCK_HASH = 1;

        private static final short TLV_TIMESTAMP = 2;

        private static final short TLV_LEDGER_DATA = 3;

        private static final short TLV_PROOF = 4;

        private static final short TLV_TX_HASH = 5;

        @TLVField(tag = TLV_HEIGHT, type = TLVTypeEnum.VAR_INT)
        private BigInteger height;

        @TLVField(tag = TLV_BLOCK_HASH, type = TLVTypeEnum.BYTES, order = TLV_BLOCK_HASH)
        private byte[] blockHash;

        @TLVField(tag = TLV_TIMESTAMP, type = TLVTypeEnum.UINT64, order = TLV_TIMESTAMP)
        private long timestamp;

        @TLVField(tag = TLV_LEDGER_DATA, type = TLVTypeEnum.BYTES, order = TLV_LEDGER_DATA)
        private byte[] ledgerData;

        @TLVField(tag = TLV_PROOF, type = TLVTypeEnum.BYTES, order = TLV_PROOF)
        private byte[] proof;

        @TLVField(tag = TLV_TX_HASH, type = TLVTypeEnum.BYTES, order = TLV_TX_HASH)
        private byte[] txHash;

        public long getHeight() {
            return height.longValue();
        }

        public void setHeight(long height) {
            this.height = BigInteger.valueOf(height);
        }

        public BigInteger getHeightVal() {
            return this.height;
        }

        public void setHeightVal(BigInteger height) {
            this.height = height;
        }

        public String getBlockHashHex() {
            return HexUtil.encodeHexStr(blockHash);
        }
    }

    public static CrossChainMessage createCrossChainMessage(
            CrossChainMessageType type,
            long height,
            long timestamp,
            byte[] blockHash,
            byte[] message,
            byte[] ledgerData,
            byte[] proof,
            byte[] txHash
    ) {
        return createCrossChainMessage(type, BigInteger.valueOf(height), timestamp, blockHash, message, ledgerData, proof, txHash);
    }

    public static CrossChainMessage createCrossChainMessage(
            CrossChainMessageType type,
            BigInteger height,
            long timestamp,
            byte[] blockHash,
            byte[] message,
            byte[] ledgerData,
            byte[] proof,
            byte[] txHash
    ) {
        CrossChainMessage msg = new CrossChainMessage();
        msg.setType(type);
        msg.setMessage(message);
        ProvableLedgerData provableLedgerData = new ProvableLedgerData();
        provableLedgerData.setLedgerData(ledgerData);
        provableLedgerData.setProof(proof);
        provableLedgerData.setBlockHash(blockHash);
        provableLedgerData.setHeightVal(height);
        provableLedgerData.setTimestamp(timestamp);
        provableLedgerData.setTxHash(txHash);
        msg.setProvableData(provableLedgerData);

        return msg;
    }

    public static CrossChainMessage decode(byte[] raw) {
        return TLVUtils.decode(raw, CrossChainMessage.class);
    }

    @TLVField(tag = TLV_MESSAGE_TYPE, type = TLVTypeEnum.UINT8)
    private CrossChainMessageType type;

    @TLVField(tag = TLV_MESSAGE, type = TLVTypeEnum.BYTES, order = TLV_MESSAGE)
    private byte[] message;

    @TLVField(tag = TLV_PROVABLE_LEDGER_DATA, type = TLVTypeEnum.BYTES, order = TLV_PROVABLE_LEDGER_DATA)
    private ProvableLedgerData provableData;

    public byte[] encode() {
        return TLVUtils.encode(this);
    }
}
