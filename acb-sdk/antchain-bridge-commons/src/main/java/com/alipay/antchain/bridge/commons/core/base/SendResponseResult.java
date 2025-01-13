package com.alipay.antchain.bridge.commons.core.base;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SendResponseResult {
    private String txId;
    private boolean confirmed;
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private long txTimestamp;
    private byte[] rawTx;

    public SendResponseResult(String txId, boolean success, String errorCode, String errorMessage) {
        this.txId = txId;
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public SendResponseResult(String txId, boolean confirmed, boolean success, String errorCode, String errorMessage) {
        this.txId = txId;
        this.confirmed = confirmed;
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public SendResponseResult(String txId, boolean success, int errorCode, String errorMessage) {
        this(txId, success, String.valueOf(errorCode), errorMessage);
    }
}
