package com.alipay.antchain.bridge.commons.core.ptc;

import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import lombok.Getter;

@Getter
public class ValidatedConsensusStateV1 extends ValidatedConsensusState {

    public static final short MY_VERSION = 1;

    public static ValidatedConsensusState decode(byte[] data) {
        return TLVUtils.decode(data, ValidatedConsensusStateV1.class);
    }

    public ValidatedConsensusStateV1() {
        super();
        setVcsVersion(MY_VERSION);
    }
}
