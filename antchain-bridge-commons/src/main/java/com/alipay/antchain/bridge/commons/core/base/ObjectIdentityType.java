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

package com.alipay.antchain.bridge.commons.core.base;

import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;

public enum ObjectIdentityType {

    /**
     * X.509 Subject Public Key Info.
     * <p>
     * According to <a href="https://datatracker.ietf.org/doc/html/rfc5280#section-4.1.2.7">RFC5280</a>,
     * we use the {@code X.509 Subject Public Key Info} structure to
     * represent the object identity.
     * </p>
     */
    X509_PUBLIC_KEY_INFO,

    BID;

    public static ObjectIdentityType parseFromValue(int value) {
        if (value == X509_PUBLIC_KEY_INFO.ordinal()) {
            return X509_PUBLIC_KEY_INFO;
        } else if (value == BID.ordinal()) {
            return BID;
        }
        throw new AntChainBridgeCommonsException(
                CommonsErrorCodeEnum.UNSUPPORTED_OID_TYPE_ERROR,
                "Invalid value for oid type: " + value
        );
    }

    public static ObjectIdentityType valueOf(Byte value) {
        switch (value) {
            case 0:
                return X509_PUBLIC_KEY_INFO;
            case 1:
                return BID;
            default:
                return null;
        }
    }
}
