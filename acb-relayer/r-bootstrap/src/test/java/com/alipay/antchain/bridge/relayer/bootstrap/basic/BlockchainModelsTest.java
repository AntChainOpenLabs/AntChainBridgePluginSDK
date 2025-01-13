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

package com.alipay.antchain.bridge.relayer.bootstrap.basic;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.relayer.commons.constant.OnChainServiceStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainContent;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainInfo;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("all")
public class BlockchainModelsTest {

    private static final String BLOCKCHAIN_META_EXAMPLE = "{\n" +
            "  \"init_block_height\" : \"13947633\",\n" +
            "  \"anchor_runtime_status\" : \"RUNNING\",\n" +
            "  \"sdp_msg_contract_address\" : \"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "  \"is_domain_registered\" : \"true\",\n" +
            "  \"heterogeneous_bbc_context\" : \"{\\\"am_contract\\\":{\\\"contractAddress\\\":\\\"0x72e82e6aa48fca141ceb5914382be199fa514f96\\\",\\\"status\\\":\\\"CONTRACT_READY\\\"},\\\"raw_conf\\\":\\\"eyJ0ZXN0IjoidGVzdCJ9\\\",\\\"sdp_contract\\\":{\\\"contractAddress\\\":\\\"0x098310f3921eb1f7488ee169298e92759caa4e14\\\",\\\"status\\\":\\\"CONTRACT_READY\\\"}}\",\n" +
            "  \"am_service_status\" : \"DEPLOY_FINISHED\",\n" +
            "  \"am_client_contract_address\" : \"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "  \"plugin_server_id\" : \"p-QYj86x8Zd\"\n" +
            "}";

    public static final String BLOCKCHAIN_META_EXAMPLE_OBJ = "{\n" +
            "    \"init_block_height\":\"13947633\",\n" +
            "    \"anchor_runtime_status\":\"RUNNING\",\n" +
            "    \"sdp_msg_contract_address\":\"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "    \"is_domain_registered\":\"true\",\n" +
            "    \"heterogeneous_bbc_context\":{\n" +
            "        \"am_contract\":{\n" +
            "            \"contractAddress\":\"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        },\n" +
            "        \"raw_conf\":\"eyJ0ZXN0IjoidGVzdCJ9\",\n" +
            "        \"sdp_contract\":{\n" +
            "            \"contractAddress\":\"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        },\n" +
            "        \"ptc_contract\":{\n" +
            "            \"contractAddress\":\"0x098310f3921eb1f7488ee169298e92759caa4e15\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"am_service_status\":\"DEPLOY_FINISHED\",\n" +
            "    \"am_client_contract_address\":\"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "    \"ptc_contract_address\":\"0x098310f3921eb1f7488ee169298e92759caa4e15\",\n" +
            "    \"plugin_server_id\":\"p-QYj86x8Zd\"\n" +
            "}";

    private static final String NEW_BBC_CONTEXT = "{\n" +
            "        \"am_contract\":{\n" +
            "            \"contractAddress\":\"1234\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        },\n" +
            "        \"raw_conf\":\"eyJ0ZXN0IjoidGVzdCJ9\",\n" +
            "        \"sdp_contract\":{\n" +
            "            \"contractAddress\":\"1234\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        }\n" +
            "    }";

    private static final String BLOCKCHAIN_META_EXAMPLE_OBJ_LESS_INFO = "{\n" +
            "    \"heterogeneous_bbc_context\":{\n" +
            "        \"am_contract\":{\n" +
            "            \"contractAddress\":\"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        },\n" +
            "        \"raw_conf\":\"eyJ0ZXN0IjoidGVzdCJ9\",\n" +
            "        \"sdp_contract\":{\n" +
            "            \"contractAddress\":\"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"plugin_server_id\":\"p-QYj86x8Zd\"\n" +
            "}";

    private static final String BLOCKCHAIN_META_EXAMPLE_OBJ_EXTRA_NOT_IN_MAP = "{\n" +
            "    \"test\":\"test\",\n" +
            "    \"init_block_height\":\"13947633\",\n" +
            "    \"anchor_runtime_status\":\"RUNNING\",\n" +
            "    \"sdp_msg_contract_address\":\"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "    \"is_domain_registered\":\"true\",\n" +
            "    \"heterogeneous_bbc_context\":{\n" +
            "        \"am_contract\":{\n" +
            "            \"contractAddress\":\"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        },\n" +
            "        \"raw_conf\":\"eyJ0ZXN0IjoidGVzdCJ9\",\n" +
            "        \"sdp_contract\":{\n" +
            "            \"contractAddress\":\"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"am_service_status\":\"DEPLOY_FINISHED\",\n" +
            "    \"am_client_contract_address\":\"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "    \"plugin_server_id\":\"p-QYj86x8Zd\"\n" +
            "}";

