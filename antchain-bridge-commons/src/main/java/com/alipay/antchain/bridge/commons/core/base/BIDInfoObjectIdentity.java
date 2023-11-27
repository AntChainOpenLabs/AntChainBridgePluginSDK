package com.alipay.antchain.bridge.commons.core.base;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BIDInfoObjectIdentity extends ObjectIdentity {

    private String did;

    public BIDInfoObjectIdentity(String did) {
        super(ObjectIdentityType.BID, did.getBytes());
        this.did = did;
    }

    public BIDInfoObjectIdentity(byte[] rawSubjectBIDInfo) {
        super(ObjectIdentityType.BID, rawSubjectBIDInfo);
        this.did = new String(rawSubjectBIDInfo);
    }
}
