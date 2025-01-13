package com.alipay.antchain.bridge.ptc.types;

/**
 * We are going to use one byte bits to represent the provided features of a PTC.
 * <p>
 *     For now, the first bit is used to represent whether the PTC supports storage
 *     which save the <b>ValidatedConsensusState</b>, <b>BTA</b>, <b>TpBta</b> and so on.
 * </p>
 */
public class PtcFeatureDescriptor {

    private byte features = 0x00;

    public void enableStorage() {
        features |= 0x01;
    }

    public boolean isStorageEnabled() {
        return (features & 0x01) != 0;
    }
}
