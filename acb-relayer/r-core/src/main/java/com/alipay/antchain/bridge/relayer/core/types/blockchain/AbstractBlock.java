package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.base.ConsensusState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractBlock {

    @JSONField
    private String product;

    @JSONField
    private String blockchainId;

    @JSONField
    //TODO: replace with type BigInteger
    private long height;

    public abstract byte[] encode();

    public abstract void decode(byte[] data);

    public abstract String getDomain();

    public abstract ConsensusState getConsensusState();
}
