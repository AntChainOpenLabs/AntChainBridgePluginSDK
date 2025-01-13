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

import java.util.Map;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgTrustLevelEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthMsgWrapper {

    public static String AM_TX_ID = "AM_TX_ID";
    public static String AM_HETEROGENEOUS_RANDOM_UUID = "AM_HETEROGENEOUS_RANDOM_UUID"; // only for heterogeneous chain
    public static String AM_BLOCK_HEIGHT = "AM_BLOCK_HEIGHT";
    public static String AM_BLOCK_HASH = "AM_BLOCK_HASH";
    public static String AM_RECEIPT_INDEX = "AM_RECEIPT_INDEX";
    public static String AM_HINTS = "AM_HINTS";
    public static String AM_BLOCK_TIMESTAMP = "AM_BLOCK_TIMESTAMP";    // 链上时间戳
    public static String AM_CAPTURE_TIMESTAMP = "AM_CAPTURE_TIMESTAMP"; // 监听时间戳
    public static String AM_SENDER_GAS_USED = "AM_SENDER_GAS_USED";

    public static AuthMsgWrapper buildFrom(
            String product,
            String blockchainId,
            String domain,
            String ucpId,
            CrossChainMessage crossChainMessage
    ) {
        if (CrossChainMessage.CrossChainMessageType.AUTH_MSG != crossChainMessage.getType()) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.UNKNOWN_INTERNAL_ERROR,
                    "not a valid auth message type: " + crossChainMessage.getType().name()
            );
        }
        return buildFrom(
                product,
                blockchainId,
                domain,
                ucpId,
                AuthMessageFactory.createAuthMessage(crossChainMessage.getMessage())
        );
    }

    public static AuthMsgWrapper buildFrom(
            String product,
            String blockchainId,
            String domain,
            String ucpId,
            IAuthMessage authMessage
    ) {
        AuthMsgWrapper wrapper = new AuthMsgWrapper();
        wrapper.setUcpId(ucpId);
        wrapper.setAuthMessage(authMessage);
        wrapper.setMsgSender(authMessage.getIdentity().toHex());
        wrapper.setProduct(product);
        wrapper.setBlockchainId(blockchainId);
        wrapper.setDomain(domain);
        wrapper.setVersion(authMessage.getVersion());
        wrapper.setProtocolType(UpperProtocolTypeBeyondAMEnum.parseFromValue(authMessage.getUpperProtocol()));
        wrapper.setTrustLevel(
                authMessage.getVersion() >= 2 ?
                        AuthMsgTrustLevelEnum.parseFromValue(((AuthMessageV2) authMessage).getTrustLevel().ordinal())
                        : AuthMsgTrustLevelEnum.NEGATIVE_TRUST
        );
        return wrapper;
    }

    private long authMsgId;

    private String product = StrUtil.EMPTY;

    private String blockchainId = StrUtil.EMPTY;

    private String domain;

    private String ucpId;

    private String amClientContractAddress;

    private int version;

    private String msgSender;

    private UpperProtocolTypeBeyondAMEnum protocolType;

    private AuthMsgTrustLevelEnum trustLevel;

    private AuthMsgProcessStateEnum processState;

    private int failCount;

    private IAuthMessage authMessage;

    private Map<String, String> ledgerInfo = MapUtil.newHashMap();

    private boolean isNetworkAM;

    public AuthMsgWrapper(
            String product,
            String blockchainId,
            String domain,
            String ucpId,
            String amClientContractAddress,
            AuthMsgProcessStateEnum processState,
            int failCount,
            IAuthMessage authMessage
    ) {
        this(0, product, blockchainId, domain, ucpId, amClientContractAddress, processState, failCount, null, authMessage);
    }

    public AuthMsgWrapper(
            long authMsgId,
            String product,
            String blockchainId,
            String domain,
            String ucpId,
            String amClientContractAddress,
            AuthMsgProcessStateEnum processState,
            int failCount,
            byte[] rawLedgerInfo,
            IAuthMessage authMessage
    ) {
        this.authMsgId = authMsgId;
        this.authMessage = authMessage;
        this.product = product;
        this.domain = domain;
        this.blockchainId = blockchainId;
        this.ucpId = ucpId;
        this.amClientContractAddress = amClientContractAddress;
        this.version = authMessage.getVersion();
        this.msgSender = authMessage.getIdentity().toHex();
        this.protocolType = UpperProtocolTypeBeyondAMEnum.parseFromValue(authMessage.getUpperProtocol());
        this.processState = processState;
        this.failCount = failCount;
        this.trustLevel = authMessage.getVersion() >= 2 ?
                AuthMsgTrustLevelEnum.parseFromValue(((AuthMessageV2) authMessage).getTrustLevel().ordinal())
                : AuthMsgTrustLevelEnum.NEGATIVE_TRUST;
        if (ObjectUtil.isNotEmpty(rawLedgerInfo)) {
            this.setLedgerInfo(new String(rawLedgerInfo));
        }
    }

    public byte[] getPayload() {
        return this.authMessage.getPayload();
    }

    public String getUcpIdHex() {
        return HexUtil.encodeHexStr(this.getUcpId());
    }

    public byte[] getRawLedgerInfo() {
        return ledgerInfo.isEmpty() ? null : JSON.toJSONBytes(ledgerInfo);
    }

    public void addLedgerInfo(String key, String value) {
        this.ledgerInfo.put(key, value);
    }

    public void setLedgerInfo(String raw) {
        if (StrUtil.isNotEmpty(raw)) {
            JSON.parseObject(raw).forEach((key, value) -> ledgerInfo.put(key, (String) value));
        }
    }
}
