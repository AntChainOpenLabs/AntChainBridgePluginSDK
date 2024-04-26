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

package com.alipay.antchain.bridge.plugins.fiscobcos;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.AuthMessageContract;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.bbc.syscontract.SDPContract;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.AppContract;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.AuthMsg;
import com.alipay.antchain.bridge.plugins.fiscobcos.abi.SDPMsg;
import lombok.Getter;
import lombok.Setter;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.config.ConfigOption;
import org.fisco.bcos.sdk.v3.config.model.*;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.manager.AssembleTransactionProcessor;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessorFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.*;


public class FISCOBCOSBBCServiceTest {
    private static final String VALID_FILENAME = "config.toml";

    private static final String INVALID_FILENAME = "config-example.toml";

    public static final String abiFile = FISCOBCOSBBCService.class.getClassLoader().getResource("abi").getPath();
    public static final String binFile = FISCOBCOSBBCService.class.getClassLoader().getResource("bin").getPath();

    private static final String CA_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDITCCAgkCFBBmnJbO8ph/5jyDSIg4xVF9xhDcMA0GCSqGSIb3DQEBCwUAMEwx\n" +
            "HDAaBgNVBAMME0ZJU0NPLUJDT1MtZjJiMTUyN2MxHDAaBgNVBAoME0ZJU0NPLUJD\n" +
            "T1MtZjJiMTUyN2MxDjAMBgNVBAsMBWNoYWluMCAXDTI0MDQyMjA2Mzk1MFoYDzIx\n" +
            "MjQwMzI5MDYzOTUwWjBMMRwwGgYDVQQDDBNGSVNDTy1CQ09TLWYyYjE1MjdjMRww\n" +
            "GgYDVQQKDBNGSVNDTy1CQ09TLWYyYjE1MjdjMQ4wDAYDVQQLDAVjaGFpbjCCASIw\n" +
            "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMd1fZyF4EGrOEJf6SL2cqrcaZS6\n" +
            "3UZvKUN0vJMJGAh3OBMe2JLbLl+oSnRjcM7XgroCJf0vqLw71oIL7CV8SIwJJQ0k\n" +
            "/WRR6bPQbZTgNVBw83jlb8Ifj0P2us37dHdTNlvV1jJxkNRCtxjY9mgtn/Ie0MbY\n" +
            "khPUfIUkXU4DvlEep6irISaSkavcUKZoUysjdmJma8JCxfVFMVNfZCFRvoB70Izn\n" +
            "eix6TZFjMT3my4Szo9CM8/Ofo/CQx9IkngK7vqgZRkk0Srb1GNUlUU1nmOMWv1Pf\n" +
            "lEC7cG4l2vRtYrQyoynL1ioXKtJ01+bj/Nh9TojBPKi3tSBKlnkZ8TKiZ9ECAwEA\n" +
            "ATANBgkqhkiG9w0BAQsFAAOCAQEAvJ1jwSr4qftu8hC0ktLNFOQo/TWgAO7lT0dN\n" +
            "aKic9ipZHrnhs8JwdPrOHv44lZcXnKTbQdsDpsHmt+pDWMeY2w0ASsHLjv52Z21h\n" +
            "JcEzkEno/QHYbv5GRSlPeLe1uLe/lORIiwDWX+4ERPHUvyTGq2sDxACbTnVzrbmF\n" +
            "p8eDL9d0169B4Ed+eyUHahON9wV/Cykx6lVoIA/2WrzldPOKWf0cFfXNmdWRHt1s\n" +
            "ssSL/GrvXif1AMCJNKzxsP+e3y/DSlDutUtzMfuU+He6qnW+SlZ+sSpRg+Vf/sfR\n" +
            "+2qITapHVFsC4zoeRbBGjLYXhSsvJlu2YLJdtjWX83nuSMBshA==\n" +
            "-----END CERTIFICATE-----\n";

