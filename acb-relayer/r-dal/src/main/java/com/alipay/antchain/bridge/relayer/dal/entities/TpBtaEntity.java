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
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@TableName("tpbta")
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class TpBtaEntity extends BaseEntity {

    @TableField("blockchain_product")
    private String blockchainProduct;

    @TableField("blockchain_id")
    private String blockchainId;

    @TableField("ptc_service_id")
    private String ptcServiceId;

    @TableField("version")
    private Integer version;

    @TableField("bta_subject_version")
    private Integer btaSubjectVersion;

    @TableField("sender_domain")
    private String senderDomain;

    @TableField("sender_id")
    private String senderId;

    @TableField("receiver_domain")
    private String receiverDomain;

    @TableField("receiver_id")
    private String receiverId;

    @TableField("tpbta_version")
    private Integer tpbtaVersion;

    @TableField("ptc_verify_anchor_version")
    private String ptcVerifyAnchorVersion;

    @TableField("raw_tpbta")
    private byte[] rawTpbta;
}
