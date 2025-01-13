package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyProof;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVItem;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVPacket;
import lombok.Getter;
import lombok.Setter;

/**
 * ptc证明转化结果
 */
@Getter
@Setter
public class PTCProofResult {

    public static short TLV_ORACLE_PUBKEY_HASH = 0;
    public static short TLV_ORACLE_REQUEST_ID = 1;
    public static short TLV_ORACLE_REQUEST_BODY = 2;
    public static short TLV_ORACLE_SIGNATURE_TYPE = 3;
    public static short TLV_ORACLE_REQUEST = 4;
    public static short TLV_ORACLE_RESPONSE_BODY = 5;  // 这里填充RESPONSE 内容
    public static short TLV_ORACLE_RESPONSE_SIGNATURE = 6;
    public static short TLV_ORACLE_ERROR_CODE = 7;
    public static short TLV_ORACLE_ERROR_MSG = 8;

    /**
     * 仅用于向异构链mock proof数据
     */
    public static short TLV_PROOF_SENDER_DOMAIN = 9;

    /**
     * 返回内容，PTC返回内容，无AM扩展时是PTC原始返回数据，有AM扩展插件时，填充的是AM插件处理后的数据，如果AM扩展执行发生异常，则BODY被清空填充空数组。
     */
    public static short PTC_RESP_FIELD_BODY = 0;

    /**
     * 定长 4字节uint32_t，取值0/1, 0 代表访问AM扩展插件失败，OS层面需要重试；
     */
    public static short PTC_RESP_FIELD_AMEXT_CALL_SUCCESS = 1;

    /**
     * 定长 4字节uint32_t，取值0/1, 0 代表调用执行AM扩展查询发生异常，详细内容见PTC_RESP_FIELD_AMEXT_RETURN
     */
    public static short PTC_RESP_FIELD_AMEXT_EXEC_SUCCESS = 2;

    /**
     * 字符串，执行AM扩展查询发生异常具体报错信息
     */
    public static short PTC_RESP_FIELD_AMEXT_EXEC_OUTPUT = 3;

    public static PTCProofResult generateEmptyProofForAM(AuthMsgWrapper authMsgWrapper) {

        List<TLVItem> respItems = new ArrayList<>();
        respItems.add(
                TLVItem.fromBytes(
                        PTC_RESP_FIELD_BODY, 
                        authMsgWrapper.getAuthMessage().encode()
                )
        );
        TLVPacket respPacket = new TLVPacket((short) 0, respItems);

        List<TLVItem> tlvItems = new ArrayList<>();
        tlvItems.add(
                TLVItem.fromBytes(
                        TLV_ORACLE_RESPONSE_BODY, 
                        respPacket.encode()
                )
        );
        tlvItems.add(
                TLVItem.fromUTF8String(
                        TLV_PROOF_SENDER_DOMAIN,
                        authMsgWrapper.getDomain()
                )
        );
        TLVPacket tlvPacket = new TLVPacket((short) 0, tlvItems);

        PTCProofResult result = new PTCProofResult();
        result.setPtcProof(tlvPacket.encode());
        result.setSuccess(true);

        return result;
    }

    // ptcProof原始字节数组
    private byte[] ptcProof;

    // 证明转化组件执行结果
    private boolean isSuccess;

    private int errorCode;

    private String errorMsg;

    // cid的内容
    private byte[] content;

    public PTCProofResult() {}

    public PTCProofResult(ThirdPartyProof tpProof) {
        TLVPacket packet = TLVPacket.decode(tpProof.encode());
        packet.getTlvItems().add(
                TLVItem.fromUTF8String(
                        TLV_PROOF_SENDER_DOMAIN,
                        tpProof.getTpbtaCrossChainLane().getSenderDomain().getDomain()
                )
        );
        this.ptcProof = packet.encode();
        this.isSuccess = true;
        this.errorCode = 0;
        this.errorMsg = "";
    }

    public PTCProofResult(byte[] ptcProof) {

        this.ptcProof = ptcProof;

        TLVPacket tlvPacket = TLVPacket.decode(ptcProof);

        List<TLVItem> tlvItems = tlvPacket.getTlvItems();

        Map<Short, TLVItem> tlvItemMap = new HashMap<Short, TLVItem>();

        for (TLVItem tlvItem : tlvItems) {
            tlvItemMap.put(tlvItem.getType(), tlvItem);
        }

        // 读errorCode
        if (!tlvItemMap.containsKey(TLV_ORACLE_ERROR_CODE)) {
            throw new RuntimeException("error ptc proof, not TLV_ORACLE_ERROR_CODE");
        }
        this.errorCode = tlvItemMap.get(TLV_ORACLE_ERROR_CODE).getUint32Value();
        this.isSuccess = (0 == errorCode);

        // 读errorMsg
        if (!tlvItemMap.containsKey(TLV_ORACLE_ERROR_MSG)) {
            throw new RuntimeException("error ptc proof, not TLV_ORACLE_ERROR_MSG");
        }
        this.errorMsg = tlvItemMap.get(TLV_ORACLE_ERROR_MSG).getUtf8String();

        // 读response
        if (!tlvItemMap.containsKey(TLV_ORACLE_RESPONSE_BODY)) {
            throw new RuntimeException("error ptc proof, not TLV_ORACLE_RESPONSE_BODY");
        }
        byte[] response = tlvItemMap.get(TLV_ORACLE_RESPONSE_BODY).getValue();

        // 解response TLV Packet
        List<TLVItem> respTlvItems = TLVPacket.decode(response).getTlvItems();

        Map<Short, TLVItem> respTlvItemMap = new HashMap<Short, TLVItem>();

        for (TLVItem tlvItem : respTlvItems) {
            respTlvItemMap.put(tlvItem.getType(), tlvItem);
        }

        // 读content
        if (!respTlvItemMap.containsKey(PTC_RESP_FIELD_BODY)) {
            throw new RuntimeException("error ptc proof, not PTC_RESP_FIELD_BODY");
        }
        this.content = respTlvItemMap.get(PTC_RESP_FIELD_BODY).getValue();
    }
}
