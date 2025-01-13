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

import java.io.*;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.ObjectIdentityUtil;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class RelayerNodeInfo {

    public static String calculateNodeId(AbstractCrossChainCertificate crossChainCertificate) {
        Assert.equals(
                CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE,
                crossChainCertificate.getType()
        );

        return DigestUtil.sha256Hex(
                RelayerCredentialSubject.decode(
                        crossChainCertificate.getCredentialSubject()
                ).getApplicant().encode()
        );
    }

    @Getter
    @Setter
    public static class RelayerNodeProperties {
        /**
         * 本地对该relayer节点配置的信任根，使用oracleservice模型
         */
        public static String TRUSTED_SERVICE_ID = "trusted_service_id";

        /**
         * relayer节点是否要求ssl
         */
        public static String TLS_REQUIRED = "tls_required";

        /**
         * relayer node info在properties中记录上次本地和该relayer握手的时间
         */
        public static String LAST_TIME_HANDSHAKE = "last_handshake_time";

        public static String RELAYER_REQ_VERSION = "relayer_req_version";

        public static RelayerNodeProperties decodeFromJson(String json) {
            RelayerNodeProperties relayerNodeProperties = new RelayerNodeProperties();
            JSON.parseObject(json).getInnerMap()
                    .forEach((key, value) -> relayerNodeProperties.getProperties().put(key, (String) value));
            return relayerNodeProperties;
        }

        /**
         * Relayer其他属性
         */
        private final Map<String, String> properties = MapUtil.newHashMap();

        public String getTrustedServiceId() {
            return properties.get(TRUSTED_SERVICE_ID);
        }

        public boolean isTLSRequired() {
            return BooleanUtil.toBoolean(properties.getOrDefault(TLS_REQUIRED, "true"));
        }

        public long getLastHandshakeTime() {
            return NumberUtil.parseLong(properties.get(LAST_TIME_HANDSHAKE), 0L);
        }

        public short getRelayerReqVersion() {
            return Short.parseShort(properties.getOrDefault(RELAYER_REQ_VERSION, "0"));
        }

        public void setRelayerReqVersion(short version) {
            properties.put(RELAYER_REQ_VERSION, Short.toString(version));
        }

        public byte[] encode() {
            return JSON.toJSONBytes(properties);
        }

        public String encodeToJson() {
            return JSON.toJSONString(properties);
        }
    }

    public static RelayerNodeInfo decode(byte[] rawData) {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(rawData);
        DataInputStream stream = new DataInputStream(byteArrayInputStream);

        try {
            RelayerNodeInfo info = new RelayerNodeInfo();
            info.setNodeId(stream.readUTF());
            info.setRelayerCrossChainCertificate(
                    CrossChainCertificateFactory.createCrossChainCertificate(
                            Base64.decode(stream.readUTF())
                    )
            );
            info.setRelayerCertId(info.getRelayerCrossChainCertificate().getId());
            Assert.equals(
                    CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE,
                    info.getRelayerCrossChainCertificate().getType()
            );
            info.setRelayerCredentialSubject(
                    RelayerCredentialSubject.decode(
                            info.getRelayerCrossChainCertificate().getCredentialSubject()
                    )
            );

            info.setSigAlgo(SignAlgoEnum.getByName(stream.readUTF()));

            int endpointSize = stream.readInt();

            while (endpointSize > 0) {
                info.addEndpoint(stream.readUTF());
                endpointSize--;
            }

            int domainSize = stream.readInt();

            while (domainSize > 0) {
                info.addDomainIfNotExist(stream.readUTF());
                domainSize--;
            }

            try {
                String rawProperties = stream.readUTF();
                info.setProperties(
                        RelayerNodeProperties.decodeFromJson(rawProperties)
                );
                String rawBlockchainContent = stream.readUTF();
                info.setRelayerBlockchainContent(
                        RelayerBlockchainContent.decodeFromJson(rawBlockchainContent)
                );
            } catch (EOFException e) {
                return info;
            }

            return info;

        } catch (Exception e) {
            throw new RuntimeException("failed to decode relayer node info: ", e);
        }
    }

    //************************************************
    // 基础属性
    //************************************************

    /**
     * 节点id，使用公钥hash的hex值，(不含'0x'前缀)
     */
    private String nodeId;

    private String relayerCertId;

    private AbstractCrossChainCertificate relayerCrossChainCertificate;

    private RelayerCredentialSubject relayerCredentialSubject;

    /**
     * 节点接入点数组 "ip:port"
     */
    private List<String> endpoints = ListUtil.toList();

    /**
     * Relayer支持的domain数组
     */
    private List<String> domains = ListUtil.toList();

    /**
     * Relayer其他属性
     */
    private RelayerNodeProperties properties;

    /**
     * 从properties中的json解析出的RelayerBlockchainInfo
     * 用于缓存，重复利用，修改完后，需要dump回properties
     */
    private RelayerBlockchainContent relayerBlockchainContent;

    //************************************************
    // 其他属性
    //************************************************

    private SignAlgoEnum sigAlgo;

    public RelayerNodeInfo(
            AbstractCrossChainCertificate relayerCrossChainCertificate,
            SignAlgoEnum sigAlgo,
            List<String> endpoints,
            List<String> domains
    ) {
        Assert.equals(CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE, relayerCrossChainCertificate.getType());
        this.nodeId = RelayerNodeInfo.calculateNodeId(relayerCrossChainCertificate);
        this.relayerCertId = relayerCrossChainCertificate.getId();
        this.relayerCrossChainCertificate = relayerCrossChainCertificate;
        this.relayerCredentialSubject = RelayerCredentialSubject.decode(relayerCrossChainCertificate.getCredentialSubject());
        this.sigAlgo = sigAlgo;
        this.endpoints = endpoints;
        this.domains = domains;
        this.properties = new RelayerNodeProperties();
    }

    public RelayerNodeInfo(
            String nodeId,
            AbstractCrossChainCertificate relayerCrossChainCertificate,
            SignAlgoEnum sigAlgo,
            List<String> endpoints,
            List<String> domains
    ) {
        Assert.equals(CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE, relayerCrossChainCertificate.getType());
        this.nodeId = nodeId;
        this.relayerCertId = relayerCrossChainCertificate.getId();
        this.relayerCrossChainCertificate = relayerCrossChainCertificate;
        this.relayerCredentialSubject = RelayerCredentialSubject.decode(relayerCrossChainCertificate.getCredentialSubject());
        this.sigAlgo = sigAlgo;
        this.endpoints = endpoints;
        this.domains = domains;
        this.properties = new RelayerNodeProperties();
    }

    public RelayerNodeInfo(
            String nodeId,
            AbstractCrossChainCertificate relayerCrossChainCertificate,
            RelayerCredentialSubject relayerCredentialSubject,
            SignAlgoEnum sigAlgo,
            List<String> endpoints,
            List<String> domains
    ) {
        this.nodeId = nodeId;
        this.relayerCertId = relayerCrossChainCertificate.getId();
        this.relayerCrossChainCertificate = relayerCrossChainCertificate;
        this.relayerCredentialSubject = relayerCredentialSubject;
        this.sigAlgo = sigAlgo;
        this.endpoints = endpoints;
        this.domains = domains;
        this.properties = new RelayerNodeProperties();
    }

    /**
     * RelayerNodeInfo编码值，用于交换信息时私钥签名
     */
    public byte[] getEncode() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);
        encodeWithPropertiesExcluded(stream);
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] encodeWithProperties() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);
        encodeWithPropertiesExcluded(stream);

        try {
            stream.writeUTF(this.properties.encodeToJson());
            stream.writeUTF(this.relayerBlockchainContent.encodeToJson());
        } catch (Exception e) {
            throw new RuntimeException("failed to encode with properties: ", e);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private void encodeWithPropertiesExcluded(DataOutputStream stream) {
        try {
            stream.writeUTF(nodeId);
            stream.writeUTF(
                    Base64.encode(relayerCrossChainCertificate.encode())
            );
            stream.writeUTF(sigAlgo.getName());

            stream.writeInt(endpoints.size());
            for (String endpoint : endpoints) {
                stream.writeUTF(endpoint);
            }

            stream.writeInt(domains.size());
            for (String domain : domains) {
                stream.writeUTF(domain);
            }

            RelayerNodeProperties temp = new RelayerNodeProperties();
            temp.setRelayerReqVersion(this.properties.getRelayerReqVersion());
            stream.writeUTF(temp.encodeToJson());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void unmarshalProperties(String properties) {
        this.properties = RelayerNodeProperties.decodeFromJson(properties);
    }

    public String marshalProperties() {
        return ObjectUtil.isNull(properties) ? null : JSON.toJSONString(properties.getProperties());
    }

    public void addProperty(String key, String value) {
        this.properties.getProperties().put(key, value);
    }

    public void addEndpoint(String endpoint) {
        this.endpoints.add(endpoint);
    }

    public void addDomainIfNotExist(String domain) {
        if (this.domains.contains(domain)) {
            return;
        }
        this.domains.add(domain);
    }

    public void addDomains(List<String> domains) {
        this.domains.addAll(domains);
    }

    public Long getLastTimeHandshake() {
        return properties.getLastHandshakeTime();
    }

    public PublicKey getPublicKey() {
        return ObjectIdentityUtil.getPublicKeyFromSubject(
                relayerCredentialSubject.getApplicant(),
                relayerCredentialSubject.getSubjectInfo()
        );
    }
}