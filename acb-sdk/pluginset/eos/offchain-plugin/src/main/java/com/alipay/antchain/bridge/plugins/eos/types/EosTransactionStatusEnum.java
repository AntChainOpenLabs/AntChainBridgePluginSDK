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

package com.alipay.antchain.bridge.plugins.eos.types;

public enum EosTransactionStatusEnum {
    EXECUTED("executed"),
    SOFTFAIL("soft_fail"),
    HARDFAIL("hard_fail"),
    DELAYED("delayed"),
    EXPIRED("expired"),
    UNKNOWN("unknown");

    public static EosTransactionStatusEnum parse(String status) {
        switch (status.toLowerCase()) {
            case "executed":
                return EXECUTED;
            case "soft_fail":
                return SOFTFAIL;
            case "hard_fail":
                return HARDFAIL;
            case "delayed":
                return DELAYED;
            case "expired":
                return EXPIRED;
            default:
                return UNKNOWN;
        }
    }

    private final String status;

    EosTransactionStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}