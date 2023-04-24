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

package com.alipay.antchain.bridge.plugins.manager.core;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

@Getter
@Setter
public abstract class AbstractAntChainBridgePlugin implements IAntChainBridgePlugin {

    private IBBCServiceFactory bbcServiceFactory;

    private AntChainBridgePluginState state;

    public AbstractAntChainBridgePlugin() {
        this.state = AntChainBridgePluginState.INIT;
    }

    @Synchronized
    public AntChainBridgePluginState getState() {
            return state;
    }

    @Synchronized
    public void setState(AntChainBridgePluginState state) {
            this.state = state;
    }
}
