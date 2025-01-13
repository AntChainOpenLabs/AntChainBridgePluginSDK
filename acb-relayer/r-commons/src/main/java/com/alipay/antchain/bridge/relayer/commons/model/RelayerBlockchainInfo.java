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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 区块链信息
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelayerBlockchainInfo {

    public RelayerBlockchainInfo(
            DomainCertWrapper domainCert,
            List<String> domainSpaceChain,
            String amContractClientAddresses
    ) {
        this.domainCert = domainCert;
        this.domainSpaceChain = domainSpaceChain;
        this.amContractClientAddresses = amContractClientAddresses;
    }

    @JSONField
    private List<String> domainSpaceChain;

    /**
     * 区块链域名证书
     */
    @JSONField(serialize = false, deserialize = false)
    private DomainCertWrapper domainCert;

    @JSONField(serialize = false, deserialize = false)
    private AbstractCrossChainCertificate ptcCrossChainCert;

    @JSONField
    private byte[] tpbta;

    /**
     * 信任的am合约，配置合约的id
     */
    @JSONField
    private String amContractClientAddresses;

    /**
     * 链的拓展特征，比如mychain是否支持TEE等.
     * 方便跨链时候传递一些链的特性，在逻辑上适配不同的链。
     */
    @JSONField
    private Map<String, String> chainFeatures = new HashMap<>();

    /**
     * json编码值
     */
    public String encode() {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(this);
        return jsonObject
                .fluentPut("product", domainCert.getBlockchainProduct())
                .fluentPut("domain", domainCert.getDomain())
                .fluentPut("domainCert", domainCert.getCrossChainCertificate().encode())
                .fluentPut("ptcCrossChainCert", ObjectUtil.isNull(ptcCrossChainCert) ? "" : ptcCrossChainCert.encode())
                .toJSONString();
    }

    /**
     * 从json解码
     *
     * @param jsonData
     * @return
     */
    public static RelayerBlockchainInfo decode(String jsonData) {

        JSONObject jsonObject = JSONObject.parseObject(jsonData);

        AbstractCrossChainCertificate domainCrossChainCert = CrossChainCertificateFactory.createCrossChainCertificate(
                jsonObject.getBytes("domainCert")
        );
        Assert.equals(
                CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE,
                domainCrossChainCert.getType()
        );

        DomainCertWrapper domainCertWrapper = new DomainCertWrapper();
        domainCertWrapper.setBlockchainProduct(jsonObject.getString("product"));
        domainCertWrapper.setDomain(jsonObject.getString("domain"));
        domainCertWrapper.setCrossChainCertificate(
                domainCrossChainCert
        );
        domainCertWrapper.setDomainNameCredentialSubject(
                DomainNameCredentialSubject.decode(domainCrossChainCert.getCredentialSubject())
        );
        byte[] rawPTCCert = jsonObject.getBytes("ptcCrossChainCert");
        AbstractCrossChainCertificate ptcCrossChainCert = null;
        if (ObjectUtil.isNotEmpty(rawPTCCert)) {
            ptcCrossChainCert = CrossChainCertificateFactory.createCrossChainCertificate(
                    jsonObject.getBytes("ptcCrossChainCert")
            );
            Assert.equals(
                    CrossChainCertificateTypeEnum.PROOF_TRANSFORMATION_COMPONENT_CERTIFICATE,
                    ptcCrossChainCert.getType()
            );
        }

        RelayerBlockchainInfo blockchainInfo = jsonObject.toJavaObject(RelayerBlockchainInfo.class);
        blockchainInfo.setDomainCert(domainCertWrapper);
        blockchainInfo.setPtcCrossChainCert(ptcCrossChainCert);
        domainCertWrapper.setDomainSpace(blockchainInfo.getDomainSpaceChain().get(0));

        return blockchainInfo;
    }

    /**
     * 添加链的特性。
     *
     * @param key
     * @param value
     */
    public void addChainFeature(String key, String value) {
        this.chainFeatures.put(key, value);
    }

    public void deleteChainFeature(String key) {
        this.chainFeatures.remove(key);
    }
}