    private static final String SSL_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDLTCCAhWgAwIBAgIUagPe/fiX+eEUdwAjAMJCORK+6UEwDQYJKoZIhvcNAQEL\n" +
            "BQAwTDEcMBoGA1UEAwwTRklTQ08tQkNPUy1mMmIxNTI3YzEcMBoGA1UECgwTRklT\n" +
            "Q08tQkNPUy1mMmIxNTI3YzEOMAwGA1UECwwFY2hhaW4wIBcNMjQwNDIyMDYzOTUw\n" +
            "WhgPMjEyNDAzMjkwNjM5NTBaMEQxHDAaBgNVBAMME0ZJU0NPLUJDT1MtZjJiMTUy\n" +
            "N2MxEzARBgNVBAoMCmZpc2NvLWJjb3MxDzANBgNVBAsMBmFnZW5jeTCCASIwDQYJ\n" +
            "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAOl/oFwkPHwSGhYiFftwat5aNXNfIf37\n" +
            "ObLs9YFQxi/sM5Pnu5VOuxDa4lBO13RrRadKfvOobhxRWkoUuAjK6T0UzgBrxCEO\n" +
            "p/wGObm9atBir5FKS2EBohf9eo2uQ+c8PqYKZh6K/Hqrl5F0Dlhn+Xo5eFCFFbwc\n" +
            "67Qqmi1V62fusQmOd/LoIJNoa4GKC3hwMjgNgcQk9a7u004y0ylvGyK4ukW76bMb\n" +
            "02knW7Fgi0qDmcnrn6tEYgkSRz7/uv699qW7z34pSbVczL7JBB9NF9lKqZBdpHff\n" +
            "okdrDPQ/idYCQFFXmlM3b3lByutti1QGi76+3vUnVcP+qKMCaEfmXR0CAwEAAaMN\n" +
            "MAswCQYDVR0TBAIwADANBgkqhkiG9w0BAQsFAAOCAQEAc3u4ieA28WiJUVgSWl7R\n" +
            "7pFnRq2J1GUif1Kc9mxLjNQICYCegkBiHdflGN0jz9BH2QbGChF02oErjeDsp1Ao\n" +
            "CnykkGY9cEF3RiPMM1ydnl+P2cXU8dY4MaQdBFWvIv0XmC/qF6oQ3jHODO8a728p\n" +
            "rcOgWfd7xlxK9JT2Tlt3oU8aKQ0+NNPw0SukvtZQ5y5cLvxgyYCabFm9g+c+SkYI\n" +
            "L05CF0tsLv0p2buxGnFTzTmuo5B47/IBYZ/gWHRFpPswPrbTfzsjxt/Aay6E0jy/\n" +
            "sEeomcYLO+sF8Rj9Dfj1W93Q+aKBLzeCzHd+w699ydgKJtd/4yf07zHjxDc1VIrw\n" +
            "Tw==\n" +
            "-----END CERTIFICATE-----\n";

