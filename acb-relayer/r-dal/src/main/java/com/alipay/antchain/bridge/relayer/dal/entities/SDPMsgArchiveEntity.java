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

package com.alipay.antchain.bridge.relayer.dal.entities;

import java.math.BigInteger;

import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sdp_msg_archive")
public class SDPMsgArchiveEntity extends BaseEntity {
    @TableField("auth_msg_id")
    private Long authMsgId;

    @TableField("version")
    private Integer version;

    @TableField("message_id")
    private String messageId;

    @TableField("atomic_flag")
    private Integer atomicFlag;

    @TableField("nonce")
    private String nonce;

    @TableField("sender_blockchain_product")
    private String senderBlockchainProduct;

    @TableField("sender_instance")
    private String senderBlockchainId;

    @TableField("sender_domain_name")
    private String senderDomainName;

    @TableField("sender_identity")
    private String senderId;

    @TableField("sender_amclient_contract")
    private String senderAMClientContract;

    @TableField("receiver_blockchain_product")
    private String receiverBlockchainProduct;

    @TableField("receiver_instance")
    private String receiverBlockchainId;

    @TableField("receiver_domain_name")
    private String receiverDomainName;

    @TableField("receiver_identity")
    private String receiverId;

    @TableField("receiver_amclient_contract")
    private String receiverAMClientContract;

    @TableField("msg_sequence")
    private Long msgSequence;

    @TableField("process_state")
    private SDPMsgProcessStateEnum processState;

    @TableField("tx_hash")
    private String txHash;

    @TableField("tx_success")
    private Boolean txSuccess;

    @TableField("tx_fail_reason")
    private String txFailReason;

    @TableField("timeout_measure")
    private Integer timeoutMeasureEnum;

    @TableField("timeout")
    private BigInteger timeout;
}
