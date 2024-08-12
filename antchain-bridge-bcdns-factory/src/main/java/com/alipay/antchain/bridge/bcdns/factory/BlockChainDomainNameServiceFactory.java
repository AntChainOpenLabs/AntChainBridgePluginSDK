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

package com.alipay.antchain.bridge.bcdns.factory;

import com.alipay.antchain.bridge.bcdns.embedded.client.EmbeddedBCDNSClient;
import com.alipay.antchain.bridge.bcdns.impl.bif.BifBCDNSClient;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.exception.AntChainBridgeBCDNSException;
import com.alipay.antchain.bridge.bcdns.types.exception.BCDNSErrorCodeEnum;

public class BlockChainDomainNameServiceFactory {

    public static IBlockChainDomainNameService create(BCDNSTypeEnum type, byte[] rawConfig) {
        switch (type) {
            case BIF:
                return BifBCDNSClient.generateFrom(rawConfig);
            case EMBEDDED:
                return EmbeddedBCDNSClient.generateFrom(rawConfig);
            default:
                throw new AntChainBridgeBCDNSException(
                        BCDNSErrorCodeEnum.BCDNS_TYPE_UNKNOWN,
                        "unknown bcdns type : " + type.getCode()
                );
        }
    }
}
