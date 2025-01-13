package com.alipay.antchain.bridge.plugins.fabric;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.junit.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Slf4j
public class Fabric14ConnectionTest {
    private Fabric14Client fabric14Client;
    private String testChaincodeName = "bizcc";
    final private ChaincodeID testChaincodeID;

    public Fabric14ConnectionTest() {
        testChaincodeID = ChaincodeID.newBuilder().setName(testChaincodeName).build();
    }

    private byte[] getRawMsgFromRelayer(String targetDomain, byte[] targetId, long seq, byte[] payload, byte[] senderID, String senderDomain) throws IOException {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                targetDomain,
                targetId,
                (int)seq,
                payload
        );

        IAuthMessage am = AuthMessageFactory.createAuthMessage(
                1,
                senderID,
                0,
                sdpMessage.encode()
        );

        Fabric14Client.MockResp resp = new Fabric14Client.MockResp();
        resp.setRawResponse(am.encode());

        Fabric14Client.MockProof proof = new Fabric14Client.MockProof();
        proof.setResp(resp);
        proof.setDomain(senderDomain);

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

    private String readFromConf(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        // 使用 try-with-resources 自动关闭资源
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // 这里添加了换行符以保留原始格式
            }
        }catch (Exception e) {
            log.error("read failed {}", e.toString());
        }
        // 删除最后一个追加的换行符（如果需要）
        if (stringBuilder.length() > 0 && stringBuilder.charAt(stringBuilder.length() - 1) == '\n') {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }
    @Before
    public void setUp() {
        String conf = "conf.json";
        String confJson = "";
        // 使用当前线程的类加载器获取 JSON 文件的输入流
        try (InputStream inputStream = Fabric14ConnectionTest.class.getClassLoader().getResourceAsStream(conf)) {
            // 检查输入流是否为 null，这可能是因为找不到文件
            if (inputStream == null) {
                System.out.println("Sorry, unable to find " + conf);
                return;
            }

            confJson = readFromConf(inputStream);

        } catch (Exception e) {
            log.error("read error " + e);
        }

        fabric14Client = new Fabric14Client(confJson, log);
        fabric14Client.start();
    }

    @After
    public void clean() {
        Assert.assertTrue(fabric14Client.shutdown());
    }

    @Test
    public void TestReadPriv() {
        Fabric14User fabric14User = new Fabric14User("test");
        String cert = "-----BEGIN CERTIFICATE-----\\nMIICKDCCAc+gAwIBAgIQc7na9lUQI1ZC1c26e6esZDAKBggqhkjOPQQDAjBzMQsw\\nCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZy\\nYW5jaXNjbzEZMBcGA1UEChMQb3JnMS5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eu\\nb3JnMS5leGFtcGxlLmNvbTAeFw0yNDA1MTYwODU5MDBaFw0zNDA1MTQwODU5MDBa\\nMGsxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1T\\nYW4gRnJhbmNpc2NvMQ4wDAYDVQQLEwVhZG1pbjEfMB0GA1UEAwwWQWRtaW5Ab3Jn\\nMS5leGFtcGxlLmNvbTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABL7xLQtH5Jni\\n5Y+NUIC9BehO02zrCAvAR28zUHM5sAhG862F6wCGe2hbDD+wEZWPOT8vfiHn/VK2\\nKMLY47fz2KOjTTBLMA4GA1UdDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMCsGA1Ud\\nIwQkMCKAIJzrXDDao7mL9kI3m9gTct2nyhDmBcXpPIhR04Fy2W7qMAoGCCqGSM49\\nBAMCA0cAMEQCIH1VMoByRtEmjSEE3jyAxiSauTFOUuPXnrRaE9bG+OPcAiBkA3z3\\ns5gdUicLSCybGIdOgucMshMbedU8GFEwtIGQeA==\\n-----END CERTIFICATE-----\\n";
        String priv = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgQXYnrDKBLOV05yXA\n" +
                "EkfpgBmec2mHBjQYUvn/VSCMBAyhRANCAAS+8S0LR+SZ4uWPjVCAvQXoTtNs6wgL\n" +
                "wEdvM1BzObAIRvOthesAhntoWww/sBGVjzk/L34h5/1StijC2OO389ij\n" +
                "-----END PRIVATE KEY-----";
        fabric14User.setEnrollment(priv, cert);
    }

    @Test
    public void TestQueryFunc() {
        log.info("block number {}", fabric14Client.getLastBlockHeight());
        Collection<String> codenames = fabric14Client.getDiscoveredChaincodeNames();
        for (String codename : codenames) {
            log.info("codename {}", codename);
        }
    }

    @Test
    @Ignore
    public void testInstallChaincode() {
        ChaincodeID targetID = ChaincodeID.newBuilder().setName("bizcc10").setPath("bizcc.tar.gz").build();
        try (InputStream inputStream = Fabric14ConnectionTest.class.getClassLoader().getResourceAsStream(targetID.getPath())) {
            CrossChainMessageReceipt ret = fabric14Client.deployContract(targetID, inputStream) ;
            Assert.assertTrue(ret.isSuccessful());
        } catch (IOException e) {
            log.info(e.toString());
        }
    }

    @Test
    public void testGetLastMsg() {
        ArrayList<String> args = new ArrayList<>();
        Map<String, byte[]> trans = new HashMap<>();
        ArrayList<String> cc_interest = new ArrayList<>();
        int timeout = 10000;
        CrossChainMessageReceipt receipt = fabric14Client.chaincodeInvokeBase(testChaincodeID, "getLastMsg", args, trans, cc_interest, timeout);
        log.info("receipt, isSuccess: {}, error msg: {}, txID {}", receipt.isSuccessful(), receipt.getErrorMsg(), receipt.getTxhash());
        String txID = receipt.getTxhash();
        CrossChainMessageReceipt queryReceipt = fabric14Client.queryTransactionInfo(txID);
        Assert.assertEquals(receipt.getErrorMsg(), queryReceipt.getErrorMsg());
        Assert.assertEquals(receipt.getTxhash(), queryReceipt.getTxhash());
        Assert.assertEquals(receipt.isSuccessful(), queryReceipt.isSuccessful());
        // Assert.assertEquals(receipt.isConfirmed(), queryReceipt.isConfirmed());
        long height = fabric14Client.getLastBlockHeight();
        log.info("block number {}", height);
        List<CrossChainMessage> crossChainMessages = fabric14Client.readCrossChainMessageReceipt(height);
        // do not have cross chain msgs
        Assert.assertTrue(crossChainMessages.size() == 0);
        for (CrossChainMessage crossChainMessage : crossChainMessages) {
            log.info("{} {} {} {}", crossChainMessage.getMessage(), crossChainMessage.getType(), crossChainMessage.getProvableData().getBlockHash(), crossChainMessage.getProvableData().getTxHash());
        }
    }

    @Test
    public void testSendMsg() {
        String destDN = "testDN";
        String desctContractImage = "dest_bizcc";
        String destContractAddr = DigestUtil.sha256Hex(desctContractImage);
        log.info("desctContractImage {} hex1 {}, hex2 {}", desctContractImage, destContractAddr,  DigestUtil.sha256Hex(desctContractImage));
        String message = "testmessage";
        String nonce = "1";
        ArrayList<String> args = new ArrayList<>();
        args.add("crosschain");
        args.add(destDN);
        args.add(destContractAddr);
        args.add(message);
        args.add(nonce);
        Map<String, byte[]> trans = new HashMap<>();
        ArrayList<String> cc_interest = new ArrayList<>();
        int timeout = 10000;
        CrossChainMessageReceipt receipt = fabric14Client.chaincodeInvokeBase(testChaincodeID, "testSendMessage", args, trans, cc_interest, timeout);
        log.info("receipt, isSuccess: {}, error msg: {}", receipt.isSuccessful(), receipt.getErrorMsg());
        String txID = receipt.getTxhash();
        CrossChainMessageReceipt queryReceipt = fabric14Client.queryTransactionInfo(txID);
        Assert.assertEquals(receipt.getErrorMsg(), queryReceipt.getErrorMsg());
        Assert.assertEquals(receipt.getTxhash(), queryReceipt.getTxhash());
        Assert.assertEquals(receipt.isSuccessful(), queryReceipt.isSuccessful());

        long height = fabric14Client.getLastBlockHeight();
        log.info("block number {}", height);
        List<CrossChainMessage> crossChainMessages = fabric14Client.readCrossChainMessageReceipt(height);
        // do not have cross chain msgs
        Assert.assertTrue(crossChainMessages.size() == 1);
        for (CrossChainMessage crossChainMessage : crossChainMessages) {
            try {
                IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(crossChainMessage.getMessage());
                Assert.assertEquals(authMessage.getVersion(), 1);
                Assert.assertEquals(authMessage.getUpperProtocol(), 0);
                Assert.assertEquals(authMessage.getIdentity().toHex(), DigestUtil.sha256Hex(testChaincodeName));
                ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(authMessage.getPayload());
                Assert.assertEquals(new String(sdpMessage.getPayload()), message);
                Assert.assertEquals(sdpMessage.getTargetIdentity().toHex(), destContractAddr);
                Assert.assertEquals(sdpMessage.getTargetDomain().getDomain(), destDN);
            } catch (Exception e) {
                log.info("decode error {}", e.toString());
                Assert.assertTrue(false);
            }

            // 1. first set admin
            {
                args.clear();
                args.add(fabric14Client.getFabricUser().getEnrollment().getCert());
                trans.clear();
                cc_interest.clear();
                CrossChainMessageReceipt set_admin_receipt = fabric14Client.chaincodeInvokeBase(fabric14Client.getOralceChaincodeId(), "setAdmin", args, trans, cc_interest, timeout);
                Assert.assertEquals(set_admin_receipt.isSuccessful(), true);
            }

            // 2. set image invert
            String senderDomain = "senderDomain";
            {
                args.clear();
                args.add("registerSha256Invert");
                args.add(senderDomain);
                trans.clear();
                cc_interest.clear();
                CrossChainMessageReceipt set_admin_receipt = fabric14Client.chaincodeInvokeBase(fabric14Client.getOralceChaincodeId(), "oracleAdminManage", args, trans, cc_interest, timeout);
                Assert.assertEquals(set_admin_receipt.isSuccessful(), true);
            }

            // 3. query image invert
            // {
            //     args.clear();
            //     args.add("querySha256Invert");
            //     args.add(destContractAddr);
            //     trans.clear();
            //     cc_interest.clear();
            //     CrossChainMessageReceipt set_admin_receipt = fabric14Client.chaincodeInvokeBase(fabric14Client.getOralceChaincodeId(), "oracleAdminManage", args, trans, cc_interest, timeout);
            //     Assert.assertEquals(set_admin_receipt.isSuccessful(), true);
            //     log.info("image {}", set_admin_receipt.getErrorMsg());
            // }

            // set expected domain
            {
                fabric14Client.setLocalDomain(destDN);
            }

            long seq = fabric14Client.querySDPMessageSeq(senderDomain, DigestUtil.sha256Hex(testChaincodeName), destContractAddr);
            log.info("sender domain {}, sender contract {}, dest contract {}, seq {}", senderDomain, DigestUtil.sha256Hex(testChaincodeName), destContractAddr, seq);
            try {
                byte[] relayerProof = getRawMsgFromRelayer(destDN, HexUtil.decodeHex(destContractAddr), seq, message.getBytes(), DigestUtil.sha256(testChaincodeName), senderDomain);
                CrossChainMessageReceipt recv_receipt = fabric14Client.recvPkgFromRelayer(relayerProof);
                log.info("recv msg {}", recv_receipt.getErrorMsg());
                Assert.assertEquals(recv_receipt.isSuccessful(), true);
            } catch (IOException e) {
                log.warn("io exception {}", e.toString());
                Assert.assertTrue(false);
            }

        }

    }

    @Test
    public void calcRelayerProof() {
        String destDN = "testDN";
        String desctContractImage = "dest_bizcc";
        String destContractAddr = DigestUtil.sha256Hex(desctContractImage);
        String message = "test relayer";
        String senderDomain = "senderDomain";
        try {
            byte[] relayerProof = getRawMsgFromRelayer(destDN, HexUtil.decodeHex(destContractAddr), 0, message.getBytes(), DigestUtil.sha256Hex(testChaincodeName).getBytes(), senderDomain);
            String res = Base64.getEncoder().encodeToString(relayerProof);
            log.info("res: {}", res);
        } catch (IOException e) {
            Assert.assertTrue(false);
        }
    }
}