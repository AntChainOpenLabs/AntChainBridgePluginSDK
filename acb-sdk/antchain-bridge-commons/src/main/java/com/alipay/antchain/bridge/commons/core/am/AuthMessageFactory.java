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

package com.alipay.antchain.bridge.commons.core.am;

import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;

public class AuthMessageFactory {

    public static IAuthMessage createAuthMessage(byte[] rawMessage) {
        IAuthMessage authMessage = createAbstractAuthMessage(AbstractAuthMessage.decodeVersionFromBytes(rawMessage));
        authMessage.decode(rawMessage);
        return authMessage;
    }

    public static IAuthMessage createAuthMessage(int version, byte[] identity, int upperProtocol, byte[] payload) {
        AbstractAuthMessage authMessage = createAbstractAuthMessage(version);

        authMessage.setUpperProtocol(upperProtocol);
        authMessage.setIdentity(new CrossChainIdentity(identity));
        authMessage.setPayload(payload);

        return authMessage;
    }

    public static IAuthMessage createAuthMessage(int version) {
        return createAbstractAuthMessage(version);
    }

    private static AbstractAuthMessage createAbstractAuthMessage(int version) {
        AbstractAuthMessage authMessage;
        switch (version) {
            case AuthMessageV1.MY_VERSION:
                authMessage = new AuthMessageV1();
                break;
            case AuthMessageV2.MY_VERSION:
                authMessage = new AuthMessageV2();
                break;
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.INCORRECT_AUTH_MESSAGE_ERROR,
                        String.format("wrong version: %d", version)
                );
        }
        return authMessage;
    }
}
