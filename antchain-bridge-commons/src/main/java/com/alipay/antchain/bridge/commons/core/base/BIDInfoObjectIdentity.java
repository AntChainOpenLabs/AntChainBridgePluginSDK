package com.alipay.antchain.bridge.commons.core.base;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.bif.common.JsonUtils;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BIDInfoObjectIdentity extends ObjectIdentity {
    private BIDDocumentOperation document;

    public BIDInfoObjectIdentity(BIDDocumentOperation bidDocumentOperation) {
        document = bidDocumentOperation;
        setType(ObjectIdentityType.BID);
        setRawId(JsonUtils.toJSONString(bidDocumentOperation).getBytes());
    }

    public BIDInfoObjectIdentity(byte[] rawSubjectBIDInfo) {
        super(ObjectIdentityType.BID, rawSubjectBIDInfo);
    }

    public BIDInfoObjectIdentity(ObjectIdentity objectIdentity) {
        super(objectIdentity.getType(), objectIdentity.getRawId());
    }

    public String getPublicKey() {
        try {
            return document.getPublicKey()[0].getPublicKeyHex();
        } catch (Exception e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.BCDNS_OID_BID_INFO_ERROR,
                    "failed to get public key from subject bid info",
                    e
            );
        }
    }
}