    private static final String SSL_KEY = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDpf6BcJDx8EhoW\n" +
            "IhX7cGreWjVzXyH9+zmy7PWBUMYv7DOT57uVTrsQ2uJQTtd0a0WnSn7zqG4cUVpK\n" +
            "FLgIyuk9FM4Aa8QhDqf8Bjm5vWrQYq+RSkthAaIX/XqNrkPnPD6mCmYeivx6q5eR\n" +
            "dA5YZ/l6OXhQhRW8HOu0KpotVetn7rEJjnfy6CCTaGuBigt4cDI4DYHEJPWu7tNO\n" +
            "MtMpbxsiuLpFu+mzG9NpJ1uxYItKg5nJ65+rRGIJEkc+/7r+vfalu89+KUm1XMy+\n" +
            "yQQfTRfZSqmQXaR336JHawz0P4nWAkBRV5pTN295QcrrbYtUBou+vt71J1XD/qij\n" +
            "AmhH5l0dAgMBAAECggEAB8YuCqNSODdKEw6TeMmnZTvPp8W1FgW1SBXHnsSXtKTC\n" +
            "mtwOTMxQQiFng5D8b23a5dT/IRGsiNjUjr7d2b0XwubcbPspjog0Y5m5dkuuML34\n" +
            "Znf4xoQNZ4sS94Cj1iEVXOXfvIiYG2V3KGqax6q+jak4LkYgESFNX7RDadsAeXdS\n" +
            "uFqu7Mm50pbJU7we8TcmkyRP29c2f1UmsjJPnpBLnOloErf7imoHFkHr9+5pCNQF\n" +
            "xoPbwaMg4gzQ27mfdp5RUpRclE+B/MyfuPLgiQ7iwEbS5OeJoIchZVQoSBm7nv0C\n" +
            "AkmNImCFbtKBqT34pj8uT2BAUJx6eA4U0nrpJnGuaQKBgQD7HXDvrGBGKfm9COoR\n" +
            "xdzNPV9ns616CvZKzEZxo+DoR9a1JPEpfpdHUfWlx0+Vt5akNRunr/b2FaJVWw/5\n" +
            "NHBTuPMn+fLLGK0XEKmK6m2DDLoIfrdxc/FW1cWAnA/+J2KCtemPnyzgErQl/5Vo\n" +
            "+gA2W1tIP/BIreqcVidbnmacgwKBgQDuCnRvsxiPDZ0myLRPa++iqDcEsYsnVpQZ\n" +
            "1KK5LVaRCqGcdfZdX0Ywi3X3ZepdJIR4c4kSymRbJxhQn9UaOVEpveQ07gdLqFjU\n" +
            "PAOcaUSAe6ldSa3hWbkJ7GtmxUuAVZ5zToRmsYAkAl24MRu5rGfhpFIGxJvSmLZS\n" +
            "KMBX0yst3wKBgQDS8KxJ6LcGuYP7810MiPUtwvw9lIWJG2RA+M/D7jGjbZVCnUGn\n" +
            "5ZsWYhbDp2WHEq0MS0Br4DjIBuxSXyhP4mjpK1e2oRP+3z+nPGvvMXXEvBAZyrg2\n" +
            "KXr1wqUhn/cfO95Yho8oAkIkCBIkSUos4LUE9ED9tBgYNV/667QsFieEGQKBgCDI\n" +
            "q9Sec+lv1I784Wh20yAxzrIEycd3MxqDoI2kYuHC9xMXZADkGESjUHHsRWTinKQC\n" +
            "NYSy/zNWpRClkrHz5uu6zW1Ewxh2bRV91nl6Pgb8AQ1qElqRAt0NBJW44ncgU5xJ\n" +
            "2g5Sr/VFpiayDMF7ryryeKGZ/mP4yFN0bVkrKi09AoGBALuQwI3QGdEHdgOKYEB/\n" +
            "I9Fe7wTTIIt7xLdCEKN0YSJth8X8jfwvn3sFV9/ls/CYeg9Yz0azHq17S0Eik7ZV\n" +
            "ed7nL+nrNqhVhyqj03TJmH906564Ei7Jy1xPZ5XeMCgUSPZrCyOCVrFNxUCIRyDy\n" +
            "myZCv4ZXPmYxa47qEAaPXGFP\n" +
            "-----END PRIVATE KEY-----\n";

    private static final String VALID_GROUPID = "group0";

    private static final long WAIT_TIME = 5000;

    private static FISCOBCOSBBCService fiscobcosBBCService;

    private static AppContract appContract;

    private static final String REMOTE_APP_CONTRACT = "0xab6f2a90671fa1b244cd0b3fd8adc3ff22759d06";

    @Before
    public void init() throws Exception {
        fiscobcosBBCService = new FISCOBCOSBBCService();
        FISCOBCOSConfig config = new FISCOBCOSConfig();

        ConfigProperty configProperty = new ConfigProperty();

        // 实例化 cryptoMaterial
        Map<String, Object> cryptoMaterial = new HashMap<>();
        cryptoMaterial.put("useSMCrypto", config.getUseSMCrypto());
        cryptoMaterial.put("disableSsl", config.getDisableSsl());
        configProperty.cryptoMaterial = cryptoMaterial;

        // 实例化 network
        Map<String, Object> network = new HashMap<>();
        network.put("messageTimeout", config.getMessageTimeout());
        network.put("defaultGroup", config.getDefaultGroup());
        network.put("peers", new ArrayList<>(Collections.singletonList(config.getConnectPeer())));
        configProperty.network = network;

        // 实例化 account
        Map<String, Object> account = new HashMap<>();
        account.put("keyStoreDir", config.getKeyStoreDir());
        account.put("accountFileFormat", config.getAccountFileFormat());
        configProperty.account = account;

        // 实例化 threadPool
        Map<String, Object> threadPool = new HashMap<>();
        configProperty.threadPool = threadPool;

        // 实例化 amop
        List<AmopTopic> amop = new ArrayList<>();
        configProperty.amop = amop;

        ConfigOption configOption = new ConfigOption();

        CryptoMaterialConfig cryptoMaterialConfig = new CryptoMaterialConfig();
        cryptoMaterialConfig.setCaCert(CA_CERT);
        cryptoMaterialConfig.setSdkCert(SSL_CERT);
        cryptoMaterialConfig.setSdkPrivateKey(SSL_KEY);
        configOption.setCryptoMaterialConfig(cryptoMaterialConfig);

        configOption.setAccountConfig(new AccountConfig(configProperty));
        configOption.setAmopConfig(new AmopConfig(configProperty));
        configOption.setNetworkConfig(new NetworkConfig(configProperty));
        configOption.setThreadPoolConfig(new ThreadPoolConfig(configProperty));

        configOption.setJniConfig(configOption.generateJniConfig());
        configOption.setConfigProperty(configProperty);

        // Initialize BcosSDK
        BcosSDK sdk = new BcosSDK(configOption);
        // Initialize the client for the group
        Client client = sdk.getClient(VALID_GROUPID);

        appContract = AppContract.deploy(client, client.getCryptoSuite().getCryptoKeyPair());
    }

