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

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SDPMsgCommitResult {

    public final static int TX_MSG_TRUNCATED_LEN = 160;

    public final static int TX_MSG_PREFIX_LEN = 20;

    public final static String TIMEOUT_FAIL_REASON = "msg timeout";

    private Long sdpMsgId;

    private String receiveProduct;

    private String receiveBlockchainId;

    private String txHash;

    private boolean commitSuccess;

    private boolean confirmed;

    private boolean timeout;

    private String failReason;

    private long blockTimestamp;

    public SDPMsgCommitResult(String txHash) {
        this.txHash = txHash;
        this.commitSuccess = true;
        this.failReason = null;
    }

    public SDPMsgCommitResult(String txHash, boolean commitSuccess, String failReason) {
        this.txHash = txHash;
        this.commitSuccess = commitSuccess;
        this.failReason = failReason;
    }

    public SDPMsgCommitResult(String txHash, boolean commitSuccess, boolean timeout) {
        this(txHash, commitSuccess, TIMEOUT_FAIL_REASON);
        this.timeout = timeout;
    }

    public SDPMsgCommitResult(String txHash, boolean confirmed, boolean commitSuccess, String failReason) {
        this.txHash = txHash;
        this.confirmed = confirmed;
        this.commitSuccess = commitSuccess;
        this.failReason = failReason;
    }

    public SDPMsgCommitResult(String receiveProduct, String receiveBlockchainId, String txHash, boolean commitSuccess,
                              String failReason, long blockTimestamp) {
        this.receiveProduct = receiveProduct;
        this.receiveBlockchainId = receiveBlockchainId;
        this.txHash = txHash;
        this.commitSuccess = commitSuccess;
        this.failReason = failReason;
        this.blockTimestamp = blockTimestamp;
    }

    public SDPMsgCommitResult(Long sdpMsgId, String receiveProduct, String receiveBlockchainId, String txHash, boolean commitSuccess,
                              String failReason, long blockTimestamp) {
        this.sdpMsgId = sdpMsgId;
        this.receiveProduct = receiveProduct;
        this.receiveBlockchainId = receiveBlockchainId;
        this.txHash = txHash;
        this.commitSuccess = commitSuccess;
        this.failReason = failReason;
        this.blockTimestamp = blockTimestamp;
    }

    public SDPMsgCommitResult(Long sdpMsgId, String receiveProduct, String receiveBlockchainId, String txHash, boolean commitSuccess,
                              String failReason, long blockTimestamp, boolean timeout) {
        this.sdpMsgId = sdpMsgId;
        this.receiveProduct = receiveProduct;
        this.receiveBlockchainId = receiveBlockchainId;
        this.txHash = txHash;
        this.commitSuccess = commitSuccess;
        this.failReason = failReason;
        this.blockTimestamp = blockTimestamp;
        this.timeout = timeout;
    }

    public String getFailReasonTruncated() {
        if (StrUtil.isNotEmpty(failReason) && failReason.length() > TX_MSG_TRUNCATED_LEN) {
            return StrUtil.truncateUtf8(failReason, TX_MSG_TRUNCATED_LEN);
        }
        return StrUtil.emptyToDefault(failReason, "");
    }

    public SDPMsgProcessStateEnum getProcessState() {
        if (commitSuccess) {
            return SDPMsgProcessStateEnum.TX_SUCCESS;
        }
         if (isTimeout()) {
             return SDPMsgProcessStateEnum.TIMEOUT;
         }
        return SDPMsgProcessStateEnum.TX_FAILED;
    }
}

