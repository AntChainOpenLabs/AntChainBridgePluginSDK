package com.ali.antchain.testers;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.ali.antchain.abi.AppContract;
import com.ali.antchain.abi.AuthMsg;
import com.ali.antchain.abstarct.AbstractTester;

import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.junit.Assert;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class EthTester extends AbstractTester {

    private static final String WEB3J = "web3j";
    private static final String CREDENTIALS = "credentials";
    private static final String TRANSACTIONMANAGER = "rawTransactionManager";

    // TODO
    private static final String REMOTE_APP_CONTRACT = "0xdd11AA371492B94AB8CDEdf076F84ECCa72820e1";

    private static final int MAX_TX_RESULT_QUERY_TIME = 100;

    Web3j web3jClient;

    Credentials credentials;

    RawTransactionManager rawTransactionManager;

    // bbcService 启动后传入才是有意义的
    public EthTester(AbstractBBCService started_service) {

        try {
            Class<?> serviceClazz = started_service.getClass();

            // 1. 从 service 中获取 web3j 客户端
            Field web3jField = serviceClazz.getDeclaredField(WEB3J);
            // 允许访问私有字段
            web3jField.setAccessible(true);
            Object web3jObj = web3jField.get(started_service);
            if (web3jObj instanceof Web3j) {
                web3jClient = (Web3j) web3jObj;
            }

            // 2. 从 service 中获取 credentials
            Field credentialsField = serviceClazz.getDeclaredField(CREDENTIALS);
            credentialsField.setAccessible(true);
            Object credentialsObj = credentialsField.get(started_service);
            if (credentialsObj instanceof Credentials) {
                credentials = (Credentials) credentialsObj;
            }

            // 3. 从 service 中获取 rawTransactionManager
            Field rawTransactionManagerField = serviceClazz.getDeclaredField(TRANSACTIONMANAGER);
            rawTransactionManagerField.setAccessible(true);
            Object rawTransactionManagerObj = rawTransactionManagerField.get(started_service);
            if (rawTransactionManagerObj instanceof TransactionManager) {
                rawTransactionManager = (RawTransactionManager) rawTransactionManagerObj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getProtocol(String amContractAddr) {
        try {
            String protocolAddr = AuthMsg.load(
                            amContractAddr,
                            web3jClient,
                            credentials,
                            new DefaultGasProvider())
                    .getProtocol(BigInteger.ZERO)
                    .send();

            return protocolAddr;
        } catch (Exception e) {
            getBbcLogger().error("get protocol exception,", e);
        }

        return StrUtil.EMPTY;
    }

    @Override
    public byte[] deployApp(String protocolAddr) {
        try {
            // 1. 部署合约
            AppContract appContract = AppContract.deploy(
                    web3jClient,
                    rawTransactionManager,
                    new DefaultGasProvider()
            ).send();
            byte[] appContractAddr = HexUtil.decodeHex(String.format("000000000000000000000000%s", StrUtil.removePrefix(appContract.getContractAddress(), "0x")));


            // 2. 设置app合约中的protocol合约地址
            TransactionReceipt receipt = appContract.setProtocol(protocolAddr).send();
            if (receipt.isStatusOK()) {
                getBbcLogger().info("set protocol({}) to app contract({})",
                        appContract.getContractAddress(),
                        protocolAddr);
            } else {
                throw new Exception(String.format("failed to set protocol(%s) to app contract(%s)",
                        appContract.getContractAddress(),
                        protocolAddr));
            }

            return appContractAddr;
        } catch (Exception e) {
            getBbcLogger().error("", e);
        }

        return null;
    }


    @Override
    public String sendMsg(AbstractBBCService service,String sendtype) {

        String txhash = "";
        try {
            // 1. create function
            List<Type> inputParameters = new ArrayList<>();
            inputParameters.add(new Utf8String("remoteDomain"));
            inputParameters.add(new Bytes32(DigestUtil.sha256(REMOTE_APP_CONTRACT)));
            inputParameters.add(new DynamicBytes(sendtype.getBytes()));
            Function function = new Function(
                    AppContract.FUNC_SENDUNORDEREDMESSAGE, // function name
                    inputParameters, // inputs
                    Collections.emptyList() // outputs
            );
            String encodedFunc = FunctionEncoder.encode(function);

            // 获取service的class
            Class<?> serviceClazz = service.getClass();

            // 2.1 从 service 中获取 web3j
            Field web3jField = null;
            web3jField = serviceClazz.getDeclaredField(WEB3J);

            // 允许访问私有字段
            web3jField.setAccessible(true);
            Object web3jObj = null;
            web3jObj = web3jField.get(service);
            if (web3jObj instanceof Web3j) {
                web3jClient = (Web3j) web3jObj;
            }

            // 2.2 从 service 中获取 credentials
            Field credentialsField = serviceClazz.getDeclaredField(CREDENTIALS);
            credentialsField.setAccessible(true);
            Object credentialsObj = credentialsField.get(service);

            if (credentialsObj instanceof Credentials) {
                credentials = (Credentials) credentialsObj;
            }

            // 2.3 部署app合约
            AppContract appContract = AppContract.deploy(
                    web3jClient,
                    rawTransactionManager,
                    new DefaultGasProvider()
            ).send();

            // 2.4 调用ethcall
            EthCall call = web3jClient.ethCall(
                    Transaction.createEthCallTransaction(
                            credentials.getAddress(),
                            appContract.getContractAddress(),
                            encodedFunc
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            // 2.5 获取rawTransactionManager
            Field rawTransactionManagerField = serviceClazz.getDeclaredField(TRANSACTIONMANAGER);
            rawTransactionManagerField.setAccessible(true);
            Object rawTransactionManagerObj = rawTransactionManagerField.get(service);
            if (rawTransactionManagerObj instanceof TransactionManager) {
                rawTransactionManager = (RawTransactionManager) rawTransactionManagerObj;
            }

            // 2.6 发送交易
            EthSendTransaction ethSendTransaction = rawTransactionManager.sendTransaction(
                    BigInteger.valueOf(2300000000L),
                    BigInteger.valueOf(3000000),
                    appContract.getContractAddress(),
                    encodedFunc,
                    BigInteger.ZERO
            );
            txhash = ethSendTransaction.getTransactionHash();
            getBbcLogger().info("send {} msg tx {}", sendtype,ethSendTransaction.getTransactionHash());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return txhash;

    }

    @Override
    public void waitForTxConfirmed(String txhash) {
        try {
            for (int i = 0; i < MAX_TX_RESULT_QUERY_TIME; i++) {
                Optional<TransactionReceipt> receiptOptional = web3jClient.ethGetTransactionReceipt(txhash).send().getTransactionReceipt();
                if (receiptOptional.isPresent() && receiptOptional.get().getBlockNumber().longValue() > 0L) {
                    if (receiptOptional.get().getStatus().equals("0x1")) {
                        getBbcLogger().info("tx {} has been confirmed as success", txhash);
                    } else {
                        getBbcLogger().error("tx {} has been confirmed as failed", txhash);
                    }
                    break;
                }
                Thread.sleep(1_000);
            }

            EthGetTransactionReceipt ethGetTransactionReceipt = web3jClient.ethGetTransactionReceipt(txhash).send();
            TransactionReceipt transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt().get();
            Assert.assertNotNull(transactionReceipt);
            Assert.assertTrue(transactionReceipt.isStatusOK());
        } catch (Exception e) {
            getBbcLogger().error("", e);
        }

    }
}
