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

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@TableName("validated_consensus_state")
@AllArgsConstructor
@NoArgsConstructor
public class ValidatedConsensusStateEntity extends BaseEntity {

    @TableField("blockchain_product")
    private String blockchainProduct;;

    @TableField("blockchain_id")
    private String blockchainId;

    @TableField("domain")
    private String domain;

    @TableField("tpbta_lane_key")
    private String tpbtaLaneKey;

    @TableField("ptc_service_id")
    private String ptcServiceId;

    @TableField("height")
    private String height;

    @TableField("hash")
    private String hash;

    @TableField("parent_hash")
    private String parentHash;

    @TableField("state_timestamp")
    private Date stateTimestamp;

    @TableField("raw_vcs")
    private byte[] rawVcs;
}
