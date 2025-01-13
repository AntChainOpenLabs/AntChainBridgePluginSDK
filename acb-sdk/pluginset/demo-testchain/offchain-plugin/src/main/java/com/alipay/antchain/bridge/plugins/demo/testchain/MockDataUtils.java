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

package com.alipay.antchain.bridge.plugins.demo.testchain;

import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.sdp.ISDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;

public class MockDataUtils {

    public static IAuthMessage generateAM() {
        ISDPMessage sdpMessage = SDPMessageFactory.createSDPMessage(
                1,
                "messageId".getBytes(),
                "receiverDomain",
                DigestUtil.sha256("receiver"),
                -1,
                "awesome antchain-bridge".getBytes()
        );

        return AuthMessageFactory.createAuthMessage(
                1,
                DigestUtil.sha256("sender"),
                0,
                sdpMessage.encode()
        );
    }

    public static CrossChainMessage generateCCMsg(Long height, byte[] ledgerData, byte[] proof) {
        return CrossChainMessage.createCrossChainMessage(
                CrossChainMessage.CrossChainMessageType.AUTH_MSG,
                height,
                System.currentTimeMillis(),
                DigestUtil.sha256(height.toString()),
                generateAM().encode(),
                ledgerData,
                proof,
                DigestUtil.sha256(height.toString())
        );
    }
}
