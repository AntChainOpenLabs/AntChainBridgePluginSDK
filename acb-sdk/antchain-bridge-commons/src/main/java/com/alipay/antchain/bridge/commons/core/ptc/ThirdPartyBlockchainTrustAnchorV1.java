package com.alipay.antchain.bridge.commons.core.ptc;

import java.math.BigInteger;

import com.alipay.antchain.bridge.commons.bcdns.PTCCredentialSubject;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ThirdPartyBlockchainTrustAnchorV1 extends ThirdPartyBlockchainTrustAnchor {

    public static final int MY_VERSION = 1;

    public static ThirdPartyBlockchainTrustAnchor decode(byte[] rawData) {
        return TLVUtils.decode(rawData, ThirdPartyBlockchainTrustAnchorV1.class);
    }

    public ThirdPartyBlockchainTrustAnchorV1(
            int tpbtaVersion,
            BigInteger ptcVerifyAnchorVersion,
            PTCCredentialSubject signerPtcCredentialSubject,
            CrossChainLane crossChainLane,
            int btaSubjectVersion,
            HashAlgoEnum ucpMessageHashAlgo,
            byte[] endorseRoot,
            byte[] endorseProof
    ) {
        super(
                MY_VERSION,
                tpbtaVersion,
                ptcVerifyAnchorVersion,
                signerPtcCredentialSubject,
                crossChainLane,
                btaSubjectVersion,
                ucpMessageHashAlgo,
                endorseRoot,
                endorseProof
        );
    }
}
