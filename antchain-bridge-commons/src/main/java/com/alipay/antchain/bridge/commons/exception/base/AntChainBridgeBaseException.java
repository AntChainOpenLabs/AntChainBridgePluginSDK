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

package com.alipay.antchain.bridge.commons.exception.base;

import lombok.Getter;

@Getter
public class AntChainBridgeBaseException extends RuntimeException {

    /**
     * Error code for antchain-bridge SDK
     */
    private final String code;

    /**
     * Short message to
     */
    private final String msg;

    /**
     * {@code AntChainBridgeBaseException} is the base exception for whole antchain-bridge project.
     *
     * <p>
     * Other business exceptions need to be extended from {@code AntChainBridgeBaseException}.
     * </p>
     *
     * @param code    error code designed by business project
     * @param msg     message bound with code
     * @param message long message for your logger
     */
    public AntChainBridgeBaseException(String code, String msg, String message) {
        super(message);
        this.code = code;
        this.msg = msg;
    }

    /**
     * {@code AntChainBridgeBaseException} is the base exception for whole antchain-bridge project.
     *
     * <p>
     * Other business exceptions need to be extended from {@code AntChainBridgeBaseException}.
     * </p>
     *
     * @param code    error code designed by business project
     * @param msg     message bound with code
     * @param message long message for your logger
     * @param cause   business exception
     */
    public AntChainBridgeBaseException(String code, String msg, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.msg = msg;
    }
}
