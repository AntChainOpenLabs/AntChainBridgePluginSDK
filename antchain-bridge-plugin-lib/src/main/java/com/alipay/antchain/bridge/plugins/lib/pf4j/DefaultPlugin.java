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

package com.alipay.antchain.bridge.plugins.lib.pf4j;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Default plugin class
 */
public class DefaultPlugin extends Plugin {
    public DefaultPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        System.out.println("DefaultPlugin.start()");
    }

    @Override
    public void stop() {
        System.out.println("DefaultPlugin.stop()");
    }
}