    @Test
    public void testStart() {
        fiscobcosBBCService.start();
    }

    @Test
    public void testStartup() {
        // start up success
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        Assert.assertEquals(null, fiscobcosBBCService.getBbcContext().getAuthMessageContract());
        Assert.assertEquals(null, fiscobcosBBCService.getBbcContext().getSdpContract());
        // start up failed
        AbstractBBCContext mockInvalidCtx = mockInvalidCtx();
        try {
            fiscobcosBBCService.startup(mockInvalidCtx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStartupWithDeployedContract() {
        // start up a tmp
        AbstractBBCContext mockValidCtx = mockValidCtx();
        FISCOBCOSBBCService fiscobcosBBCServiceTmp = new FISCOBCOSBBCService();
        fiscobcosBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        fiscobcosBBCServiceTmp.setupAuthMessageContract();
        fiscobcosBBCServiceTmp.setupSDPMessageContract();
        String amAddr = fiscobcosBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = fiscobcosBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreDeployedContracts(amAddr, sdpAddr);
        fiscobcosBBCService.startup(ctx);
        Assert.assertEquals(amAddr, fiscobcosBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, fiscobcosBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, fiscobcosBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testStartupWithReadyContract() {
        // start up a tmp fiscobcosBBCService to set up contract
        AbstractBBCContext mockValidCtx = mockValidCtx();
        FISCOBCOSBBCService fiscobcosBBCServiceTmp = new FISCOBCOSBBCService();
        fiscobcosBBCServiceTmp.startup(mockValidCtx);

        // set up am and sdp
        fiscobcosBBCServiceTmp.setupAuthMessageContract();
        fiscobcosBBCServiceTmp.setupSDPMessageContract();
        String amAddr = fiscobcosBBCServiceTmp.getContext().getAuthMessageContract().getContractAddress();
        String sdpAddr = fiscobcosBBCServiceTmp.getContext().getSdpContract().getContractAddress();

        // start up success
        AbstractBBCContext ctx = mockValidCtxWithPreReadyContracts(amAddr, sdpAddr);
        fiscobcosBBCService.startup(ctx);
        Assert.assertEquals(amAddr, fiscobcosBBCService.getBbcContext().getAuthMessageContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, fiscobcosBBCService.getBbcContext().getAuthMessageContract().getStatus());
        Assert.assertEquals(sdpAddr, fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, fiscobcosBBCService.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testShutdown() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        fiscobcosBBCService.shutdown();
    }

    @Test
    public void testGetContext() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertNotNull(ctx);
        Assert.assertEquals(null, ctx.getAuthMessageContract());
    }

    @Test
    public void testQueryLatestHeight() {
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        Assert.assertNotNull(fiscobcosBBCService.queryLatestHeight());
    }

    @Test
    public void testSetupAuthMessageContract() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // get context
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetupSDPMessageContract() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testQuerySDPMessageSeq() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // set the domain
        fiscobcosBBCService.setLocalDomain("receiverDomain");

        // query seq
        long seq = fiscobcosBBCService.querySDPMessageSeq(
                "senderDomain",
                DigestUtil.sha256Hex("senderID"),
                "receiverDomain",
                DigestUtil.sha256Hex("receiverID")
        );
        Assert.assertEquals(0L, seq);
    }

    @Test
    public void testSetProtocol() throws Exception {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);
        AbstractBBCContext ctx;

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // get context
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set protocol to am (sdp type: 0)
        fiscobcosBBCService.setProtocol(
                ctx.getSdpContract().getContractAddress(),
                "0");

        String addr = AuthMsg.load(
                fiscobcosBBCService.getBbcContext().getAuthMessageContract().getContractAddress(),
                fiscobcosBBCService.getClient(),
                fiscobcosBBCService.getKeyPair()
        ).getProtocol(BigInteger.ZERO);
        System.out.printf("protocol: %s\n", addr);

        // check am status
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getAuthMessageContract().getStatus());
    }

    @Test
    public void testSetAmContractAndLocalDomain() throws Exception {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // get context
        AbstractBBCContext ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set am to sdp
        fiscobcosBBCService.setAmContract(ctx.getAuthMessageContract().getContractAddress());

        String amAddr = SDPMsg.load(
                fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress(),
                fiscobcosBBCService.getClient(),
                fiscobcosBBCService.getKeyPair()
        ).getAmAddress();
        System.out.printf("amAddr: %s\n", amAddr);

        // check contract status
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_DEPLOYED, ctx.getSdpContract().getStatus());

        // set the domain
        fiscobcosBBCService.setLocalDomain("receiverDomain");

        byte[] rawDomain = SDPMsg.load(
                fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress(),
                fiscobcosBBCService.getClient(),
                fiscobcosBBCService.getKeyPair()
        ).getLocalDomain();
        System.out.printf("domain: %s\n", HexUtil.encodeHexStr(rawDomain));

        // check contract status
        ctx = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctx.getSdpContract().getStatus());
    }