    public static AbstractCrossChainCertificate antchainDotComDomainCert;

    public static AbstractCrossChainCertificate catchainDotComDomainCert;

    public static AbstractCrossChainCertificate dogchainDotComDomainCert;

    public static AbstractCrossChainCertificate birdchainDotComDomainCert;

    public static AbstractCrossChainCertificate dotComDomainSpaceCert;

    public static List<AbstractCrossChainCertificate> domainCertList = new ArrayList<>();

    public static Map<String, AbstractCrossChainCertificate> trustRootMap = new HashMap<>();

    public static AbstractCrossChainCertificate relayerCert;

    public static AbstractCrossChainCertificate trustRootCert;

    public static PrivateKey privateKey;

    @BeforeClass
    public static void setup() throws Exception {
        antchainDotComDomainCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                FileUtil.readBytes("cc_certs/antchain.com.crt")
        );
        catchainDotComDomainCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                FileUtil.readBytes("cc_certs/catchain.com.crt")
        );
        dogchainDotComDomainCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                FileUtil.readBytes("cc_certs/dogchain.com.crt")
        );
        birdchainDotComDomainCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                FileUtil.readBytes("cc_certs/birdchain.com.crt")
        );

        domainCertList.add(antchainDotComDomainCert);
        domainCertList.add(catchainDotComDomainCert);
        domainCertList.add(dogchainDotComDomainCert);
        domainCertList.add(birdchainDotComDomainCert);

        relayerCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                FileUtil.readBytes("cc_certs/relayer.crt")
        );
        dotComDomainSpaceCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                FileUtil.readBytes("cc_certs/x.com.crt")
        );
        trustRootCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                FileUtil.readBytes("cc_certs/trust_root.crt")
        );
        privateKey = getLocalPrivateKey("cc_certs/private_key.pem");

        trustRootMap.put(CrossChainDomain.ROOT_DOMAIN_SPACE, trustRootCert);
        trustRootMap.put(
                CrossChainCertificateUtil.getCrossChainDomainSpace(dotComDomainSpaceCert).getDomain(),
                dotComDomainSpaceCert
        );
    }

    public static PrivateKey getLocalPrivateKey(String path) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            return PemUtil.readPemPrivateKey(new ByteArrayInputStream(FileUtil.readBytes(path)));
        } catch (Exception e) {
            byte[] rawPemOb = PemUtil.readPem(new ByteArrayInputStream(FileUtil.readBytes(path)));
            KeyFactory keyFactory = KeyFactory.getInstance(
                    PrivateKeyInfo.getInstance(rawPemOb).getPrivateKeyAlgorithm().getAlgorithm().getId()
            );
            return keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(
                            rawPemOb
                    )
            );
        }
    }

    @Test
    public void testBlockchainPropertiesDeserialization() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE.getBytes());
        Assert.assertNotNull(properties);
        Assert.assertEquals(BlockchainStateEnum.RUNNING, properties.getAnchorRuntimeStatus());
        Assert.assertEquals(OnChainServiceStatusEnum.DEPLOY_FINISHED, properties.getAmServiceStatus());
        Assert.assertNotNull(properties.getBbcContext());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, properties.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testBlockchainPropertiesDeserializationObj() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());
        Assert.assertNotNull(properties);
        Assert.assertEquals(BlockchainStateEnum.RUNNING, properties.getAnchorRuntimeStatus());
        Assert.assertEquals(OnChainServiceStatusEnum.DEPLOY_FINISHED, properties.getAmServiceStatus());
        Assert.assertNotNull(properties.getBbcContext());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, properties.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testBlockchainPropertiesDeserializationObjLessInfo() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE_OBJ_LESS_INFO.getBytes());
        Assert.assertNotNull(properties);
        Assert.assertNull(properties.getAnchorRuntimeStatus());
        Assert.assertNotNull(properties.getBbcContext());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, properties.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testBlockchainPropertiesSerialization() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());
        Assert.assertNotNull(properties);
        System.out.println(new String(properties.encode()));
    }

    @Test
    public void testBlockchainPropertiesDeserializationWithExtra() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE_OBJ_EXTRA_NOT_IN_MAP.getBytes());
        Assert.assertNotNull(properties);
        Assert.assertNotNull(properties.getExtraProperties());
        Assert.assertEquals("test", properties.getExtraProperties().get("test"));
    }

    @Test
    public void testBlockchainMetaUpdateProperty() throws Exception {
        BlockchainMeta blockchainMeta = new BlockchainMeta(
                "chain", "chainid", "", "", BLOCKCHAIN_META_EXAMPLE_OBJ_EXTRA_NOT_IN_MAP.getBytes()
        );
        blockchainMeta.updateProperty("heterogeneous_bbc_context", NEW_BBC_CONTEXT);
        blockchainMeta.updateProperty("anchor_runtime_status", BlockchainStateEnum.STOPPED.getCode());
        blockchainMeta.updateProperty("test", "newtest");
        blockchainMeta.updateProperty("testabc", "testabc");

        Assert.assertEquals(
                "1234",
                blockchainMeta.getProperties().getBbcContext().getSdpContract().getContractAddress()
        );
        Assert.assertEquals(
                "1234",
                blockchainMeta.getProperties().getBbcContext().getAuthMessageContract().getContractAddress()
        );
        Assert.assertEquals(
                "newtest",
                blockchainMeta.getProperties().getExtraProperties().get("test")
        );
        Assert.assertEquals(
                "testabc",
                blockchainMeta.getProperties().getExtraProperties().get("testabc")
        );
    }

    @Test
    public void testRelayerBlockchainInfo() {

        DomainCertWrapper domainCertWrapper = new DomainCertWrapper(
                antchainDotComDomainCert,
                DomainNameCredentialSubject.decode(antchainDotComDomainCert.getCredentialSubject()),
                "mychain",
                "antchain.com.id",
                "antchain.com",
                ".com"
        );

        RelayerBlockchainInfo relayerBlockchainInfo = new RelayerBlockchainInfo();
        relayerBlockchainInfo.setDomainCert(domainCertWrapper);
        relayerBlockchainInfo.setDomainSpaceChain(ListUtil.toList(".com"));
        relayerBlockchainInfo.setAmContractClientAddresses("0xda216434d379c95db9e80edb2566abaaac467429ce63d92cfb2c5af338f65f52");

        String rawInfo = relayerBlockchainInfo.encode();
        System.out.println(rawInfo);

        RelayerBlockchainInfo relayerBlockchainInfo1 = RelayerBlockchainInfo.decode(rawInfo);
        Assert.assertNotNull(relayerBlockchainInfo1);
        Assert.assertEquals(
                relayerBlockchainInfo.getDomainCert().getDomain(),
                relayerBlockchainInfo1.getDomainCert().getDomain()
        );
    }

    @Test
    public void testRelayerBlockchainContent() {

        RelayerBlockchainContent relayerBlockchainContent = new RelayerBlockchainContent(
                domainCertList.stream()
                        .map(
                                cert -> new RelayerBlockchainInfo(
                                        new DomainCertWrapper(
                                                cert,
                                                DomainNameCredentialSubject.decode(cert.getCredentialSubject()),
                                                "mychain",
                                                CrossChainCertificateUtil.getCrossChainDomain(cert).getDomain() + ".id",
                                                CrossChainCertificateUtil.getCrossChainDomain(cert).getDomain(),
                                                CrossChainCertificateUtil.getCrossChainDomainSpace(dotComDomainSpaceCert).getDomain()
                                        ),
                                        ListUtil.toList(".com"),
                                        HexUtil.encodeHexStr(RandomUtil.randomBytes(32))
                                )
                        ).collect(Collectors.toMap(
                                info -> info.getDomainCert().getDomain(),
                                info -> info
                        )),
                trustRootMap
        );

        Assert.assertEquals(
                CrossChainCertificateUtil.getCrossChainDomain(antchainDotComDomainCert).getDomain(),
                relayerBlockchainContent.getRelayerBlockchainInfo(
                        CrossChainCertificateUtil.getCrossChainDomain(antchainDotComDomainCert).getDomain()
                ).getDomainCert().getDomain()
        );
        String rawContent = relayerBlockchainContent.encodeToJson();
        Assert.assertNotNull(rawContent);
        System.out.println(rawContent);

        RelayerBlockchainContent relayerBlockchainContent1 = RelayerBlockchainContent.decodeFromJson(rawContent);
        Assert.assertNotNull(relayerBlockchainContent1);
        Assert.assertEquals(
                relayerBlockchainContent.getRelayerBlockchainInfo(
                        CrossChainCertificateUtil.getCrossChainDomain(catchainDotComDomainCert).getDomain()
                ).getDomainCert().getDomain(),
                relayerBlockchainContent1.getRelayerBlockchainInfo(
                        CrossChainCertificateUtil.getCrossChainDomain(catchainDotComDomainCert).getDomain()
                ).getDomainCert().getDomain()
        );

        AbstractCrossChainCertificate domainSpaceCert = relayerBlockchainContent1.getDomainSpaceCert(
                CrossChainCertificateUtil.getCrossChainDomainSpace(dotComDomainSpaceCert).getDomain()
        );
        Assert.assertNotNull(domainSpaceCert);
        Assert.assertEquals(
                dotComDomainSpaceCert.getId(),
                domainSpaceCert.getId()
        );
    }
}
