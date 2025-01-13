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

package com.alipay.antchain.bridge.relayer.commons.exception;

import lombok.Getter;

@Getter
public class UcpValidationException extends AntChainBridgeRelayerException {

    public UcpValidationException(String ucpId, String domain, String tpbtaLaneKey, String message) {
        super(RelayerErrorCodeEnum.SERVICE_VALIDATION_UCP_VERIFY_EXCEPTION,
                "failed to verify ucp {} from blockchain {} and tpbta {} : {}", ucpId, domain, tpbtaLaneKey, message);
        this.ucpId = ucpId;
        this.domain = domain;
        this.tpbtaLaneKey = tpbtaLaneKey;
    }

    private final String ucpId;

    private final String domain;

    private final String tpbtaLaneKey;
}
