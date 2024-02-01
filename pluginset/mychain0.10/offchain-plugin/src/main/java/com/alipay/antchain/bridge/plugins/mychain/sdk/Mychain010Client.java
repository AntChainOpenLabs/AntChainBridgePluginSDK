package com.alipay.antchain.bridge.plugins.mychain.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.plugins.mychain.crypto.CryptoSuiteEnum;
import com.alipay.antchain.bridge.plugins.mychain.utils.MychainUtils;
import com.alipay.mychain.sdk.api.MychainClient;
import com.alipay.mychain.sdk.api.env.ClientEnv;
import com.alipay.mychain.sdk.api.utils.ConfidentialUtil;
import com.alipay.mychain.sdk.api.utils.Utils;
import com.alipay.mychain.sdk.common.Parameters;
import com.alipay.mychain.sdk.common.VMTypeEnum;
import com.alipay.mychain.sdk.crypto.PublicKey;
import com.alipay.mychain.sdk.crypto.hash.Hash;
import com.alipay.mychain.sdk.domain.account.Identity;
import com.alipay.mychain.sdk.domain.block.Block;
import com.alipay.mychain.sdk.domain.contract.Contract;
import com.alipay.mychain.sdk.domain.spv.BlockHeaderInfo;
import com.alipay.mychain.sdk.domain.transaction.TransactionReceipt;
import com.alipay.mychain.sdk.errorcode.ErrorCode;
import com.alipay.mychain.sdk.message.query.QueryAccountResponse;
import com.alipay.mychain.sdk.message.query.QueryContractResponse;
import com.alipay.mychain.sdk.message.transaction.AbstractTransactionRequest;
import com.alipay.mychain.sdk.message.transaction.TransactionReceiptResponse;
import com.alipay.mychain.sdk.message.transaction.confidential.ConfidentialRequest;
import com.alipay.mychain.sdk.message.transaction.confidential.ConfidentialResponse;
import com.alipay.mychain.sdk.message.transaction.contract.*;
import com.alipay.mychain.sdk.type.BaseFixedSizeUnsignedInteger;
import com.alipay.mychain.sdk.utils.RandomUtil;
import com.alipay.mychain.sdk.vm.EVMParameter;
import com.alipay.mychain.sdk.vm.WASMParameter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;

public class Mychain010Client {

    private static final int AUTH_MAP_WEIGHT = 100;
    private Mychain010Config config;
    private Mychain010TeeConfig teeConfig;
    private ClientEnv env = null;
    private final MychainClient sdk;

    private final Logger logger;

    public Mychain010Client(byte[] configData, Logger logger) {
        try {
            this.config = Mychain010Config.fromJsonString(new String(configData));
        } catch (IOException e) {
            throw new RuntimeException("[Mychain010BBCService] build config info error, ", e);
        }

        if (config.isTEEChain()) {
            this.teeConfig.setTeePublicKeys(
                    config.getMychainTeePublicKey().getBytes());
        }

        this.env = config.buildMychainEnvWithConfigUpdate();
        this.sdk = new MychainClient();
        this.logger = logger;
    }

    public boolean startup() {
        if (!sdk.init(env)) {
            logger.error("[Mychain010BBCService] init mychain sdk fail for {}.",
                    config.getMychainPrimary());
            sdk.shutDown();
            return false;
        }

        if (!ifAccountValid()) {
            logger.error("[Mychain010BBCService] account is invalid for {}, account: {},  pubkey: {}",
                    config.getMychainPrimary(),
                    config.getMychainAnchorAccount(),
                    config.getMychainAccountPubKey()
            );
            return false;
        }

        logger.info("[Mychain010Client] startup mychain0.10 sdk success for {}, isSM:{}",
                config.getMychainPrimary(),
                config.isSMChain());
        return true;
    }

    public boolean shutdown() {
        sdk.shutDown();

        logger.info("[Mychain010Client] shutdown mychain0.10 sdk success for {}, isSM:{}",
                config.getMychainPrimary(),
                config.isSMChain());

        return true;
    }

