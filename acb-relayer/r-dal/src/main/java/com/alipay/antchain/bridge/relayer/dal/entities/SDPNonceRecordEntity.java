/*
 * Copyright 2024 Ant Group
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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@TableName("sdp_nonce_record")
@AllArgsConstructor
@NoArgsConstructor
public class SDPNonceRecordEntity extends BaseEntity {

    @TableField("message_id")
    private String messageId;

    @TableField("sender_domain")
    private String senderDomain;

    @TableField("sender_identity")
    private String senderIdentity;

    @TableField("receiver_domain")
    private String receiverDomain;

    @TableField("receiver_identity")
    private String receiverIdentity;

    @TableField("nonce")
    private String nonce;

    @TableField("hash_val")
    private String hashVal;
}
