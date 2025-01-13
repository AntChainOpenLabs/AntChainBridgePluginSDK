package com.alipay.antchain.bridge.relayer.commons.exception;

import lombok.Getter;

import java.math.BigInteger;

@Getter
public class BsValidationException extends AntChainBridgeRelayerException{
    public BsValidationException(String domain, byte[] blockHash, BigInteger blockHeight, Long blockTimestamp, String tpbtaLaneKey, String message) {
        super(RelayerErrorCodeEnum.SERVICE_VALIDATION_BS_VERIFY_EXCEPTION,
                "failed to verify block state {}-{}-{} from blockchain {} and tpbta {} : {}",
                blockHash, blockHeight, blockTimestamp, domain, tpbtaLaneKey, message);
        this.domain = domain;
        this.blockHash = blockHash;
        this.blockHeight = blockHeight;
        this.blockTimestamp = blockTimestamp;
        this.tpbtaLaneKey = tpbtaLaneKey;
    }

    private final String domain;

    private final byte[] blockHash;

    private final BigInteger blockHeight;

    private final Long blockTimestamp;

    private final String tpbtaLaneKey;
}
