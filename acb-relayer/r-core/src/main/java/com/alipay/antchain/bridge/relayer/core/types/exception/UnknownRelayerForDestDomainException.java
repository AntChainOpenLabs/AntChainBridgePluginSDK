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

package com.alipay.antchain.bridge.relayer.core.types.exception;

import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;

public class UnknownRelayerForDestDomainException extends AntChainBridgeRelayerException {
    public UnknownRelayerForDestDomainException(String message, Throwable cause) {
        super(RelayerErrorCodeEnum.CORE_UNKNOWN_RELAYER_FOR_DEST_DOMAIN, message, cause);
    }

    public UnknownRelayerForDestDomainException(String message) {
        super(RelayerErrorCodeEnum.CORE_UNKNOWN_RELAYER_FOR_DEST_DOMAIN, message);
    }
}
