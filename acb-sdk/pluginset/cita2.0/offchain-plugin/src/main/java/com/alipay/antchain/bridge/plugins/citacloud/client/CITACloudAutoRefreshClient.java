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

package com.alipay.antchain.bridge.plugins.citacloud.client;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import blockchain.Blockchain;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.plugins.citacloud.CITACloudConfig;
import com.alipay.antchain.bridge.plugins.citacloud.types.AppAuthToken;
import com.alipay.antchain.bridge.plugins.citacloud.types.AppAuthTokenResponse;
import com.alipay.antchain.bridge.plugins.citacloud.types.CITACloudLogInfo;
import com.cita.cloud.constant.ContractParam;
import com.cita.cloud.request.CallRequest;
import com.cita.cloud.response.AppResponse;
import com.cita.cloud.response.Block;
import com.cita.cloud.response.TransactionReceipt;
import com.cita.cloud.sdk.ChannelConfig;
import com.cita.cloud.sdk.CitaCloudClient;
import com.cita.cloud.sdk.TargetChannelConfig;
import com.cita.cloud.util.ContractUtil;
import com.cita.cloud.util.ConvertStrByte;
import com.cita.cloud.util.SignUtil;
import com.citahub.cita.abi.FunctionEncoder;
import com.citahub.cita.abi.FunctionReturnDecoder;
import com.citahub.cita.abi.TypeReference;
import com.citahub.cita.abi.datatypes.Function;
import com.citahub.cita.abi.datatypes.Type;
import com.citahub.cita.crypto.Hash;
import com.citahub.cita.crypto.sm2.SM3;
import com.citahub.cita.protocol.core.methods.request.Transaction;
import com.citahub.cita.utils.Numeric;
import com.google.protobuf.ByteString;
import lombok.SneakyThrows;
import lombok.Synchronized;
import okhttp3.*;

public class CITACloudAutoRefreshClient {

    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private TargetChannelConfig config;

    private String chainCode;

    private CitaCloudClient citaCloudClient;

    private OkHttpClient httpClient;

    private AppAuthToken authToken;

    private String urlToGetAuthToken;

    private String jsonBodyToGetAuthToken;

    private long secondsBeforeAuthTokenExpired;

    private String privateKey;

    private String address;

    private String cryptoSuite;

    private Thread daemonThread;

    public CITACloudAutoRefreshClient(CITACloudConfig config) {
        this(
                config.getNodeHost(), config.getTxPort(), config.getQueryPort(),
                config.getChainCode(), config.getGatewayHost(), config.getAppId(), config.getAppSecret(),
                config.getSecondsBeforeAuthTokenExpired(),
                config.getCryptoSuite(),
                config.getPrivateKey(), config.getAddress()
        );
    }

