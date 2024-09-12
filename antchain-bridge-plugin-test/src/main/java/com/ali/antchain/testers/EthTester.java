package com.ali.antchain.testers;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.ali.antchain.abstarct.AbstractTester;
import com.ali.antchain.lib.abi.AppContract;
import com.ali.antchain.lib.abi.AuthMsg;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.AbstractBBCService;
import org.junit.Assert;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Optional;

public class EthTester extends AbstractTester {

    private static final String WEB3J = "web3j";
    private static final String CREDENTIALS = "credentials";
    private static final String TRANSACTIONMANAGER = "rawTransactionManager";

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
    public String getProtocol(AbstractBBCContext _context) {
        try {
            String protocolAddr = AuthMsg.load(
                            _context.getAuthMessageContract().getContractAddress(),
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
    public byte[] deployApp() {
        try {
            AppContract appContract = AppContract.deploy(
                    web3jClient,
                    rawTransactionManager,
                    new DefaultGasProvider()
            ).send();
            return HexUtil.decodeHex(
                    String.format("000000000000000000000000%s", StrUtil.removePrefix(appContract.getContractAddress(), "0x"))
            );
        } catch (Exception e) {
            getBbcLogger().error("", e);
        }

        return null;
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
