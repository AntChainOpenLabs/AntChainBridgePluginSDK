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

import java.util.Collections;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.exception.base.AntChainBridgeBaseException;
import org.junit.Assert;
import org.junit.Test;

public class AntChainBridgeRulesAspectTest {

    private static final String OVERSIZE_DOMAIN = String.join("", Collections.nCopies(128, "a"));

    @Test
    public void testCrossChainDomain() {

        Assert.assertThrows(AntChainBridgeBaseException.class, () -> {
                new CrossChainDomain(OVERSIZE_DOMAIN);
        });

        new CrossChainDomain("test");
    }
}
