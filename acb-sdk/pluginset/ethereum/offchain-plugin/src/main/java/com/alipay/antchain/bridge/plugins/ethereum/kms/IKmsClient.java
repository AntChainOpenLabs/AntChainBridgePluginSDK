package com.alipay.antchain.bridge.plugins.ethereum.kms;

import com.alipay.antchain.bridge.plugins.ethereum.kms.req.KmsClientCreateAndImportReq;
import com.alipay.antchain.bridge.plugins.ethereum.kms.req.KmsClientCreateKeyReq;
import com.alipay.antchain.bridge.plugins.ethereum.kms.req.KmsClientGetSecretReq;
import com.alipay.antchain.bridge.plugins.ethereum.kms.req.KmsClientSignReq;
import com.alipay.antchain.bridge.plugins.ethereum.kms.resp.KmsClientSignResp;

public interface IKmsClient {
    /**
     * 生成数据密钥
     * @return
     */
    String createKey(KmsClientCreateKeyReq req);

    /**
     * 计划删除密钥（默认在7天后删除）
     * @param keyId
     */
    String deleteKey(String keyId);

    /**
     * 导入数据密钥
     * @return
     */
    String createAndImportKey(KmsClientCreateAndImportReq importReq);

    /**
     * 签名
     * @param request
     * @return
     */
    KmsClientSignResp sign(KmsClientSignReq request);

    /**
     * 获取凭据值
     * @param request
     * @return
     */
    String getSecret(KmsClientGetSecretReq request) throws Exception;
}