    public CITACloudAutoRefreshClient(
            String nodeHost,
            int txPort,
            int queryPort,
            String chainCode,
            String gatewayHost,
            String appId,
            String appSecret,
            long secondsBeforeAuthTokenExpired,
            String cryptoSuite,
            String privateKey,
            String address
    ) {
        TargetChannelConfig.TargetChannelConfigBuilder targetChannelConfigBuilder = TargetChannelConfig.builder();
        // 交易端口号
        targetChannelConfigBuilder.rpcChannelConfig(
                new ChannelConfig(nodeHost, txPort)
        );
        // 查询端口号
        targetChannelConfigBuilder.callChannelConfig(
                new ChannelConfig(nodeHost, queryPort)
        );
        config = targetChannelConfigBuilder.build();

        if (gatewayHost.startsWith("https://")) {
            try {
                TrustManager[] trustAllCerts = buildTrustManagers();
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                httpClient = new OkHttpClient.Builder()
                        .sslSocketFactory(
                                sslContext.getSocketFactory(),
                                (X509TrustManager) trustAllCerts[0]
                        ).hostnameVerifier((hostname, session) -> true)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            httpClient = new OkHttpClient.Builder().build();
            if (!gatewayHost.startsWith("http://")) {
                gatewayHost = StrUtil.format("http://{}", gatewayHost);
            }
        }

        urlToGetAuthToken = String.format("%s/getToken", gatewayHost);
        jsonBodyToGetAuthToken = String.format("{\"appId\":\"%s\",\"appSecret\":\"%s\"}", appId, appSecret);
        this.chainCode = chainCode;
        this.secondsBeforeAuthTokenExpired = secondsBeforeAuthTokenExpired;
        this.daemonThread = new Thread(() -> {
            while (true) {
                try {
                    if (ifNeedRefreshAuthToken()) {
                        System.out.println("Refresh token now " + DateUtil.now());
                        refreshAuthToken();

                        Map<String, String> headerMap = new HashMap<>();
                        // 请求头设置：身份凭证token、指定的链标识
                        headerMap.put("x-authorization", Objects.requireNonNull(authToken).getAuthToken());
                        headerMap.put("chain_code", chainCode);
                        CitaCloudClient.SINGLETON.refreshStub(headerMap);
                    } else {
                        Thread.sleep(Math.min(30_000, Math.max(1000, 1000 * this.secondsBeforeAuthTokenExpired / 2)));
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] failed to refresh auth token");
                    e.printStackTrace();
                }
            }
        });
        this.daemonThread.setName(String.format("cita-auth-token-daemon-%s@%s", appId, this));
        this.daemonThread.setDaemon(true);
        this.cryptoSuite = StrUtil.isEmpty(cryptoSuite) ? CITACloudConfig.CITA_CRYPTO_SUITE_TYPE_SM : cryptoSuite;
        this.privateKey = privateKey;
        this.address = address;
    }

    public void init() {
        refreshAuthToken();

        Map<String, String> headerMap = new HashMap<>();
        // 请求头设置：身份凭证token、指定的链标识
        headerMap.put("x-authorization", Objects.requireNonNull(authToken).getAuthToken());
        headerMap.put("chain_code", chainCode);
        this.config.setCallCredentialsMap(headerMap);

        try {
            citaCloudClient = CitaCloudClient.SINGLETON.init(config, false);
        } catch (Exception e) {
            throw new RuntimeException("failed to init CITA client: ", e);
        }

        this.daemonThread.start();
    }

    private long calcRefreshPeriod() {
        return Math.min(this.authToken.getExpireTime() / 2, this.secondsBeforeAuthTokenExpired);
    }

    private boolean ifNeedRefreshAuthToken() {
        return System.currentTimeMillis() / 1000 >= this.authToken.getEndTime() - calcRefreshPeriod();
    }

    @Synchronized
    private void refreshAuthToken() {
        try (
                Response response = httpClient.newCall(
                        new Request.Builder()
                                .url(urlToGetAuthToken)
                                .post(RequestBody.create(JSON_MEDIA_TYPE, jsonBodyToGetAuthToken))
                                .build()
                ).execute();
        ) {
            AppAuthTokenResponse appAuthTokenResponse = AppAuthTokenResponse.parseFrom(Objects.requireNonNull(response.body()).bytes());
            if (ObjectUtil.isNull(appAuthTokenResponse) || appAuthTokenResponse.getCode() != 200) {
                throw new RuntimeException("unexpected response: " + response.body().string());
            }

            this.authToken = Objects.requireNonNull(appAuthTokenResponse.getToken());
            this.authToken.setEndTime(System.currentTimeMillis() / 1000 + this.authToken.getExpireTime());
        } catch (Exception e) {
            throw new RuntimeException("failed to call " + urlToGetAuthToken, e);
        }
    }

    public void shutdown() {
        this.daemonThread.interrupt();
    }

    public Long queryLatestHeight() {
        AppResponse<BigInteger> response = CitaCloudClient.getServiceInstance().getCurrentBlockNumber();
        if (ObjectUtil.isNotEmpty(response.getError())) {
            throw new RuntimeException("failed to query latest height: " + response.getError());
        }
        return response.getData().longValue();
    }

    public String deployContractWithoutConstructor(String contractByteCodeHex, boolean sync) {

        try {
//            DeployContractRequest request = new DeployContractRequest();
//            request.setContractCode(HexUtil.encodeHexStr(contractByteCode));
//            request.setPrivateKey(this.privateKey);
//            request.setCryptoTx();
//            CitaCloudClient.getServiceInstance().deployContract()

            // 构建交易数据：交易数据、合约地址、有效块高
            Blockchain.Transaction transaction = CitaCloudClient.getServiceInstance()
                    .transaction(
                            contractByteCodeHex, null
                    ).getData();

            // 交易数据签名：私钥、交易数据、加密方式
            String signature = HexUtil.encodeHexStr(
                    SignUtil.getSignature(privateKey, transaction.toByteArray(), selectCryptoTx())
            );

            Blockchain.Witness witness = Blockchain.Witness.newBuilder()
                    .setSignature(ByteString.copyFrom(ConvertStrByte.hexStringToBytes(signature)))
                    .setSender(ByteString.copyFrom(ConvertStrByte.hexStringToBytes(address)))
                    .build();

            Blockchain.UnverifiedTransaction unverifiedTransaction = Blockchain.UnverifiedTransaction.newBuilder()
                    .setTransaction(transaction)
                    .setTransactionHash(ByteString.copyFrom(this.hash(transaction.toByteArray())))
                    .setWitness(witness)
                    .build();

            return checkTxReceipt(
                    sync,
                    CitaCloudClient.getServiceInstance()
                            .sendRawTransaction(ConvertStrByte.bytesToHexString(unverifiedTransaction.toByteArray()))
            ).getContractAddress();

        } catch (Exception e) {
            throw new RuntimeException("failed to deploy contract: ", e);
        }
    }

    public String deployContractWithConstructor(String contractByteCodeHex, List<ContractParam> params, boolean sync) {
        return deployContractWithoutConstructor(contractByteCodeHex + encodeConstructor(params), sync);
    }

    private String encodeConstructor(List<ContractParam> params) {
        return FunctionEncoder.encodeConstructor(
                ContractUtil.convertInputParams(params)
        );
    }

    @SneakyThrows
    public List<CITACloudLogInfo> filterContractEventsByHeight(long height, String contract, String eventMethod) {
        AppResponse<Block> response = null;
        for (int i = 0; i < 10; i++) {
            try {
                response = CitaCloudClient.getServiceInstance().getBlockByNumber(BigInteger.valueOf(height));
            } catch (Exception e) {
                if (i == 9) {
                    throw e;
                }
                Thread.sleep(500);
            }
        }

        if (ObjectUtil.isNull(response) || StrUtil.isNotEmpty(response.getError())) {
            throw new RuntimeException(
                    String.format("query block %d failed: %s",
                            height, ObjectUtil.isNull(response) ? "resp is null" : response.getError())
            );
        }
        if (ObjectUtil.isEmpty(response.getData().getBody().getTransactions())) {
            return ListUtil.empty();
        }

        AppResponse<Block> finalResponse = response;
        AppResponse<Block> finalResponse1 = response;
        return response.getData().getBody().getTransactions().stream()
                .flatMap(
                        transaction -> {
                            AppResponse<TransactionReceipt> receiptAppResponse =
                                    CitaCloudClient.getServiceInstance().getTransactionReceiptWithState(transaction.getHash());
                            if (ObjectUtil.isNull(receiptAppResponse) || StrUtil.isNotEmpty(receiptAppResponse.getError())) {
                                throw new RuntimeException(
                                        String.format("query tx %s failed: %s",
                                                transaction.getHash(), ObjectUtil.isNull(finalResponse) ? "resp is null" : finalResponse.getError())
                                );
                            }
                            return receiptAppResponse.getData().getLogs().stream();
                        }
                ).filter(
                        log -> StrUtil.equalsIgnoreCase(Numeric.cleanHexPrefix(log.getAddress()), Numeric.cleanHexPrefix(contract))
                                && StrUtil.equalsIgnoreCase(log.getTopics().get(0), calcEventSingleTopic(eventMethod))
                ).map(log -> new CITACloudLogInfo(log, finalResponse1.getData().getHeader().getTimestamp()))
                .collect(Collectors.toList());
    }

    private String calcEventSingleTopic(String eventMethod) {
        return Numeric.toHexString(Hash.sha3(eventMethod.getBytes()));
    }

    public TransactionReceipt callContract(String contract, String funcName, List<Type> inputParameters, boolean sync) {
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Function function = new Function(funcName, inputParameters, outputParameters);
        String encodedFunction = FunctionEncoder.encode(function);

        try {
            // 构建交易数据：交易数据、合约地址、有效块高
            Blockchain.Transaction transaction = CitaCloudClient.getServiceInstance()
                    .transaction(encodedFunction, contract)
                    .getData();
            // 交易数据签名：私钥、交易数据、加密方式
            String signature = HexUtil.encodeHexStr(
                    SignUtil.getSignature(privateKey, transaction.toByteArray(), selectCryptoTx())
            );
            Blockchain.Witness witness = Blockchain.Witness.newBuilder()
                    .setSignature(ByteString.copyFrom(ConvertStrByte.hexStringToBytes(signature)))
                    .setSender(ByteString.copyFrom(ConvertStrByte.hexStringToBytes(address)))
                    .build();

            byte[] hash;
            try {
                hash = hash(transaction.toByteArray());
            } catch (IOException e) {
                throw new IllegalArgumentException("hash error.", e);
            }

            Blockchain.UnverifiedTransaction unverifiedTransaction = Blockchain.UnverifiedTransaction.newBuilder()
                    .setTransaction(transaction)
                    .setTransactionHash(ByteString.copyFrom(hash))
                    .setWitness(witness)
                    .build();

            return checkTxReceipt(
                    sync,
                    CitaCloudClient.getServiceInstance()
                            .sendRawTransaction(ConvertStrByte.bytesToHexString(unverifiedTransaction.toByteArray()))
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("failed to call contract %s with parameters %s",
                            contract, CollectionUtil.join(inputParameters, ",")), e
            );
        }
    }

    public List<Type> localCallContract(String contract, String funcName, List<Type> inputParameters, List<TypeReference<?>> outputParameters) {
        CallRequest request = new CallRequest();
        request.setContractAddress(contract);
        request.setFunction(
                new Function(
                        funcName,
                        inputParameters,
                        outputParameters
                )
        );
        request.setSender(address);

        AppResponse<String> response = CitaCloudClient.getServiceInstance().call(request);
        return FunctionReturnDecoder.decode(response.getData(), request.getFunction().getOutputParameters());
    }

    private TransactionReceipt checkTxReceipt(boolean sync, AppResponse<String> response) throws InterruptedException {
        String txHash = response.getData().startsWith("0x") ? response.getData() : "0x" + response.getData();
        TransactionReceipt receipt = this.queryTxInfo(txHash);
        if (ObjectUtil.isNull(receipt) || !isTxExecuteSuccess(receipt)) {
            throw new RuntimeException("transaction failed to execute");
        }
        if (sync && !ifTxConfirmed(receipt)) {
            receipt = waitUntilTxConfirmed(receipt);
        }

        return receipt;
    }

    public TransactionReceipt queryTxInfo(String txHash) {
        AppResponse<TransactionReceipt> response = CitaCloudClient.getServiceInstance().getTransactionReceiptWithState(txHash);
        if (ObjectUtil.isNull(response) || StrUtil.isNotEmpty(response.getError())) {
            throw new RuntimeException(String.format("tx %s not found", txHash));
        }
        return response.getData();
    }

    private Transaction.CryptoTx selectCryptoTx() {
        switch (this.cryptoSuite) {
            case CITACloudConfig.CITA_CRYPTO_SUITE_TYPE_SM:
                return Transaction.CryptoTx.SM2;
            case CITACloudConfig.CITA_CRYPTO_SUITE_TYPE_DEFAULT:
            default:
                throw new RuntimeException("crypto suite not support: " + this.cryptoSuite);
        }
    }

    private byte[] hash(byte[] raw) throws IOException {
        switch (this.cryptoSuite) {
            case CITACloudConfig.CITA_CRYPTO_SUITE_TYPE_SM:
                return SM3.hash(raw);
            case CITACloudConfig.CITA_CRYPTO_SUITE_TYPE_DEFAULT:
            default:
                throw new RuntimeException("crypto suite not support: " + this.cryptoSuite);
        }
    }

    private TransactionReceipt waitUntilTxConfirmed(TransactionReceipt receipt) throws InterruptedException {
        int count = 100;
        while (--count >= 0) {
            receipt = queryTxInfo(receipt.getTransactionHash());
            if (ifTxConfirmed(receipt)) {
                break;
            }
            Thread.sleep(200);
        }
        if (count < 0) {
            throw new RuntimeException("waitUntilTxConfirmed: runs out of query number");
        }
        return receipt;
    }

    private boolean ifTxConfirmed(TransactionReceipt receipt) {
        return isTxExecuteSuccess(receipt) && ObjectUtil.isNotNull(receipt.getBlockNumber()) && receipt.getBlockNumber().longValue() > 0;
    }

    public boolean isTxExecuteSuccess(TransactionReceipt receipt) {
        return !StrUtil.equals(receipt.getStatus(), "error") && !StrUtil.containsIgnoreCase(receipt.getErrorMessage(), "Reverted");
    }

    private static TrustManager[] buildTrustManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }

}