    // ==================================================== 合约相关接口
    public TransactionReceiptResponse callContract(String contractID, Parameters parameters, boolean sync) {
        String account = config.getMychainAnchorAccount();

        VMTypeEnum vmType = MychainUtils.getSupportedParameterType(parameters);
        switch (vmType) {
            case EVM:
                logger.info("call contractID {}.{}",
                        contractID,
                        ((EVMParameter) parameters).getMethodSignature());
                break;
            case WASM:
                logger.info("call contractID {}.{}",
                        contractID,
                        ((WASMParameter) parameters).getMethodSignature());
                break;
        }

        try {
            CallContractRequest request = new CallContractRequest(
                    Utils.getIdentityByName(account, config.getMychainHashType()),
                    Utils.getIdentityByName(contractID, config.getMychainHashType()),
                    parameters,
                    BigInteger.ZERO,
                    vmType
            );
            request.setTxGas(config.getTxGas());

            if (sync) {
                return sdk.getContractService().callContract(request);
            } else {
                long async_ts = System.currentTimeMillis();
                int ret = sdk.getContractService().asyncCallContract(request, (errorCode, response) -> {
                            logger.info(
                                    "async call contract async result callback: errorCode:{}, result:{}", errorCode, response.getErrorCode());
                        }
                );

                logger.info("call contract tx {}, no receipt", request.getTransaction().getHash().hexStrValue());
                logger.info("async call contract tx {}, cost {} ms", request.getTransaction().getHash(),
                        System.currentTimeMillis() - async_ts);

                // 异步调用注意需要关注是否调用成功
                // 根据 response 构造 crosschainMessage 时 需要填充 txHash、receipt.result 和 confirmed 字段
                TransactionReceiptResponse response = new TransactionReceiptResponse();
                response.setTxHash(request.getTransaction().getHash());
                TransactionReceipt receipt = new TransactionReceipt();
                receipt.setResult(ret);
                response.setTransactionReceipt(receipt);
                return response;
            }
        } catch (
                Exception e) {
            logger.error("send call contract error.", e);
            throw new RuntimeException(e);
        }
    }

    public TransactionReceiptResponse callTeeWasmContract(String contractID, WASMParameter parameters, boolean sync) {
        String account = config.getMychainAnchorAccount();

        logger.info("call tee contractID {}.{}", contractID, parameters.getMethodSignature());

        try {

            CallContractRequest request = new CallContractRequest(
                    Utils.getIdentityByName(account, config.getMychainHashType()),
                    Utils.getIdentityByName(contractID, config.getMychainHashType()),
                    parameters,
                    BigInteger.ZERO,
                    VMTypeEnum.WASM
            );
            request.setTxGas(config.getTxGas());

            generateTeeRequest(request);

            byte[] transactionKey = ConfidentialUtil.keyGenerate(
                    teeConfig.getTeeSecretKey(),
                    request.getTransaction().getHash().getValue());
            ConfidentialRequest confidentialRequest = new ConfidentialRequest(
                    request,
                    teeConfig.getTeePublicKeys(),
                    transactionKey);

            if (sync) {
                return sdk.getConfidentialService().confidentialRequest(confidentialRequest);
            } else {
                int ret = sdk.getConfidentialService().asyncConfidentialRequest(confidentialRequest, (errorCode, response) ->
                        logger.info("async call tee contract, errorCode:{}, response:{}", errorCode, response.getErrorCode()));

                logger.info("async call tee contract tx {}, no receipt", confidentialRequest.getTransaction().getHash().hexStrValue());
                return null;
            }
        } catch (Exception e) {
            logger.error("call tee contract error.", e);
            throw new RuntimeException(e);
        }
    }

    public TransactionReceipt localCallContract(String contractID, Parameters parameters) {
        String account = config.getMychainAnchorAccount();

        VMTypeEnum vmType = this.getSupportedParameterType(parameters);
        switch (vmType) {
            case EVM:
                logger.info("local call contractID {}.{}", contractID, ((EVMParameter) parameters).getMethodSignature());
                break;
            case WASM:
                logger.info("local call contractID {}.{}", contractID, ((WASMParameter) parameters).getMethodSignature());
                break;
        }

        CallContractRequest request = new CallContractRequest(
                Utils.getIdentityByName(account, config.getMychainHashType()),
                Utils.getIdentityByName(contractID, config.getMychainHashType()),
                parameters,
                BigInteger.ZERO,
                vmType
        );
        request.setTxGas(config.getTxGas());

        try {
            request.setLocal();

            logger.info("call detail {}", request.toString());

            CallContractResponse result = sdk.getContractService().callContract(request);

            logger.info("call result {}", result.toString());

            return result.getTransactionReceipt();

        } catch (Exception e) {
            logger.error("send local call error.", e);
            throw new RuntimeException(e);
        }
    }

