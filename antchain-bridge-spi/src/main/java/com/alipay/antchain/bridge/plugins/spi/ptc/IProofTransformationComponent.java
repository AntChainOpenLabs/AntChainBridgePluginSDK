package com.alipay.antchain.bridge.plugins.spi.ptc;

import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import com.alipay.antchain.bridge.commons.core.ptc.TPProof;

public interface IProofTransformationComponent {

    TPProof verifyUCP(UniformCrosschainPacket ucp);
}
