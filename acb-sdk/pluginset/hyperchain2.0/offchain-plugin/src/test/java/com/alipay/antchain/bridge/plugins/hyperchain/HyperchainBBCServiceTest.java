package com.alipay.antchain.bridge.plugins.hyperchain;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hyperchain.sdk.account.Algo;
import cn.hyperchain.sdk.common.utils.FuncParams;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HyperchainBBCServiceTest {

    private static HyperchainBBCService hyperchainBBCService;
    public static final String SEND_ABI_PATH = "/contract/send/SenderContract.abi";
    public static final String SEND_BIN_PATH = "/contract/send/SenderContract.bin";
    public static final String RECV_ABI_PATH = "/contract/recv/ReceiverContract.abi";
    public static final String RECV_BIN_PATH = "/contract/recv/ReceiverContract.bin";
    public static final String SEND_SETSDPMSGADDRESS_METHOD = "setSdpMSGAddress(address)";
    public static final String SEND_SENDERUNORDERED_METHOD = "sendUnordered(bytes32,string,bytes)";
    public static final String RECV_GETLASTMSG_METHOD = "getLastUnorderedMsg()";
    public static final int RETRY_LIMIT = 10;
    public static final int RETRY_INTERVAL = 1000;

    public static final String HYPERCHAIN_CONFIG_TEMPLATE_FILENAME = "hyperchain2_template.json";
    public static final String HYPERCHAIN_CONFIG_WITHOUT_ACCOUNT_FILENAME = "hyperchain2_withoutAccount.json";

    @Before
    public void init() throws Exception {
        hyperchainBBCService = new HyperchainBBCService();
//        hyperchainBBCService.startup(hyperchainNoContractCtx(HYPERCHAIN_CONFIG_TEMPLATE_FILENAME));
        hyperchainBBCService.startup(hyperchainNoContractCtx(HYPERCHAIN_CONFIG_WITHOUT_ACCOUNT_FILENAME));
    }

    @After
    public void clear() {
        hyperchainBBCService.shutdown();
    }

    // 用于生成账户
    @Test
    public void genAccount() {
        String password = "";
        System.out.printf("gen account json: [%s]",
                hyperchainBBCService.accountService.genAccount(Algo.SMRAW, password));
    }

    @Test
    public void startupTest() {
        Assert.notNull(hyperchainBBCService.account);
        Assert.notNull(hyperchainBBCService.accountService);
        Assert.notNull(hyperchainBBCService.contractService);

        AbstractBBCContext contextBefore = hyperchainBBCService.getContext();
        Assert.isNull(contextBefore.getAuthMessageContract());
    }

    @Test
    public void setupAuthMessageContractTest() {
        AbstractBBCContext contextBefore = hyperchainBBCService.getContext();
        Assert.isNull(contextBefore.getAuthMessageContract());

        hyperchainBBCService.setupAuthMessageContract();

        AbstractBBCContext contextAfter = hyperchainBBCService.getContext();
        Assert.notNull(contextAfter.getAuthMessageContract());
        Assert.notNull(contextAfter.getAuthMessageContract().getContractAddress());
    }

    @Test
    public void setupSDPMessageContractTest() {
        AbstractBBCContext contextBefore = hyperchainBBCService.getContext();
        Assert.isNull(contextBefore.getSdpContract());

        hyperchainBBCService.setupSDPMessageContract();

        AbstractBBCContext contextAfter = hyperchainBBCService.getContext();
        Assert.notNull(contextAfter.getSdpContract());
        Assert.notNull(contextAfter.getSdpContract().getContractAddress());
    }

    @Test
    public void setProtocolTest() {
        AbstractBBCContext context1 = hyperchainBBCService.getContext();
        Assert.isNull(context1.getAuthMessageContract());
        Assert.isNull(context1.getSdpContract());

        hyperchainBBCService.setupAuthMessageContract();
        hyperchainBBCService.setupSDPMessageContract();

        AbstractBBCContext context2 = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getSdpContract().getStatus());

        hyperchainBBCService.setProtocol(context2.getSdpContract().getContractAddress(), "0");

        AbstractBBCContext context3 = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_READY, context3.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context3.getSdpContract().getStatus());
    }

    @Test
    public void setAmContractAndDomainTest() {
        AbstractBBCContext context1 = hyperchainBBCService.getContext();
        Assert.isNull(context1.getAuthMessageContract());
        Assert.isNull(context1.getSdpContract());

        hyperchainBBCService.setupAuthMessageContract();
        hyperchainBBCService.setupSDPMessageContract();

        AbstractBBCContext context2 = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getSdpContract().getStatus());

        hyperchainBBCService.setAmContract(context2.getAuthMessageContract().getContractAddress());

        AbstractBBCContext context3 = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context3.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context3.getSdpContract().getStatus());

        String testDomain = "test.domain";
        hyperchainBBCService.setLocalDomain(testDomain);

        AbstractBBCContext context4 = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context4.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_READY, context4.getSdpContract().getStatus());
    }

    @Test
    public void querySDPMessageSeqTest() {
        AbstractBBCContext context1 = hyperchainBBCService.getContext();
        Assert.isNull(context1.getSdpContract());

        hyperchainBBCService.setupAuthMessageContract();
        hyperchainBBCService.setupSDPMessageContract();

        AbstractBBCContext context2 = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context2.getSdpContract().getStatus());

        hyperchainBBCService.setAmContract(context2.getAuthMessageContract().getContractAddress());
        AbstractBBCContext context3 = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context3.getSdpContract().getStatus());

        String senderDomain = "sender.domain";
        String receiverDomain = "receiver.domain";
        hyperchainBBCService.setLocalDomain(receiverDomain);
        AbstractBBCContext context4 = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_READY, context4.getSdpContract().getStatus());

        Long ret = hyperchainBBCService.querySDPMessageSeq(senderDomain, "from", receiverDomain, "to");
        Assert.equals(0L, ret);
    }


    @Test
    public void readCrossChainMessagesByHeightTest() {
        AbstractBBCContext context = hyperchainBBCService.getContext();
        Assert.isNull(context.getAuthMessageContract());
        Assert.isNull(context.getSdpContract());

        hyperchainBBCService.setupAuthMessageContract();
        hyperchainBBCService.setupSDPMessageContract();

        context = hyperchainBBCService.getContext();
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context.getAuthMessageContract().getStatus());
        Assert.equals(ContractStatusEnum.CONTRACT_DEPLOYED, context.getSdpContract().getStatus());

        hyperchainBBCService.setProtocol(context.getSdpContract().getContractAddress(), "0");
        Assert.equals(ContractStatusEnum.CONTRACT_READY, context.getAuthMessageContract().getStatus());

        List<CrossChainMessage> messageList = hyperchainBBCService.readCrossChainMessagesByHeight(hyperchainBBCService.queryLatestHeight() - 1);
        Assert.equals(0, messageList.size());
    }

    @Test
    public void queryLatestHeightTest() {
        Assert.notEquals(0, hyperchainBBCService.queryLatestHeight());
    }

    @Test
    public void testDemo() throws InterruptedException {
        // 前置准备 =================================
        String sendDomain = "hpc20240523T1.web3";
        String senderSdpAddr = "0x8b82f63dc237b108677dca44a85369e642e7ef2a";
        String recvDomain = "hpc20240523T2.web3";
        String msg = "ant chain bridge";

        // demo 测试 ===============================

        String sendAddr = hyperchainBBCService.deployContract(SEND_BIN_PATH, SEND_ABI_PATH);
        System.out.printf("deploy send app contract %s\n", sendAddr);
        String recvAddr = hyperchainBBCService.deployContract(RECV_BIN_PATH, RECV_ABI_PATH);
        System.out.printf("deploy recv app contract %s\n", recvAddr);

        FuncParams params = new FuncParams();
        params.addParams(senderSdpAddr);
        String ret = hyperchainBBCService.invokeContract(
                sendAddr,
                SEND_ABI_PATH,
                SEND_SETSDPMSGADDRESS_METHOD,
                params);
        System.out.printf("invoke set sdp %s by tx %s\n",
                senderSdpAddr,
                ret.split(",")[0]);

        System.out.printf("wait for add cross chain msg scl: " +
                        "grantDomain %s, " +
                        "grandIdentity 000000000000000000000000%s, " +
                        "ownerDomain %s, " +
                        "ownerIdentity 000000000000000000000000%s\n",
                sendDomain,
                sendAddr.replace("0x", ""),
                recvDomain,
                recvAddr.replace("0x", ""));
        Thread.sleep(RETRY_INTERVAL);

        params = new FuncParams();
        params.addParams(str2bytes32(recvAddr.replace("0x", "000000000000000000000000")));
        params.addParams(recvDomain);
        params.addParams(msg.getBytes(StandardCharsets.UTF_8));
        ret = hyperchainBBCService.invokeContract(
                sendAddr,
                SEND_ABI_PATH,
                SEND_SENDERUNORDERED_METHOD,
                params);
        System.out.printf("invoke send unordered msg by tx %s\n",
                ret.split(",")[0]);

        String res = "";
        for (int i = 0; i < RETRY_LIMIT; i++) {
            ret = hyperchainBBCService.invokeContract(
                    recvAddr,
                    RECV_ABI_PATH,
                    RECV_GETLASTMSG_METHOD,
                    new FuncParams()
            );

            if (StrUtil.isNotEmpty(ret.split(",")[1])) {
                res = new String(HexUtil.decodeHex(ret.split(",")[1].replace("0x", "")));
                if (res.contains(msg)){
                    break;
                }
            }
        }
        System.out.printf("invoke get last msg [%s] by tx %s\n",
                res,
                ret.split(",")[0]);
    }

    public byte[] str2bytes32(String str) {
        if (str.length() != 64) {
            throw new RuntimeException("str {} length is not 64");
        }
        byte[] byteArray = new byte[32];

        for (int i = 0; i < byteArray.length; i++) {
            int index = i * 2;
            // 将每两个十六进制字符转换为一个 byte
            int value = Integer.parseInt(str.substring(index, index + 2), 16);
            byteArray[i] = (byte) value;
        }

        return byteArray;
    }

    private AbstractBBCContext hyperchainNoContractCtx(String chainConfigFileName) throws IOException {
        String jsonStr = HyperchainConfig.readFileJson(chainConfigFileName);
        HyperchainConfig mockConf = HyperchainConfig.fromJsonString(jsonStr);

        AbstractBBCContext mockCtx = new DefaultBBCContext();
        mockCtx.setConfForBlockchainClient(mockConf.toJsonString().getBytes());
        return mockCtx;
    }
}