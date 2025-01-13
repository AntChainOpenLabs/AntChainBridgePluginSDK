package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbstractTransactionWithProof {

    private String product;

    private String blockchainId;

    private long blockHeight;

    private String txHash;

    private byte[] tx;

    private byte[] ext;
}
