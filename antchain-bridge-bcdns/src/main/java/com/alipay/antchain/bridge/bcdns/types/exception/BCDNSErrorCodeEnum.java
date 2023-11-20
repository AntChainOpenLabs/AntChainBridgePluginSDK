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

package com.alipay.antchain.bridge.bcdns.types.exception;

import lombok.Getter;

/**
 * Error code for {@code antchain-bridge-bcdns}
 *
 * <p>
 *     The {@code errorCode} field supposed to be hex and has two bytes.
 *     First byte represents the space code for project.
 *     Last byte represents the specific error scenarios.
 * </p>
 *
 * <p>
 *     Space code interval for {@code antchain-bridge-bcdns} is from 80 to 9f.
 * </p>
 */
@Getter
public enum BCDNSErrorCodeEnum {
    BCDNS_SIGN_REQUEST_FAILED("8001", "client failed to sign"),

    BCDNS_CLIENT_INIT_FAILED("8002", "client init failed"),

    BCDNS_QUERY_RELAYER_INFO_FAILED("8003", "query relayer info failed");

    /**
     * Error code for errors happened in project {@code antchain-bridge-commons}
     */
    private final String errorCode;

    /**
     * Every code has a short message to describe the error stuff
     */
    private final String shortMsg;

    BCDNSErrorCodeEnum(String errorCode, String shortMsg) {
        this.errorCode = errorCode;
        this.shortMsg = shortMsg;
    }
}
