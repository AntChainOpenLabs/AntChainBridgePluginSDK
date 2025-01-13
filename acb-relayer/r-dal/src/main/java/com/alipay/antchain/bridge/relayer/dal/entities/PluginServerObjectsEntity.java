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

import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("plugin_server_objects")
public class PluginServerObjectsEntity extends BaseEntity {

    @TableField("ps_id")
    private String psId;

    @TableField("address")
    private String address;

    @TableField("state")
    private PluginServerStateEnum state;

    @TableField("products_supported")
    private String products;

    @TableField("domains_serving")
    private String domains;

    @TableField("properties")
    private byte[] properties;
}