    public TransactionReceipt localCallTeeWasmContract(String contractID, WASMParameter parameters) {
        String account = config.getMychainAnchorAccount();

        logger.info("localcall tee contractID {}.{}", contractID, parameters.getMethodSignature());
        CallContractRequest request = new CallContractRequest(
                Utils.getIdentityByName(account, config.getMychainHashType()),
                Utils.getIdentityByName(contractID, config.getMychainHashType()),
                parameters,
                BigInteger.ZERO,
                VMTypeEnum.WASM
        );
        request.setTxGas(config.getTxGas());

        try {
            request.setLocal();
            logger.info("tee localcall detail {}", request.toString());

            generateTeeRequest(request);

            byte[] transactionKey = ConfidentialUtil.keyGenerate(
                    teeConfig.getTeeSecretKey(),
                    request.getTransaction().getHash().getValue());

            ConfidentialRequest confidentialRequest = new ConfidentialRequest(
                    request,
                    teeConfig.getTeePublicKeys(),
                    transactionKey);

            ConfidentialResponse response = sdk.getConfidentialService()
                    .confidentialRequest(confidentialRequest);

            return response.getReceipt(transactionKey);
        } catch (Exception e) {
            logger.error("send tee local call error.", e);
            throw new RuntimeException(e);
        }
    }


    private VMTypeEnum getSupportedParameterType(Parameters parameters) {
        if (parameters instanceof EVMParameter) {
            return VMTypeEnum.EVM;
        } else if (parameters instanceof WASMParameter) {
            return VMTypeEnum.WASM;
        } else {
            throw new RuntimeException("Unsupported parameter type: " + parameters.getClass().getTypeName());
        }
    }

    public boolean deployContract(String contractFilename, String contractId, VMTypeEnum vmType, Parameters parameters) {
        logger.info("deploy contract {} {}", contractId, contractFilename);
        String account = config.getMychainAnchorAccount();

        byte[] contractCode = readFileBytes(contractFilename);
        if (vmType == VMTypeEnum.EVM) {
            contractCode = Hex.decode(new String(contractCode));
        }

        DeployContractRequest request = new DeployContractRequest(
                Utils.getIdentityByName(account, config.getMychainHashType()),
                Utils.getIdentityByName(contractId, config.getMychainHashType()),
                contractCode,
                vmType,
                parameters,
                BigInteger.ZERO);
        request.setTxGas(config.getTxGas());

        //deploy contract
        DeployContractResponse response = sdk.getContractService().deployContract(request);

        logger.info("chain {} with crypto_suite {} deploy contract {} txHash {} - {} - {} - {} ",
                config.getMychainPrimary(),
                config.getMychainCryptoSuiteEnum().getName(),
                contractId,
                response.getTxHash().hexStrValue(),
                response.isSuccess(),
                response.getErrorCode().getErrorCode(),
                response.getErrorCode().getErrorDesc());
        if (!response.isSuccess()) {
            return false;
        }

        logger.info("deploy receipt : {}", response.toString());
        return ErrorCode.SUCCESS.getErrorCode() == response.getTransactionReceipt().getResult();
    }

    public boolean deployTeeWasmContract(String contractFilename, String contractId, WASMParameter parameters) {
        logger.info("deploy tee contract {} {}", contractId, contractFilename);
        String account = config.getMychainAnchorAccount();

        byte[] contractCode = this.readFileBytes(contractFilename);
        DeployContractRequest request = new DeployContractRequest(
                Utils.getIdentityByName(account, config.getMychainHashType()),
                Utils.getIdentityByName(contractId, config.getMychainHashType()),
                contractCode,
                VMTypeEnum.WASM,
                parameters,
                BigInteger.ZERO);
        request.setTxGas(config.getTxGas());

        generateTeeRequest(request);

        byte[] transactionKey = ConfidentialUtil.keyGenerate(
                teeConfig.getTeeSecretKey(),
                request.getTransaction().getHash().getValue());
        ConfidentialRequest confidentialRequest = new ConfidentialRequest(
                request,
                teeConfig.getTeePublicKeys(),
                transactionKey);
        if (!confidentialRequest.isValid()) {
            return false;
        }
        confidentialRequest.complete();

        //deploy contract
        ConfidentialResponse response = sdk.getConfidentialService().confidentialRequest(confidentialRequest);

        logger.info("deploy tee wasm contract {}~{} txHash {} - {} - {} - {}", account, contractId,
                response.getTxHash().hexStrValue(),
                response.isSuccess(), response.getErrorCode().getErrorCode(), response.getErrorCode().getErrorDesc());

        if (!response.isSuccess()) {
            return false;
        }

        TransactionReceipt receipt = response.getReceipt(transactionKey);
        logger.info("deploy tee wasm receipt : {}-{}", receipt.getResult(), receipt.toString());

        return receipt.getResult() == ErrorCode.SUCCESS.getErrorCode();
    }

