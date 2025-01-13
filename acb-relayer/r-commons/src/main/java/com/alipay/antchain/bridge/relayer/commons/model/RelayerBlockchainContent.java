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

import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.bridge.commons.bcdns.*;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.trie.PatriciaTrie;

@Getter
@Setter
@FieldNameConstants
@Slf4j
public class RelayerBlockchainContent {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidationResult {

        private Map<String, RelayerBlockchainInfo> blockchainInfoMapValidated = MapUtil.newHashMap();

        private Map<String, AbstractCrossChainCertificate> domainSpaceValidated = MapUtil.newHashMap();
    }

    public static RelayerBlockchainContent decodeFromJson(String jsonStr) {
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        Map<String, RelayerBlockchainInfo> relayerBlockchainInfoMap = jsonObject.getJSONObject(Fields.relayerBlockchainInfoTrie)
                .entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> RelayerBlockchainInfo.decode((String) entry.getValue())
                ));
        Map<String, AbstractCrossChainCertificate> trustRootCertMap = jsonObject.getJSONObject(Fields.trustRootCertTrie)
                .entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> CrossChainCertificateFactory.createCrossChainCertificate(Base64.decode((String) entry.getValue()))
                ));
        return new RelayerBlockchainContent(
                relayerBlockchainInfoMap,
                trustRootCertMap
        );
    }

    private PatriciaTrie<RelayerBlockchainInfo> relayerBlockchainInfoTrie;

    private PatriciaTrie<AbstractCrossChainCertificate> trustRootCertTrie;

    public RelayerBlockchainContent(
            Map<String, RelayerBlockchainInfo> relayerBlockchainInfoMap,
            Map<String, AbstractCrossChainCertificate> trustRootCertMap
    ) {
        relayerBlockchainInfoTrie = new PatriciaTrie<>(
                relayerBlockchainInfoMap.entrySet().stream()
                        .map(entry -> MapUtil.entry(StrUtil.reverse(entry.getKey()), entry.getValue()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ))
        );
        trustRootCertTrie = new PatriciaTrie<>(
                trustRootCertMap.entrySet().stream()
                        .map(entry -> MapUtil.entry(StrUtil.reverse(entry.getKey()), entry.getValue()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ))
        );
    }

    public String encodeToJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(
                Fields.relayerBlockchainInfoTrie,
                relayerBlockchainInfoTrie.entrySet().stream().collect(
                        Collectors.toMap(
                                entry -> StrUtil.reverse(entry.getKey()),
                                entry -> entry.getValue().encode()
                        )
                )
        );
        jsonObject.put(
                Fields.trustRootCertTrie,
                trustRootCertTrie.entrySet().stream().collect(
                        Collectors.toMap(
                                entry -> StrUtil.reverse(entry.getKey()),
                                entry -> entry.getValue().encode()
                        )
                )
        );
        return jsonObject.toJSONString();
    }

    public RelayerBlockchainInfo getRelayerBlockchainInfo(String domain) {
        return this.relayerBlockchainInfoTrie.get(StrUtil.reverse(domain));
    }

    public AbstractCrossChainCertificate getDomainSpaceCert(String domainSpace) {
        return this.trustRootCertTrie.get(StrUtil.reverse(domainSpace));
    }

    public ValidationResult validate(AbstractCrossChainCertificate trustRootCert) {
        AbstractCrossChainCertificate myRootCert = getDomainSpaceCert(CrossChainDomain.ROOT_DOMAIN_SPACE);
        if (CrossChainCertificateTypeEnum.BCDNS_TRUST_ROOT_CERTIFICATE != myRootCert.getType()) {
            throw new RuntimeException("wrong trust root certificate type: " + myRootCert.getType().name());
        }

        BCDNSTrustRootCredentialSubject validatorSubject = BCDNSTrustRootCredentialSubject.decode(trustRootCert.getCredentialSubject());
        BCDNSTrustRootCredentialSubject myRootSubject = BCDNSTrustRootCredentialSubject.decode(myRootCert.getCredentialSubject());
        if (
                !ArrayUtil.equals(
                        validatorSubject.getBcdnsRootOwner().encode(),
                        myRootSubject.getBcdnsRootOwner().encode()
                )
        ) {
            throw new RuntimeException("owner of BCDNS root is not equal");
        }

        ValidationResult result = new ValidationResult();
        result.setBlockchainInfoMapValidated(
                this.relayerBlockchainInfoTrie.entrySet().stream().filter(
                        entry -> {
                            String domain = StrUtil.reverse(entry.getKey());
                            RelayerBlockchainInfo blockchainInfo = entry.getValue();

                            AbstractCrossChainCertificate parentCert = trustRootCert;
                            for (String domainSpace : ListUtil.sort(blockchainInfo.getDomainSpaceChain(), String::compareTo)) {
                                if (StrUtil.equals(domainSpace, CrossChainDomain.ROOT_DOMAIN_SPACE)) {
                                    continue;
                                }

                                AbstractCrossChainCertificate currentDomainSpaceCert = getDomainSpaceCert(domainSpace);
                                if (result.getDomainSpaceValidated().containsKey(domainSpace)) {
                                    parentCert = currentDomainSpaceCert;
                                    continue;
                                }
                                if (
                                        !validateDomainSpaceCertWithParent(
                                                domainSpace,
                                                currentDomainSpaceCert,
                                                parentCert
                                        )
                                ) {
                                    log.error("proof for domain {} is not valid: domain space {} not pass", domain, domainSpace);
                                    return false;
                                }
                                parentCert = currentDomainSpaceCert;
                                result.getDomainSpaceValidated().put(domainSpace, currentDomainSpaceCert);
                            }

                            return validateDomainSpaceCertWithParent(
                                    domain,
                                    blockchainInfo.getDomainCert().getCrossChainCertificate(),
                                    parentCert
                            );
                        }
                ).collect(Collectors.toMap(
                        entry -> StrUtil.reverse(entry.getKey()),
                        Map.Entry::getValue
                ))
        );

        if (this.relayerBlockchainInfoTrie.size() > result.getBlockchainInfoMapValidated().size()) {
            log.error(
                    "not all domains pass validation ( passed: {}, all: {} )",
                    StrUtil.join(StrUtil.COMMA, result.getBlockchainInfoMapValidated().keySet()),
                    StrUtil.join(StrUtil.COMMA, this.relayerBlockchainInfoTrie.keySet())
            );
        } else {
            log.info("all domains passed validation");
        }

        return result;
    }

    private boolean validateDomainSpaceCertWithParent(
            String domainOrSpace,
            AbstractCrossChainCertificate domainOrSpaceCert,
            AbstractCrossChainCertificate parent
    ) {
        try {
            DomainNameCredentialSubject domainNameCredentialSubject = DomainNameCredentialSubject.decode(domainOrSpaceCert.getCredentialSubject());
            if (
                    !StrUtil.equals(
                            domainNameCredentialSubject.getDomainName().getDomain(),
                            domainOrSpace
                    )
            ) {
                log.error("domain or space name {} not equal with name {} in cert",
                        domainOrSpace, domainNameCredentialSubject.getDomainName().getDomain());
                return false;
            }

            if (
                    !ArrayUtil.equals(
                            domainOrSpaceCert.getIssuer().encode(),
                            parent.getCredentialSubjectInstance().getApplicant().encode()
                    )
            ) {
                log.error(
                        "issuer of domain or space cert {} not equal with {} in parent cert",
                        Base64.encode(domainOrSpaceCert.getIssuer().encode()),
                        Base64.encode(parent.getCredentialSubjectInstance().getApplicant().encode())
                );
                return false;
            }

            if (
                    !domainNameCredentialSubject.getParentDomainSpace().equals(
                            CrossChainCertificateUtil.getCrossChainDomainSpace(parent)
                    )
            ) {
                log.error(
                        "wrong parent of domain or space cert {}, expected is {}, but get {}",
                        Base64.encode(domainOrSpaceCert.getIssuer().encode()),
                        DomainNameCredentialSubject.decode(parent.getCredentialSubject()).getDomainName(),
                        domainNameCredentialSubject.getParentDomainSpace()
                );
                return false;
            }

            return parent.getCredentialSubjectInstance().verifyIssueProof(
                    domainOrSpaceCert.getEncodedToSign(),
                    domainOrSpaceCert.getProof()
            );
        } catch (Exception e) {
            log.error(
                    "Failed to validate crosschain cert {} with parent {} for domain or space {}",
                    domainOrSpaceCert.encodeToBase64(), parent.encodeToBase64(), domainOrSpace,
                    e
            );
            return false;
        }
    }

    public void addRelayerBlockchainContent(RelayerBlockchainContent relayerBlockchainContent) {
        relayerBlockchainInfoTrie.putAll(relayerBlockchainContent.getRelayerBlockchainInfoTrie());
        trustRootCertTrie.putAll(relayerBlockchainContent.getTrustRootCertTrie());
    }
}
