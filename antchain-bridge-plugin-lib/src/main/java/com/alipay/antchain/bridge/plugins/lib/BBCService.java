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

package com.alipay.antchain.bridge.plugins.lib;


import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * By {@code BBCService}, developers can mark their
 * implementation class as the BBC plugin.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
@Documented
public @interface BBCService {

    /**
     * the blockchain product name like {@code Ethereum} etc.
     *
     * @return {@link String[]}
     */
    String[] products() default {};

    /**
     * the unique identity of the plugin
     *
     * @return {@link String[]}
     */
    String[] pluginId() default {};
}