    @Test
    public void testRelayAuthMessage() throws Exception {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt receipt = fiscobcosBBCService.relayAuthMessage(getRawMsgFromRelayer());
        System.out.println(String.format("sleep %ds for tx to be packaged...", WAIT_TIME / 1000));
        Thread.sleep(WAIT_TIME);

        System.out.println(receipt.getErrorMsg());
        System.out.println(receipt.isSuccessful());

        TransactionReceipt transactionReceipt = fiscobcosBBCService.getClient().getTransactionReceipt(receipt.getTxhash(), false).getTransactionReceipt();
        Assert.assertNotNull(transactionReceipt);
    }

    @Test
    public void testReadCrossChainMessageReceipt() throws IOException, InterruptedException {
        relayAmPrepare();

        // relay am msg
        CrossChainMessageReceipt crossChainMessageReceipt = fiscobcosBBCService.relayAuthMessage(getRawMsgFromRelayer());

        System.out.println(String.format("sleep %ds for tx to be packaged...", WAIT_TIME / 1000));
        Thread.sleep(WAIT_TIME);

        // read receipt by txHash
        CrossChainMessageReceipt crossChainMessageReceipt1 = fiscobcosBBCService.readCrossChainMessageReceipt(crossChainMessageReceipt.getTxhash());
        Assert.assertTrue(crossChainMessageReceipt1.isConfirmed());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendUnordered() throws Exception {
        relayAmPrepare();

        // 1. set sdp addr
        TransactionReceipt receipt = appContract.setProtocol(fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        if (receipt.isStatusOK()) {
            System.out.printf("set protocol(%s) to app contract(%s) \n",
                    appContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                    appContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        // 2. send msg
        try {
            // 2.1 create inputParameters
            List<Object> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes("UnorderedCrossChainMessage".getBytes()));

            // 2.2 async send tx
            AssembleTransactionProcessor transactionProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                    fiscobcosBBCService.getClient(),
                    fiscobcosBBCService.getKeyPair(),
                    this.abiFile,
                    this.binFile
            );
            transactionProcessor.sendTransactionAndGetReceiptByContractLoaderAsync(
                    "AppContract", // contract name
                    appContract.getContractAddress(),  // contract address
                    AppContract.FUNC_SENDUNORDEREDMESSAGE, // function name
                    inputParameters, // input
                    new TransactionCallback() { // callback
                        @Override
                        public void onResponse(TransactionReceipt receipt) {
                            System.out.printf("send unordered msg tx %s\n", receipt.getTransactionHash());
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(
                    "failed to send unordered msg", e
            );
        }

        // 3. query latest height
        long height1 = fiscobcosBBCService.queryLatestHeight();

        System.out.printf("sleep %ds for tx to be packaged...%n", WAIT_TIME / 1000);
        Thread.sleep(WAIT_TIME);

        long height2 = fiscobcosBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for (long i = height1; i <= height2; i++) {
            messageList.addAll(fiscobcosBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    @Test
    public void testReadCrossChainMessagesByHeight_sendOrdered() throws Exception {
        relayAmPrepare();

        // 1. set sdp addr
        TransactionReceipt receipt = appContract.setProtocol(fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        if (receipt.isStatusOK()) {
            System.out.printf("set protocol(%s) to app contract(%s) \n",
                    appContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress());
        } else {
            throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                    appContract.getContractAddress(),
                    fiscobcosBBCService.getBbcContext().getSdpContract().getContractAddress()));
        }

        // 2. send msg
        try {
            // 2.1 create inputParameters
            List<Object> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes("CrossChainMessage".getBytes()));

            // 2.2 async send tx
            AssembleTransactionProcessor transactionProcessor = TransactionProcessorFactory.createAssembleTransactionProcessor(
                    fiscobcosBBCService.getClient(),
                    fiscobcosBBCService.getKeyPair(),
                    this.abiFile,
                    this.binFile
            );
            transactionProcessor.sendTransactionAndGetReceiptByContractLoaderAsync(
                    "AppContract", // contract name
                    appContract.getContractAddress(),  // contract address
                    AppContract.FUNC_SENDMESSAGE, // function name
                    inputParameters, // input
                    new TransactionCallback() { // callback
                        @Override
                        public void onResponse(TransactionReceipt receipt) {
                            System.out.printf("send ordered msg tx %s\n", receipt.getTransactionHash());
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(
                    "failed to send ordered msg", e
            );
        }

        // 3. query latest height
        long height1 = fiscobcosBBCService.queryLatestHeight();

        System.out.printf("sleep %ds for tx to be packaged...%n", WAIT_TIME / 1000);
        Thread.sleep(WAIT_TIME);

        long height2 = fiscobcosBBCService.queryLatestHeight();

        // 4. read cc msg
        List<CrossChainMessage> messageList = ListUtil.toList();
        for (long i = height1; i <= height2; i++) {
            messageList.addAll(fiscobcosBBCService.readCrossChainMessagesByHeight(i));
        }
        Assert.assertEquals(1, messageList.size());
        Assert.assertEquals(CrossChainMessage.CrossChainMessageType.AUTH_MSG, messageList.get(0).getType());
    }

    private void relayAmPrepare() {
        // start up
        AbstractBBCContext mockValidCtx = mockValidCtx();
        fiscobcosBBCService.startup(mockValidCtx);

        // set up am
        fiscobcosBBCService.setupAuthMessageContract();

        // set up sdp
        fiscobcosBBCService.setupSDPMessageContract();

        // set protocol to am (sdp type: 0)
        fiscobcosBBCService.setProtocol(
                mockValidCtx.getSdpContract().getContractAddress(),
                "0");
        System.out.println("sdp address:" + mockValidCtx.getSdpContract().getContractAddress());
        System.out.println("am address:" + mockValidCtx.getAuthMessageContract().getContractAddress());

        // set am to sdp
        fiscobcosBBCService.setAmContract(mockValidCtx.getAuthMessageContract().getContractAddress());

        // set local domain to sdp
        fiscobcosBBCService.setLocalDomain("receiverDomain");

        // check contract ready
        AbstractBBCContext ctxCheck = fiscobcosBBCService.getContext();
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getAuthMessageContract().getStatus());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, ctxCheck.getSdpContract().getStatus());
    }


    private AbstractBBCContext mockValidCtx() {
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setCaCert(CA_CERT);
        mockConf.setSslCert(SSL_CERT);
        mockConf.setSslKey(SSL_KEY);
        mockConf.setGroupID(VALID_GROUPID);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockInvalidCtx() {
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setCaCert(CA_CERT);
        mockConf.setSslCert(SSL_CERT);
        mockConf.setSslKey(SSL_KEY);
        mockConf.setGroupID(VALID_GROUPID);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreDeployedContracts(String amAddr, String sdpAddr) {
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setCaCert(CA_CERT);
        mockConf.setSslCert(SSL_CERT);
        mockConf.setSslKey(SSL_KEY);
        mockConf.setGroupID(VALID_GROUPID);
        mockConf.setAmContractAddressDeployed(amAddr);
        mockConf.setSdpContractAddressDeployed(sdpAddr);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }

    private AbstractBBCContext mockValidCtxWithPreReadyContracts(String amAddr, String sdpAddr) {
        FISCOBCOSConfig mockConf = new FISCOBCOSConfig();
        mockConf.setCaCert(CA_CERT);
        mockConf.setSslCert(SSL_CERT);
        mockConf.setSslKey(SSL_KEY);
        mockConf.setGroupID(VALID_GROUPID);
        mockConf.setAmContractAddressDeployed(amAddr);
        mockConf.setSdpContractAddressDeployed(sdpAddr);
        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());

        AuthMessageContract authMessageContract = new AuthMessageContract();
        authMessageContract.setContractAddress(amAddr);
        authMessageContract.setStatus(ContractStatusEnum.CONTRACT_READY);
        mockCtx.setAuthMessageContract(authMessageContract);

        SDPContract sdpContract = new SDPContract();
        sdpContract.setContractAddress(sdpAddr);
        sdpContract.setStatus(ContractStatusEnum.CONTRACT_READY);
        mockCtx.setSdpContract(sdpContract);

        return mockCtx;
    }

    private byte[] getRawMsgFromRelayer() throws IOException {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "receiverDomain",
                HexUtil.decodeHex(
                        String.format("000000000000000000000000%s", HexUtil.encodeHexStr(RandomUtil.randomBytes(20)))
                ),
                -1,
                "awesome antchain-bridge".getBytes()
        );

        IAuthMessage am = AuthMessageFactory.createAuthMessage(
                1,
                DigestUtil.sha256("senderID"),
                0,
                sdpMessage.encode()
        );

        MockResp resp = new MockResp();
        resp.setRawResponse(am.encode());

        MockProof proof = new MockProof();
        proof.setResp(resp);
        proof.setDomain("senderDomain");

        byte[] rawProof = TLVUtils.encode(proof);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(new byte[]{0, 0, 0, 0});

        int len = rawProof.length;
        stream.write((len >>> 24) & 0xFF);
        stream.write((len >>> 16) & 0xFF);
        stream.write((len >>> 8) & 0xFF);
        stream.write((len) & 0xFF);

        stream.write(rawProof);

        return stream.toByteArray();
    }

    /**
     * Get the sdp message payload from the raw bytes
     * which is the input for {@link com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService#relayAuthMessage(byte[])}
     *
     * @param raw the input for {@link com.alipay.antchain.bridge.plugins.spi.bbc.IBBCService#relayAuthMessage(byte[])}
     * @return {@code byte[]} sdp payload
     */
    private static byte[] getSDPPayloadFromRawMsg(byte[] raw) {
        ByteArrayInputStream stream = new ByteArrayInputStream(raw);

        byte[] zeros = new byte[4];
        stream.read(zeros, 0, 4);

        byte[] rawLen = new byte[4];
        stream.read(rawLen, 0, 4);

        int len = ByteUtil.bytesToInt(rawLen, ByteOrder.BIG_ENDIAN);

        byte[] rawProof = new byte[len];
        stream.read(rawProof, 0, len);

        MockProof proof = TLVUtils.decode(rawProof, MockProof.class);
        IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(proof.getResp().getRawResponse());
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(authMessage.getPayload());

        return sdpMessage.getPayload();
    }

    @Getter
    @Setter
    public static class MockProof {

        @TLVField(tag = 5, type = TLVTypeEnum.BYTES)
        private MockResp resp;

        @TLVField(tag = 9, type = TLVTypeEnum.STRING)
        private String domain;
    }

    @Getter
    @Setter
    public static class MockResp {

        @TLVField(tag = 0, type = TLVTypeEnum.BYTES)
        private byte[] rawResponse;
    }
}
