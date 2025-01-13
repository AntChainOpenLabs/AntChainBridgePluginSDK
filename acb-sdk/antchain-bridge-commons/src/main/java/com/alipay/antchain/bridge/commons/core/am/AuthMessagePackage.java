package com.alipay.antchain.bridge.commons.core.am;

import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyProof;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthMessagePackage {

    private IAuthMessage authMessage;

    private ThirdPartyProof tpProof;

    public byte[] encode() {
        return null;
    }

    public void decode(byte[] raw) {

    }
}
