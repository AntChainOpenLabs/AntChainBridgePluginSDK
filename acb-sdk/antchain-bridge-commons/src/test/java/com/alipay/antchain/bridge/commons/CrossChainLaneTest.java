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

package com.alipay.antchain.bridge.commons;

import cn.hutool.core.util.RandomUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import org.junit.Assert;
import org.junit.Test;

public class CrossChainLaneTest {

    @Test
    public void test() {
        CrossChainLane lane = new CrossChainLane(
                new CrossChainDomain("test1"), new CrossChainDomain("test2"),
                new CrossChainIdentity(RandomUtil.randomBytes(32)),
                new CrossChainIdentity(RandomUtil.randomBytes(32))
        );
        Assert.assertNotNull(lane.getLaneKey());
        CrossChainLane lane1 = CrossChainLane.fromLaneKey(lane.getLaneKey());
        Assert.assertEquals(
                "test1",
                lane1.getSenderDomain().getDomain()
        );
        Assert.assertEquals(
                "test2",
                lane1.getReceiverDomain().getDomain()
        );
        Assert.assertArrayEquals(
                lane.getSenderId().getRawID(),
                lane1.getSenderId().getRawID()
        );
        Assert.assertArrayEquals(
                lane.getReceiverId().getRawID(),
                lane1.getReceiverId().getRawID()
        );
        Assert.assertTrue(lane.isValidated());

        lane = new CrossChainLane(new CrossChainDomain("test1"), new CrossChainDomain("test2"));
        Assert.assertNotNull(lane.getLaneKey());
        lane1 = CrossChainLane.fromLaneKey(lane.getLaneKey());
        Assert.assertEquals(
                "test1",
                lane1.getSenderDomain().getDomain()
        );
        Assert.assertEquals(
                "test2",
                lane1.getReceiverDomain().getDomain()
        );
        Assert.assertNull(
                lane1.getSenderId()
        );
        Assert.assertNull(
                lane1.getReceiverId()
        );

        Assert.assertTrue(lane.isValidated());
        lane.setSenderId(new CrossChainIdentity(RandomUtil.randomBytes(32)));
        Assert.assertFalse(lane.isValidated());
    }
}
