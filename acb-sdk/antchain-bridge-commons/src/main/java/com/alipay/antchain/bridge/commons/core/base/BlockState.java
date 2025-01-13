package com.alipay.antchain.bridge.commons.core.base;

import java.math.BigInteger;

import com.alipay.antchain.bridge.commons.core.sdp.TimeoutMeasureEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlockState {
    private static final short BS_VERSION = 0x00;
    private static final short BS_DOMAIN = 0x01;
    private static final short BS_HASH = 0x02;
    private static final short BS_HEIGHT = 0x03;
    private static final short BS_TIMESTAMP = 0x04;

    @TLVField(tag = BS_VERSION, type = TLVTypeEnum.UINT16)
    private short bsVersion;

    @TLVField(tag = BS_DOMAIN, type = TLVTypeEnum.STRING, order = BS_DOMAIN)
    private CrossChainDomain domain;

    @TLVField(tag = BS_HASH, type = TLVTypeEnum.BYTES, order = BS_HASH)
    private byte[] hash;

    @TLVField(tag = BS_HEIGHT, type = TLVTypeEnum.VAR_INT, order = BS_HEIGHT)
    private BigInteger height;

    @TLVField(tag = BS_TIMESTAMP, type = TLVTypeEnum.UINT64, order = BS_TIMESTAMP)
    private long timestamp;

    public BlockState(
            CrossChainDomain chainDomain,
            byte[] blockHash,
            BigInteger blockHeight,
            long blockTimestamp
    ) {
        this.bsVersion = 1;
        this.domain = chainDomain;
        this.hash = blockHash;
        this.height = blockHeight;
        this.timestamp = blockTimestamp;
    }

    public BlockState(
            byte[] blockHash,
            BigInteger blockHeight,
            long blockTimestamp
    ) {
        this.bsVersion = 1;
        this.hash = blockHash;
        this.height = blockHeight;
        this.timestamp = blockTimestamp;
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public int compareTo(BlockState blockState, TimeoutMeasureEnum timeoutMeasure) {
        switch (timeoutMeasure) {
            case SENDER_HEIGHT:
            case RECEIVER_HEIGHT:
                return this.height.compareTo(blockState.height);
            case SENDER_TIMESTAMP:
            case RECEIVER_TIMESTAMP:
                return Long.compare(this.timestamp, blockState.timestamp);
            default:
                throw new UnsupportedOperationException("unsupported timeout measure: " + timeoutMeasure);
        }
    }

    public static BlockState decode(byte[] raw) {
        return TLVUtils.decode(raw, BlockState.class);
    }

}
