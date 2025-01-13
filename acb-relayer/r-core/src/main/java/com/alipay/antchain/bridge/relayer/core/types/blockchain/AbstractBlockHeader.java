package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbstractBlockHeader {

    private String product;

    private String blockchainId;

    private long height;

    private byte[] blockHeader;

    private byte[] ext;
}
