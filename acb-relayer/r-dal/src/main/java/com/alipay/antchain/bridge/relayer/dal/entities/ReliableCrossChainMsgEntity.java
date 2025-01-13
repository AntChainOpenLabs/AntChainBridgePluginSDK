package com.alipay.antchain.bridge.relayer.dal.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("reliable_crosschain_msg_pool")
public class ReliableCrossChainMsgEntity extends BaseEntity {
    @TableField("sender_domain_name")
    private String senderDomainName;

    @TableField("sender_identity")
    private String senderIdentity;

    @TableField("receiver_domain_name")
    private String receiverDomainName;

    @TableField("receiver_identity")
    private String receiverIdentity;

    @TableField("nonce")
    private Long nonce;

    @TableField("tx_timestamp")
    private Long txTimestamp;

    @TableField("raw_tx")
    private byte[] rawTx;

    @TableField("status")
    private String status;

    @TableField("original_hash")
    private String originalHash;

    @TableField("current_hash")
    private String currentHash;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("retry_time")
    private Integer retryTime;
}
