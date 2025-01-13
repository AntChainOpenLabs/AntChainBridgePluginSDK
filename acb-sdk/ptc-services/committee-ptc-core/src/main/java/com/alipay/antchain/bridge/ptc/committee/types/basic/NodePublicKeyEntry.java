package com.alipay.antchain.bridge.ptc.committee.types.basic;

import java.security.PublicKey;

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.base.X509PubkeyInfoObjectIdentity;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NodePublicKeyEntry {

    private static final short TAG_KEY_ID = 0x00;
    private static final short TAG_RAW_PUBKEY = 0x01;

    public NodePublicKeyEntry(String keyId, PublicKey publicKey) {
        this.keyId = keyId;
        this.setPublicKey(publicKey);
    }

    public NodePublicKeyEntry(String keyId, byte[] rawPublicKey) {
        this.keyId = keyId;
        this.rawPublicKey = rawPublicKey;
    }

    @JSONField(name = "key_id")
    @TLVField(tag = TAG_KEY_ID, type = TLVTypeEnum.STRING)
    private String keyId;

    /**
     * X.509 public key in PEM format
     */
    @JSONField(name = "public_key", serializeUsing = X509PubkeySerializer.class, deserializeUsing = X509PubkeyDeserializer.class)
    private PublicKey publicKey;

    @TLVField(tag = TAG_RAW_PUBKEY, type = TLVTypeEnum.BYTES, order = TAG_RAW_PUBKEY)
    private byte[] rawPublicKey;

    public PublicKey getPublicKey() {
        if (publicKey == null) {
            try {
                publicKey = new X509PubkeyInfoObjectIdentity(rawPublicKey).getPublicKey();
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize public key", e);
            }
        }
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        this.rawPublicKey = publicKey.getEncoded();
    }
}
