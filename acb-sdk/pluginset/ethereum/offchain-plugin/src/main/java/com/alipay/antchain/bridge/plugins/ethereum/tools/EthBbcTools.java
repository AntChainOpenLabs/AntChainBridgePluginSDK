package com.alipay.antchain.bridge.plugins.ethereum.tools;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.Collections;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.plugins.ethereum.EthereumConfig;
import com.alipay.antchain.bridge.plugins.ethereum.abi.AppContract;
import com.alipay.antchain.bridge.plugins.ethereum.helper.*;
import com.alipay.antchain.bridge.plugins.ethereum.helper.model.GasPriceProviderConfig;
import org.slf4j.helpers.NOPLogger;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Files;
import org.web3j.utils.Numeric;

public class EthBbcTools {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -cp plugins/simple-ethereum-bbc-${version}-plugin.jar com.alipay.antchain.bridge.plugins.ethereum.tools.EthBbcTools <method> [<arg1> <arg2> ...]");
            System.exit(1);
        }

        try {
            switch (args[0]) {
                case "sendUnorderedSdpV2":
                    if (args.length != 8) {
                        System.out.println("Usage: java -cp plugins/simple-ethereum-bbc-${version}-plugin.jar com.alipay.antchain.bridge.plugins.ethereum.tools.EthBbcTools " +
                                           "sendUnorderedSdpV2 <path-to-plugin-config-json> <loop-limit:int> <gap:ms> <sender-contract:hex-address> <receiver:hex-bytes32> <domain:string> <atomic:bool>");
                        System.exit(1);
                    }

                    String pluginConfigJson = args[1];
                    int loopLimit = Integer.parseInt(args[2]);
                    long gap = Long.parseLong(args[3]);
                    String senderContract = args[4];
                    String receiver = args[5];
                    String domain = args[6];
                    boolean atomic = Boolean.parseBoolean(args[7]);

                    System.out.println(StrUtil.format("sendUnorderedV2: {} - {} - {} - {} : loopLimit: {}", senderContract, receiver, domain, atomic, loopLimit));

                    EthereumConfig config = EthereumConfig.fromJsonString(Files.readString(new File(Paths.get(pluginConfigJson).toUri())));

                    Web3j web3j = Web3j.build(new HttpService(config.getUrl()));

                    Credentials credentials = Credentials.create(config.getPrivateKey());

                    RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials, web3j.ethChainId().send().getChainId().longValue());

                    IGasPriceProvider gasPriceProvider = createGasPriceProvider(web3j, config);
                    AppContract appContract = AppContract.load(
                            senderContract, web3j, transactionManager,
                            new AcbGasProvider(
                                    gasPriceProvider,
                                    createEthCallGasLimitProvider(
                                            web3j,
                                            credentials,
                                            senderContract,
                                            new Function(
                                                    AppContract.FUNC_SENDUNORDEREDV2,
                                                    ListUtil.toList(
                                                            new Bytes32(Numeric.hexStringToByteArray(receiver)),
                                                            new Utf8String(domain),
                                                            new Bool(atomic),
                                                            new DynamicBytes(Long.toString(System.currentTimeMillis()).getBytes())
                                                    ),
                                                    Collections.emptyList()
                                            ), config
                                    )
                            )
                    );

                    while (loopLimit != 0) {
                        byte[] msg = Long.toString(System.currentTimeMillis()).getBytes();
                        System.out.println(StrUtil.format("sendUnorderedV2: {} - {} - {} - {} - {} : loopLimit: {}",
                                senderContract, receiver, domain, atomic, msg, loopLimit));
                        TransactionReceipt receipt = appContract.sendUnorderedV2(Numeric.hexStringToByteArray(receiver), domain, atomic, msg).send();
                        if (receipt.isStatusOK()) {
                            System.out.println(StrUtil.format("success to send transaction: {}",receipt.getTransactionHash()));
                        } else {
                            System.err.println(StrUtil.format("failed to send transaction: {}", receipt.getTransactionHash()));
                            System.exit(1);
                        }
                        if (loopLimit > 0) {
                            Thread.sleep(gap);
                            loopLimit--;
                        }
                    }

                    System.out.println("sendUnorderedV2 done.");

                    break;
                default:
                    System.out.println("Unknown method: " + args[0]);
                    System.exit(1);
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }
    }

    private static IGasPriceProvider createGasPriceProvider(Web3j web3j, EthereumConfig config) {
        switch (config.getGasPricePolicy()) {
            case FROM_API:
                GasPriceProviderConfig gasPriceProviderConfig = new GasPriceProviderConfig();
                gasPriceProviderConfig.setGasPriceProviderSupplier(config.getGasPriceProviderSupplier());
                gasPriceProviderConfig.setGasProviderUrl(config.getGasProviderUrl());
                gasPriceProviderConfig.setApiKey(config.getPrivateKey());
                gasPriceProviderConfig.setGasUpdateInterval(config.getGasUpdateInterval());
                return GasPriceProvider.create(web3j, gasPriceProviderConfig, NOPLogger.NOP_LOGGER);
            case STATIC:
            default:
                return new StaticGasPriceProvider(BigInteger.valueOf(config.getGasPrice()));
        }
    }

    private static IGasLimitProvider createEthCallGasLimitProvider(Web3j web3j, Credentials credentials, String toAddr, Function function, EthereumConfig config) {
        switch (config.getGasLimitPolicy()) {
            case ESTIMATE:
                return new EstimateGasLimitProvider(web3j, credentials.getAddress(), toAddr, FunctionEncoder.encode(function), config.getExtraGasLimit());
            case STATIC:
            default:
                return new StaticGasLimitProvider(BigInteger.valueOf(config.getGasLimit()));
        }
    }
}