    public boolean upgradeTeeWasmContract(String contractFilename, String contractId) {
        return this.upgradeTeeWasmContract(this.readFileBytes(contractFilename), contractId);
    }

    public boolean upgradeTeeWasmContract(byte[] contractCode, String contractId) {
        if (StringUtils.isEmpty(contractId)) {
            logger.info("tee contract do not be deployed yet : {}", contractId);
            return false;
        }
        UpdateContractRequest request = new UpdateContractRequest(
                Utils.getIdentityByName(contractId, config.getMychainHashType()),
                contractCode,
                VMTypeEnum.WASM
        );

        generateTeeRequest(request);

        byte[] transactionKey = ConfidentialUtil.keyGenerate(
                teeConfig.getTeeSecretKey(),
                request.getTransaction().getHash().getValue());
        ConfidentialRequest confidentialRequest = new ConfidentialRequest(
                request,
                teeConfig.getTeePublicKeys(),
                transactionKey);

        //upgrade contract
        ConfidentialResponse response = sdk.getConfidentialService().confidentialRequest(confidentialRequest);

        logger.info("upgrade tee wasm contract {} txHash {} - {} - {} - {}", contractId,
                response.getTxHash().hexStrValue(),
                response.isSuccess(), response.getErrorCode().getErrorCode(), response.getErrorCode().getErrorDesc());

        if (!response.isSuccess()) {
            return false;
        }

        TransactionReceipt receipt = response.getReceipt(transactionKey);
        logger.info("upgrade tee wasm receipt : {}", receipt.toString());

        return receipt.getResult() == ErrorCode.SUCCESS.getErrorCode();
    }

    public boolean upgradeContract(String contractFilename, String contractId, VMTypeEnum type) {
        return this.upgradeContract(this.readFileBytes(contractFilename), contractId, type);
    }

    public boolean upgradeContract(byte[] contractCode, String contractId, VMTypeEnum type) {
        if (StringUtils.isEmpty(contractId)) {
            logger.error("contract do not be deployed yet : {}", contractId);
            return false;
        }
        if (type == VMTypeEnum.EVM) {
            contractCode = Hex.decode(new String(contractCode));
        }

        UpdateContractRequest request = new UpdateContractRequest(
                Utils.getIdentityByName(contractId, config.getMychainHashType()),
                contractCode,
                type
        );

        UpdateContractResponse response = sdk.getContractService().updateContract(request);
        if (!response.isSuccess()) {
            logger.error(
                    "upgrade contract failed: {} - {} - {}",
                    contractId,
                    response.getErrorCode().getErrorCode(),
                    response.getErrorCode().getErrorDesc()
            );
            return false;
        }
        logger.info(
                "upgrade contract {} txHash {} - {} - {} - {}",
                contractId,
                response.getTxHash().hexStrValue(),
                response.isSuccess(),
                response.getErrorCode().getErrorCode(),
                response.getErrorCode().getErrorDesc()
        );

        logger.info("upgrade contract receipt : {}", response.getTransactionReceipt());

        return response.getTransactionReceipt().getResult() == (long) ErrorCode.SUCCESS.getErrorCode();
    }

    private void generateTeeRequest(AbstractTransactionRequest request) {
        long ts = sdk.getNetwork().getSystemTimestamp();
        request.setTxTimeNonce(ts, BaseFixedSizeUnsignedInteger.Fixed64BitUnsignedInteger
                .valueOf(RandomUtil.randomize(ts + request.getTransaction().hashCode())), true);
        request.complete();
        sdk.getConfidentialService().signRequest(
                env.getSignerOption().getSigners(),
                request);
    }

    private byte[] readFileBytes(String filePath) {
        try {
            InputStream stream = this.getClass().getResourceAsStream(filePath);
            if (stream == null) {
                String msg = StrUtil.format("load contract file fail. filePath={}", filePath);
                logger.error(msg);
                throw new RuntimeException(msg);
            }
            return IOUtils.toByteArray(stream);
        } catch (IOException e) {
            logger.error("load contract error.", e);
            throw new RuntimeException(e);
        }
    }

    // ==================================================== 链上数据（交易、区块）相关接口

    /**
     * 根据交易哈希查询交易回执
     *
     * @param txHash
     * @return
     */
    public TransactionReceipt getTxReceiptByTxhash(String txHash) {
        return sdk.getQueryService().queryTransactionReceipt(new Hash(txHash)).getTransactionReceipt();
    }

