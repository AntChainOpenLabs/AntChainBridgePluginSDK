package com.alipay.antchain.bridge.commons.core.rcc;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReliableCrossChainMessage {
    private static final short IDEMPOTENT_INFO = 0x00;
    private static final short STATUS = 0x01;
    private static final short ORIGINAL_HASH = 0x02;
    private static final short CURRENT_HASH = 0x03;
    private static final short RETRY_TIME = 0x04;
    private static final short TX_TIMESTAMP = 0x05;
    private static final short ERROR_MSG = 0x06;
    private static final short RAW_TX = 0x07;

    @TLVField(tag = IDEMPOTENT_INFO, type = TLVTypeEnum.BYTES)
    private IdempotentInfo idempotentInfo;                  // 五元组

    @TLVField(tag = STATUS, type = TLVTypeEnum.STRING, order = STATUS)
    private ReliableCrossChainMsgProcessStateEnum status;   // 请求状态

    @TLVField(tag = ORIGINAL_HASH, type = TLVTypeEnum.STRING, order = ORIGINAL_HASH)
    private String originalHash;                            // 初始交易哈希

    @TLVField(tag = CURRENT_HASH, type = TLVTypeEnum.STRING, order = CURRENT_HASH)
    private String currentHash;                             // 当前交易哈希

    @TLVField(tag = RETRY_TIME, type = TLVTypeEnum.UINT32, order = RETRY_TIME)
    private int retryTime;                              // 重试次数

    @TLVField(tag = TX_TIMESTAMP, type = TLVTypeEnum.UINT64, order = TX_TIMESTAMP)
    private Long txTimestamp;                               // 时间戳

    @TLVField(tag = ERROR_MSG, type = TLVTypeEnum.STRING, order = ERROR_MSG)
    private String errorMsg;                                // 失败原因

    @TLVField(tag = RAW_TX, type = TLVTypeEnum.BYTES, order = RAW_TX)
    private byte[] rawTx;                                   // 交易原文

    public ReliableCrossChainMessage(IdempotentInfo idempotentInfo,
                                     ReliableCrossChainMsgProcessStateEnum status,
                                     String txHash,
                                     Long txTimestamp,
                                     byte[] rawTx) {
        this.idempotentInfo = idempotentInfo;
        this.originalHash = txHash;
        this.currentHash = txHash;
        this.txTimestamp = txTimestamp;
        this.status = status;
        this.rawTx = rawTx;
    }

    public boolean isFinished(){
        return status != ReliableCrossChainMsgProcessStateEnum.PENDING;
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public static ReliableCrossChainMessage decode(byte[] data) {
        return (ReliableCrossChainMessage) TLVUtils.decode(data, ReliableCrossChainMessage.class);
    }

    public String getInfo() {
        return StrUtil.format("idempotent: {}, originHash: {} , curHash: {}, retryTime: {}, timestamp: {}, status: {}",
                ObjectUtil.isEmpty(this.getIdempotentInfo()) ? "null" : this.getIdempotentInfo().getInfo(),
                ObjectUtil.isEmpty(this.getOriginalHash()) ? "null" : this.getOriginalHash(),
                ObjectUtil.isEmpty(this.getCurrentHash()) ? "null" : this.getCurrentHash(),
                this.getRetryTime(),
                ObjectUtil.isEmpty(this.getTxTimestamp()) ? "null" : this.getTxTimestamp(),
                ObjectUtil.isEmpty(this.getStatus()) ? "null" : this.getStatus());
    }
}
