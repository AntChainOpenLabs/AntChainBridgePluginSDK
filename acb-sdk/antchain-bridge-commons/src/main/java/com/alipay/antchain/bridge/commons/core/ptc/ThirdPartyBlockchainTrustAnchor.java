/*
 * Copyright 2024 Ant Group
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

package com.alipay.antchain.bridge.commons.core.ptc;

import java.math.BigInteger;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVPacket;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class ThirdPartyBlockchainTrustAnchor {

    /**
     * {@code ThirdPartyBlockchainTrustAnchor} has and only supports three types:
     * <ul>
     *     <li>{@link TypeEnum#BLOCKCHAIN_LEVEL}</li>
     *     <li>{@link TypeEnum#CHANNEL_LEVEL}</li>
     *     <li>{@link TypeEnum#LANE_LEVEL}</li>
     * </ul>
     * <p>
     *     There is a partial order relationship between the three types of cross-chain channels:
     *     {@code BLOCKCHAIN_LEVEL} &gt;= {@code CHANNEL_LEVEL}, {@code CHANNEL_LEVEL} &gt;= {@code LANE_LEVEL}.
     *     There are some principles between different level TpBTA.
     * </p>
     *
     * <ul>
     *     <li>TpBTA must be one of the three types. TpBTA with other cross-chain lane are not supported.</li>
     *     <li>The effective TpBTA of a blockchain domain name is not allowed to have intersections. The selection principle is from large to small.</li>
     *     <li>Registering a high-range TpBTA can cover a low-range TpBTA.</li>
     *     <li>The registered TpBTA is not allowed to modify the range (cross-chain lane).</li>
     *     <li>According to the partial order relationship, select the largest TpBTA that covers the cross-chain message quadruple.</li>
     * </ul>
     */
    public enum TypeEnum {
        /**
         * Blockchain level trust anchor. Crosschain lane of this kind TpBTA has only sender domain as one-element-tuple.
         * <p>
         * This type of trust anchor is used to endorse the sending blockchain.
         * So the {@code crossChainLane} field inside the TpBTA only has sender domain field nonnull.
         * </p>
         */
        BLOCKCHAIN_LEVEL,

        /**
         * Channel level trust anchor. Crosschain lane of this kind TpBTA has sender domain and receiver domain as two-elements-tuple.
         * <p>
         * This type of trust anchor is used to endorse the channel from sending blockchain to receiving blockchain.
         * So the {@code crossChainLane} field inside the TpBTA has sender domain and receiver domain field nonnull.
         * </p>
         */
        CHANNEL_LEVEL,

        /**
         * Lane level trust anchor. Crosschain lane of this kind TpBTA has sender domain, sender id, receiver domain
         * and receiver id as four-elements-tuple.
         * <p>
         * This type of trust anchor is used to endorse the lane from sender contract on sending blockchain
         * to receiver contract on receiving blockchain. So the {@code crossChainLane} field inside the TpBTA
         * has all four fields nonnull.
         * </p>
         */
        LANE_LEVEL;

        public static TypeEnum parseFrom(CrossChainLane crossChainLane) {
            if (
                    ObjectUtil.isAllNotEmpty(
                            crossChainLane.getSenderDomain(),
                            crossChainLane.getReceiverDomain(),
                            crossChainLane.getSenderId(),
                            crossChainLane.getReceiverId()
                    ) && ObjectUtil.isAllNotEmpty(
                            crossChainLane.getSenderDomain(),
                            crossChainLane.getReceiverDomain(),
                            crossChainLane.getSenderId(),
                            crossChainLane.getReceiverId()
                    )
            ) {
                return TypeEnum.LANE_LEVEL;
            }

            if (
                    ObjectUtil.isAllNotEmpty(crossChainLane.getSenderDomain(), crossChainLane.getReceiverDomain())
                            && ObjectUtil.isAllNotEmpty(crossChainLane.getSenderDomain(), crossChainLane.getReceiverDomain())
            ) {
                return TypeEnum.CHANNEL_LEVEL;
            }

            if (ObjectUtil.isAllNotEmpty(crossChainLane.getSenderDomain()) && ObjectUtil.isAllNotEmpty(crossChainLane.getSenderDomain())) {
                return TypeEnum.BLOCKCHAIN_LEVEL;
            }
            throw new IllegalArgumentException("Invalid cross-chain lane: " + crossChainLane.getLaneKey());
        }
    }

    private static final short TPBTA_STRUCT_VERSION = 0;

    private static final short TPBTA_VERSION = 1;

    private static final short TPBTA_PTC_VA_VERSION = 2;

    private static final short TPBTA_SIGNER_PTC_CREDENTIAL_SUBJECT = 3;

    private static final short TPBTA_CROSSCHAIN_LANE = 4;

    private static final short TPBTA_BTA_ENDORSED_SUBJECT_VERSION = 5;

    private static final short TPBTA_UCP_MSG_DIGEST_HASH_ALGO = 6;

    private static final short TPBTA_ENDORSE_ROOT = 7;

    private static final short TPBTA_ENDORSE_PROOF = 0xff;

    public static ThirdPartyBlockchainTrustAnchor decode(byte[] data) {
        int version = TLVPacket.decode(data).getItemForTag(TPBTA_STRUCT_VERSION).getUint32Value();
        if (version == ThirdPartyBlockchainTrustAnchorV1.MY_VERSION) {
            return ThirdPartyBlockchainTrustAnchorV1.decode(data);
        }
        throw new IllegalArgumentException("Unsupported TPBTA version: " + version);
    }

    @TLVField(tag = TPBTA_STRUCT_VERSION, type = TLVTypeEnum.UINT32)
    private int version;

    @TLVField(tag = TPBTA_VERSION, type = TLVTypeEnum.UINT32, order = TPBTA_VERSION)
    private int tpbtaVersion;

    /**
     * which version the verify anchor used to endorse and verify this tpbta
     */
    @TLVField(tag = TPBTA_PTC_VA_VERSION, type = TLVTypeEnum.VAR_INT, order = TPBTA_PTC_VA_VERSION)
    private BigInteger ptcVerifyAnchorVersion;

    @TLVField(tag = TPBTA_SIGNER_PTC_CREDENTIAL_SUBJECT, type = TLVTypeEnum.BYTES, order = TPBTA_SIGNER_PTC_CREDENTIAL_SUBJECT)
    private PTCCredentialSubject signerPtcCredentialSubject;

    @TLVField(tag = TPBTA_CROSSCHAIN_LANE, type = TLVTypeEnum.BYTES, order = TPBTA_CROSSCHAIN_LANE)
    private CrossChainLane crossChainLane;

    @TLVField(tag = TPBTA_BTA_ENDORSED_SUBJECT_VERSION, type = TLVTypeEnum.UINT32, order = TPBTA_BTA_ENDORSED_SUBJECT_VERSION)
    private int btaSubjectVersion;

    @TLVField(tag = TPBTA_UCP_MSG_DIGEST_HASH_ALGO, type = TLVTypeEnum.STRING, order = TPBTA_UCP_MSG_DIGEST_HASH_ALGO)
    private HashAlgoEnum ucpMessageHashAlgo;

    @TLVField(tag = TPBTA_ENDORSE_ROOT, type = TLVTypeEnum.BYTES, order = TPBTA_ENDORSE_ROOT)
    private byte[] endorseRoot;

    @TLVField(tag = TPBTA_ENDORSE_PROOF, type = TLVTypeEnum.BYTES, order = TPBTA_ENDORSE_PROOF)
    private byte[] endorseProof;

    public boolean isMatched(CrossChainLane msgLane) {
        switch (this.type()) {
            case BLOCKCHAIN_LEVEL:
                return this.crossChainLane.getSenderDomain().equals(msgLane.getSenderDomain());
            case CHANNEL_LEVEL:
                return this.crossChainLane.getSenderDomain().equals(msgLane.getSenderDomain())
                        && this.crossChainLane.getReceiverDomain().equals(msgLane.getReceiverDomain());
            case LANE_LEVEL:
                return this.crossChainLane.getSenderDomain().equals(msgLane.getSenderDomain())
                        && this.crossChainLane.getSenderId().equals(msgLane.getSenderId())
                        && this.crossChainLane.getReceiverDomain().equals(msgLane.getReceiverDomain())
                        && this.crossChainLane.getReceiverId().equals(msgLane.getReceiverId());
            default:
                throw new IllegalArgumentException("Unsupported type: " + this.type());
        }
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public byte[] getEncodedToSign() {
        return TLVUtils.encode(this, TPBTA_ENDORSE_ROOT);
    }

    public TypeEnum type() {
        return TypeEnum.parseFrom(crossChainLane);
    }
}
