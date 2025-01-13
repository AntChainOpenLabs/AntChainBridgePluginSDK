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

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Relayer请求
 */
@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class RelayerRequest {

    public static final short CURR_REQ_VERSION = 1;

    public static final short TLV_TYPE_RELAYER_REQUEST_TYPE = 0;

    public static final short TLV_TYPE_RELAYER_REQUEST_NODE_ID = 1;

    public static final short TLV_TYPE_RELAYER_REQUEST_RELAYER_CERT = 2;

    public static final short TLV_TYPE_RELAYER_REQUEST_PAYLOAD = 3;

    public static final short TLV_TYPE_RELAYER_REQUEST_SIG_ALGO = 4;

    public static final short TLV_TYPE_RELAYER_REQUEST_SIGNATURE = 5;

    public static final short TLV_TYPE_RELAYER_REQUEST_VERSION = 6;

    public static RelayerRequest decode(byte[] rawData, Class<? extends RelayerRequest> requestClass) {
        return TLVUtils.decode(rawData, requestClass);
    }

    public static RelayerRequest decode(byte[] rawData) {
        return TLVUtils.decode(rawData, RelayerRequest.class);
    }

    public RelayerRequest(
            RelayerRequestType relayerRequestType
    ) {
        this.requestType = relayerRequestType;
    }

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_VERSION, type = TLVTypeEnum.UINT16, order = TLV_TYPE_RELAYER_REQUEST_VERSION)
    @JSONField(serialize = false)
    private short requestVersion = CURR_REQ_VERSION;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_TYPE, type = TLVTypeEnum.UINT8)
    @JSONField(serialize = false)
    private RelayerRequestType requestType;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_NODE_ID, type = TLVTypeEnum.STRING, order = TLV_TYPE_RELAYER_REQUEST_NODE_ID)
    @JSONField(serialize = false)
    private String nodeId;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_RELAYER_CERT, type = TLVTypeEnum.BYTES, order = TLV_TYPE_RELAYER_REQUEST_RELAYER_CERT)
    @JSONField(serialize = false)
    private AbstractCrossChainCertificate senderRelayerCertificate;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_PAYLOAD, type = TLVTypeEnum.BYTES, order = TLV_TYPE_RELAYER_REQUEST_PAYLOAD)
    @JSONField(serialize = false)
    private byte[] requestPayload;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_SIG_ALGO, type = TLVTypeEnum.STRING, order = TLV_TYPE_RELAYER_REQUEST_SIG_ALGO)
    @JSONField(serialize = false)
    private SignAlgoEnum sigAlgo;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_SIGNATURE, type = TLVTypeEnum.BYTES, order = TLV_TYPE_RELAYER_REQUEST_SIGNATURE)
    @JSONField(serialize = false)
    private byte[] signature;

    public byte[] rawEncode() {
        return TLVUtils.encode(this, TLV_TYPE_RELAYER_REQUEST_SIG_ALGO);
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    /**
     * 验签
     *
     * @return
     */
    public boolean verify() {
        try {
            return sigAlgo.getSigner().verify(
                    CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(
                            senderRelayerCertificate
                    ),
                    rawEncode(),
                    signature
            );
        } catch (Exception e) {
            throw new RuntimeException("failed to verify request sig", e);
        }
    }

    public String calcRelayerNodeId() {
        return RelayerNodeInfo.calculateNodeId(senderRelayerCertificate);
    }
}
