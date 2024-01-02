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

package com.alipay.antchain.bridge.commons;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.core.am.*;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import org.junit.Assert;
import org.junit.Test;

public class AuthMessageTest {

    private static final String RAW_MESSAGE_HEX_V1 = "8f5baede046f6bf700000000000000000000000000000000000000000000000097943daf4ed8cb477ef2e0048f5baede046f6bf797943daf4ed8cb477ef2e0040000000000000000000000000000000000000000000000000000000000000028000000010000000000000000000000007ef2e0048f5baede046f6bf797943daf4ed8cb4700000001";

    private static final String RAW_MESSAGE_HEX_ID_V1 = RAW_MESSAGE_HEX_V1.substring(200, 264);

    private static final String RAW_MESSAGE_HEX_V1_MOD32 = "8f5baede046f6bf700000000000000000000000000000000000000000000000097943daf4ed8cb477ef2e0048f5baede046f6bf797943daf4ed8cb477ef2e0040000000000000000000000000000000000000000000000000000000000000040000000010000000000000000000000007ef2e0048f5baede046f6bf797943daf4ed8cb4700000001";

    private static final String RAW_MESSAGE_HEX_PAYLOAD_V1_MOD32 = RAW_MESSAGE_HEX_V1_MOD32.substring(64, 128) + RAW_MESSAGE_HEX_V1_MOD32.substring(0, 64);

    private static final String RAW_MESSAGE_HEX_V2 = "97943daf4ed8cb477ef2e0048f5baede046f6bf797943daf4ed8cb477ef2e0048f5baede046f6bf70000002802000000010000000000000000000000007ef2e0048f5baede046f6bf797943daf4ed8cb4700000002";

    private static final String RAW_MESSAGE_HEX_ID_V2 = RAW_MESSAGE_HEX_V2.substring(98, 162);

    private static final String RAW_MESSAGE_HEX_PAYLOAD_V2 = RAW_MESSAGE_HEX_V2.substring(0, 80);

    @Test
    public void testAuthMessageV1Decode() {
        byte[] rawMessage = Convert.hexToBytes(RAW_MESSAGE_HEX_V1);
        IAuthMessage am = new AuthMessageV1();
        am.decode(rawMessage);

        Assert.assertEquals(1, am.getVersion());
        Assert.assertEquals(1, am.getUpperProtocol());
        Assert.assertEquals(RAW_MESSAGE_HEX_ID_V1, am.getIdentity().toHex());
        Assert.assertEquals(40, am.getPayload().length);
        Assert.assertEquals(RAW_MESSAGE_HEX_PAYLOAD_V2, HexUtil.encodeHexStr(am.getPayload()));

        rawMessage = Convert.hexToBytes(RAW_MESSAGE_HEX_V1_MOD32);
        am = new AuthMessageV1();
        am.decode(rawMessage);

        Assert.assertEquals(1, am.getVersion());
        Assert.assertEquals(1, am.getUpperProtocol());
        Assert.assertEquals(RAW_MESSAGE_HEX_ID_V1, am.getIdentity().toHex());
        Assert.assertEquals(64, am.getPayload().length);
        Assert.assertEquals(RAW_MESSAGE_HEX_PAYLOAD_V1_MOD32, HexUtil.encodeHexStr(am.getPayload()));
    }

    @Test
    public void testAuthMessageV1Encode() {
        AbstractAuthMessage am = new AuthMessageV1();
        am.setUpperProtocol(1);
        am.setIdentity(CrossChainIdentity.fromHexStr(RAW_MESSAGE_HEX_ID_V1));
        am.setPayload(HexUtil.decodeHex(RAW_MESSAGE_HEX_PAYLOAD_V2));

        Assert.assertEquals(RAW_MESSAGE_HEX_V1, HexUtil.encodeHexStr(am.encode()));

        am = new AuthMessageV1();
        am.setUpperProtocol(1);
        am.setIdentity(CrossChainIdentity.fromHexStr(RAW_MESSAGE_HEX_ID_V1));
        am.setPayload(HexUtil.decodeHex(RAW_MESSAGE_HEX_PAYLOAD_V1_MOD32));

        Assert.assertEquals(RAW_MESSAGE_HEX_V1_MOD32, HexUtil.encodeHexStr(am.encode()));
    }

    @Test
    public void testAuthMessageV2Decode() {
        byte[] rawMessage = Convert.hexToBytes(RAW_MESSAGE_HEX_V2);

        AuthMessageV2 am = new AuthMessageV2();
        am.decode(rawMessage);

        Assert.assertEquals(2, am.getVersion());
        Assert.assertEquals(1, am.getUpperProtocol());
        Assert.assertEquals(RAW_MESSAGE_HEX_ID_V2, am.getIdentity().toHex());
        Assert.assertEquals(40, am.getPayload().length);
        Assert.assertEquals(RAW_MESSAGE_HEX_PAYLOAD_V2, HexUtil.encodeHexStr(am.getPayload()));
        Assert.assertEquals(AuthMessageTrustLevelEnum.NEGATIVE_TRUST, am.getTrustLevel());
    }

    @Test
    public void testAuthMessageV2Encode() {
        AuthMessageV2 am = new AuthMessageV2();
        am.setUpperProtocol(1);
        am.setIdentity(CrossChainIdentity.fromHexStr(RAW_MESSAGE_HEX_ID_V2));
        am.setPayload(HexUtil.decodeHex(RAW_MESSAGE_HEX_PAYLOAD_V2));
        am.setTrustLevel(AuthMessageTrustLevelEnum.NEGATIVE_TRUST);

        Assert.assertEquals(RAW_MESSAGE_HEX_V2, HexUtil.encodeHexStr(am.encode()));
    }

    @Test
    public void testAuthMessageFactoryCreate() {
        IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(HexUtil.decodeHex(RAW_MESSAGE_HEX_V2));
        Assert.assertEquals(2, authMessage.getVersion());
        Assert.assertEquals(1, authMessage.getUpperProtocol());
        Assert.assertEquals(RAW_MESSAGE_HEX_ID_V2, authMessage.getIdentity().toHex());
        Assert.assertEquals(40, authMessage.getPayload().length);
        Assert.assertEquals(RAW_MESSAGE_HEX_PAYLOAD_V2, HexUtil.encodeHexStr(authMessage.getPayload()));
    }
}
