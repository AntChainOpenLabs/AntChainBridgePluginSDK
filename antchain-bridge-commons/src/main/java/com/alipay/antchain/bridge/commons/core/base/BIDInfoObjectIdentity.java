package com.alipay.antchain.bridge.commons.core.base;

import java.security.PublicKey;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.ac.caict.bid.model.BIDpublicKeyOperation;
import cn.bif.common.JsonUtils;
import cn.bif.module.encryption.model.KeyType;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.BCUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

@Data
@NoArgsConstructor
public class BIDInfoObjectIdentity extends ObjectIdentity implements IObjectIdentityWithPublicKey {
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
        this.document = JsonUtils.toJavaObject(new String(objectIdentity.getRawId()), BIDDocumentOperation.class);
    }

    @Override
    public byte[] getRawPublicKey() {
        BIDpublicKeyOperation biDpublicKeyOperation = document.getPublicKey()[0];
        byte[] rawPubkeyWithSignals = HexUtil.decodeHex(biDpublicKeyOperation.getPublicKeyHex());
        byte[] rawPubkey = new byte[rawPubkeyWithSignals.length - 3];
        System.arraycopy(rawPubkeyWithSignals, 3, rawPubkey, 0, rawPubkey.length);
        return rawPubkey;
    }

    public PublicKey getPublicKey() {
        try {
            BIDpublicKeyOperation biDpublicKeyOperation = document.getPublicKey()[0];
            byte[] rawPubkeyWithSignals = HexUtil.decodeHex(biDpublicKeyOperation.getPublicKeyHex());
            byte[] rawPubkey = new byte[rawPubkeyWithSignals.length - 3];
            System.arraycopy(rawPubkeyWithSignals, 3, rawPubkey, 0, rawPubkey.length);
            if (biDpublicKeyOperation.getType() == KeyType.ED25519) {
                EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
                EdDSAPublicKeySpec eddsaPubKey = new EdDSAPublicKeySpec(rawPubkey, spec);
                return new EdDSAPublicKey(eddsaPubKey);
            } else if (biDpublicKeyOperation.getType() == KeyType.SM2) {
                return BCUtil.decodeECPoint(rawPubkey, "prime256v1");
            }
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.BCDNS_BID_PUBLIC_KEY_ALGO_NOT_SUPPORT,
                    StrUtil.format("the key type of BID is not expected")
            );
        } catch (Exception e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.BCDNS_OID_BID_INFO_ERROR,
                    "failed to get public key from subject bid info",
                    e
            );
        }
    }
}
