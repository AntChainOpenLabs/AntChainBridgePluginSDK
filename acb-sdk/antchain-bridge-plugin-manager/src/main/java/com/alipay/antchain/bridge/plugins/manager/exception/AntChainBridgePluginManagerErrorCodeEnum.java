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

package com.alipay.antchain.bridge.plugins.manager.exception;

import lombok.Getter;

/**
 * Error code for {@code antchain-bridge-plugin-manager}
 *
 * <p>
 *     The {@code errorCode} field supposed to be hex and has two bytes.
 *     First byte represents the space code for project.
 *     Last byte represents the specific error scenarios.
 * </p>
 *
 * <p>
 *     Space code interval for {@code antchain-bridge-plugin-manager} is <b>from 40 to 7f</b>.
 * </p>
 */
@Getter
public enum AntChainBridgePluginManagerErrorCodeEnum {

    /**
     * Failed to create a plugin manager
     */
    PLUGIN_MANAGER_TYPE_NOT_SUPPORT("4001", "wrong plugin manager type"),

    /**
     * Failed to create a plugin manager
     */
    CREATE_PLUGIN_MANAGER_FIELD("4101", "create plugin manager failed"),

    /**
     * Two plugins has same blockchain product while loading and starting manager
     */
    BLOCKCHAIN_PRODUCT_CONFLICT_BETWEEN_PLUGINS("4102", "conflict product for plugins"),

    /**
     * Failed to start or load a plugin
     */
    PLUGIN_INIT_FAILED("4103", "init plugin failed"),

    /**
     * None plugin found for product
     */
    NONE_PLUGIN_FOUND_FOR_BLOCKCHAIN_PRODUCT("4104", "no plugin found for product"),

    /**
     * Failed to start a plugin
     */
    PLUGIN_START_FAILED("4105", "start plugin failed"),

    /**
     * Failed to stop a plugin
     */
    PLUGIN_STOP_FAILED("4106", "stop plugin failed"),

    /**
     * Failed to reload a plugin
     */
    PLUGIN_RELOAD_FAILED("4107", "reload plugin failed"),

    /**
     * Failed to unload a plugin
     */
    PLUGIN_UNLOAD_FAILED("4108", "unload plugin failed"),

    /**
     * Implementation of {@link com.alipay.antchain.bridge.plugins.manager.core.IBBCServiceFactory}
     * create {@link com.alipay.antchain.bridge.plugins.lib.BBCService} object failed.
     */
    CREATE_BBCSERVICE_FAILED("4201", "create bbcservice failed"),

    /**
     * Implementation of {@link com.alipay.antchain.bridge.plugins.lib.BBCService} in
     * plugin is illegal.
     */
    ILLEGAL_BBCSERVICE_IMPL("4202", "illegal bbcservice impl"),

    /**
     * Implementation of {@link com.alipay.antchain.bridge.plugins.manager.core.IHCDVSServiceFactory}
     * create {@link com.alipay.antchain.bridge.plugins.lib.HeteroChainDataVerifierService} object failed.
     */
    CREATE_HCDVSSERVICE_FAILED("4203", "create hcdvsservice failed"),

    /**
     * Implementation of {@link com.alipay.antchain.bridge.plugins.lib.HeteroChainDataVerifierService} in
     * plugin is illegal.
     */
    ILLEGAL_HCDVSSERVICE_IMPL("4204", "illegal hcdvsservice impl");

    /**
     * Error code for errors happened in project {@code antchain-bridge-plugin-manager}
     */
    private final String errorCode;

    /**
     * Every code has a short message to describe the error stuff
     */
    private final String shortMsg;

    AntChainBridgePluginManagerErrorCodeEnum(String errorCode, String shortMsg) {
        this.errorCode = errorCode;
        this.shortMsg = shortMsg;
    }
}
