/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.commons.bcdns.utils;

import java.io.ByteArrayInputStream;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;

public class CrossChainCertificateUtil {

    public static String formatCrossChainCertificateToPem(AbstractCrossChainCertificate certificate) {
        switch (certificate.getType()) {
            case RELAYER_CERTIFICATE:
                return PemUtil.toPem(
                        StrUtil.replace(CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE.name(), "_", " "),
                        certificate.encode()
                );
            case PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE:
                return PemUtil.toPem(
                        StrUtil.replace(CrossChainCertificateTypeEnum.PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE.name(), "_", " "),
                        certificate.encode()
                );
            case DOMAIN_NAME_CERTIFICATE:
                return PemUtil.toPem(
                        StrUtil.replace(CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE.name(), "_", " "),
                        certificate.encode()
                );
            case BCDNS_TRUST_ROOT_CERTIFICATE:
                return PemUtil.toPem(
                        StrUtil.replace(CrossChainCertificateTypeEnum.BCDNS_TRUST_ROOT_CERTIFICATE.name(), "_", " "),
                        certificate.encode()
                );
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.BCDNS_UNSUPPORTED_CA_TYPE,
                        "unsupported crosschain certificate type"
                );
        }
    }

    public AbstractCrossChainCertificate readCrossChainCertificateFromPem(byte[] rawPem) {
        return CrossChainCertificateFactory.createCrossChainCertificate(PemUtil.readPem(new ByteArrayInputStream(rawPem)));
    }
}
