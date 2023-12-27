package com.alipay.antchain.bridge.plugins.mychain.utils;

import com.alipay.antchain.bridge.commons.core.am.AbstractAuthMessage;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV1;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.plugins.mychain.model.MSGPayload;
import com.alipay.antchain.bridge.plugins.mychain.model.UdagProofResult;

public class ContractUtils {

    /**
     * 从proofs中解析出am消息进一步晋西sdp消息，最终获取接收合约id
     * @param proofsData
     * @return
     */
    public static String extractReceiverIdentity(byte[] proofsData) {
        int _len = proofsData.length;
        int _offset = 0;
        while (_offset < _len) {
            // hints len
            byte[] hints_len_bytes = new byte[4];
            System.arraycopy(proofsData, _offset, hints_len_bytes, 0, 4);
            _offset += 4;
            int hints_len = (int) SerializedDataUtils.extractUint32(hints_len_bytes, 4);
            // hints
            byte[] hints = new byte[hints_len];
            System.arraycopy(proofsData, _offset, hints, 0, hints_len);
            _offset += hints_len;

            // proof lens
            byte[] proof_len_bytes = new byte[4];
            System.arraycopy(proofsData, _offset, proof_len_bytes, 0, 4);
            _offset += 4;
            int proof_len = (int) SerializedDataUtils.extractUint32(proof_len_bytes, 4);
            // proof
            byte[] proof = new byte[proof_len];
            System.arraycopy(proofsData, _offset, proof, 0, proof_len);
            _offset += proof_len;

            // 解析proof
            UdagProofResult result = new UdagProofResult(proof);
            int version = AbstractAuthMessage.decodeVersionFromBytes(result.getContent());
            if (version == 1) {
                AuthMessageV1 authMessageV1 = new AuthMessageV1();
                authMessageV1.decode(result.getContent());

                byte[] payload = authMessageV1.getPayload();
                return MSGPayload.decode(payload).getIdentity();
            } else if (version == 2){
                AuthMessageV2 authMessageV2 = new AuthMessageV2();
                authMessageV2.decode(result.getContent());

                byte[] payload = authMessageV2.getPayload();
                return MSGPayload.decode(payload).getIdentity();
            } else {
                return null;
            }
        }
        return null;
    }
}