    /**
     * 根据区块高度获取区块
     *
     * @param height
     */
    public Block getBlockByHeight(long height) {
        return sdk.getQueryService().queryBlock(BigInteger.valueOf(height)).getBlock();
    }

    /**
     * 根据区块高度获取区块头信息
     *
     * @param height
     * @param maxAmount
     * @param reverse
     * @return
     */
    public List<BlockHeaderInfo> getRawBlockHeaderInfoByHeight(long height, int maxAmount, int reverse) {
        return sdk.getQueryService().queryBlockHeaderInfos(BigInteger.valueOf(height), maxAmount, reverse).getBlockHeaderInfoList();
    }

    /**
     * 获取最新区块高度
     *
     * @return
     */
    public Long queryLatestHeight() {
        return sdk.getQueryService().queryLastBlock().getBlock().getBlockHeader().getNumber().longValue();
    }

    // ==================================================== 其他查询接口
    public Mychain010Config getConfig() {
        return config;
    }

    public String getPrimary() {
        return config.getMychainPrimary();
    }

    public boolean isTeeChain() {
        return config.getMychainTeePublicKey() != null && !config.getMychainTeePublicKey().isEmpty();
    }

    public boolean isSMChain() {
        return ObjectUtil.equals(
                CryptoSuiteEnum.CRYPTO_SUITE_SM,
                this.config.getMychainCryptoSuiteEnum());
    }

    /**
     * 入参为合约id，即合约名称的哈希字符串，不能直接传合约名称！！！
     *
     * @param contractId
     * @return
     */
    public VMTypeEnum getContractType(String contractId) {
        if (isTeeChain()) {
            return null;
        }

        QueryContractResponse response = sdk.getQueryService().queryContract(new Identity(contractId));
        if (!response.isSuccess()
                && response.getErrorCode().getErrorCode() != ErrorCode.SERVICE_QUERY_NO_RESULT.getErrorCode()) {
            throw new RuntimeException("query mychain010 contract type fail for " + contractId);
        }

        Contract contract = response.getContract();
        if (null == contract || ArrayUtils.isEmpty(contract.getCode())) {
            return null;
        }

        return VMTypeEnum.valueOf(contract.getCode()[0]);
    }

    public boolean ifAccountValid() {
        try {
            MychainUtils.checkKeyPair(
                    config.getMychainAccountPriKey(),
                    config.getMychainAccountPubKey(),
                    config.getMychainCryptoSuiteEnum()
            );
        } catch (Exception e) {
            logger.error("invalid key pair.", e);
            return false;
        }

        QueryAccountResponse replayAccount = sdk.getQueryService().queryAccount(
                Utils.getIdentityByName(
                        config.getMychainAnchorAccount(),
                        config.getMychainHashType()));

        logger.info("Query mychain account: {} {} {} {}",
                config.getMychainAnchorAccount(),
                Utils.getIdentityByName(
                        config.getMychainAnchorAccount(),
                        config.getMychainHashType()),
                replayAccount.isSuccess(),
                replayAccount.getErrorCode());

        if (!replayAccount.isSuccess()) {
            return false;
        }

        Map<PublicKey, Integer> authMap = replayAccount.getAccount().getAuthMap().getAuthMap();

        if (authMap.size() != 1) {
            logger.info("auth map size must only has itself. size {}", authMap.size());
            return false;
        }

        PublicKey publicKey = (PublicKey) authMap.keySet().toArray()[0];

        // Check whether the pubkey weights are consistent according to the auth_map
        if (authMap.get(publicKey) != AUTH_MAP_WEIGHT) {
            logger.info("auth weight must be 100, but it's {}", authMap.get(publicKey));
            return false;
        }

        // Check whether the pubkey info is consistent with pubkey.
        // The length of the normal pubkey is 64,
        // but the length of the sm pubkey is 67 (the extra 3-byte prefix needs to be removed).
        if (publicKey.getData().length != 64 && publicKey.getData().length != 67) {
            logger.info("invalid publickey from sdk");
            return false;
        } else if (publicKey.getData().length == 67) {
            byte[] pubkeyByte = new byte[64];
            System.arraycopy(publicKey.getData(), 3, pubkeyByte, 0, pubkeyByte.length);
            publicKey = new PublicKey(pubkeyByte);
        }

        if (!publicKey.hexStrValue().equals(config.getMychainAccountPubKey())) {
            logger.info("auth map size must only has itself. size {}", authMap.size());
            return false;
        }

        return true;
    }
}
