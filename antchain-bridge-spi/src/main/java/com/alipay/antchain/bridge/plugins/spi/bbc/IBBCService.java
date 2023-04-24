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

package com.alipay.antchain.bridge.plugins.spi.bbc;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.plugins.spi.bbc.core.read.IAntChainBridgeDataReader;
import com.alipay.antchain.bridge.plugins.spi.bbc.core.write.IAntChainBridgeDataWriter;
import com.alipay.antchain.bridge.plugins.spi.utils.pf4j.Pf4jMarker;

/**
 * An {@code IBBCService} is required in the plugin developed
 * for a specific type of blockchain. A {@code IBBCService}
 * instance corresponds to a specific blockchain network.
 * {@code IBBCService} handles all communication requests
 * with the corresponded blockchain network. And every {@code IBBCService}
 * would register to the {@code AntChainBridgePluginManager} which is
 * part of relayer.
 *
 * <p>
 *     Developers need to implement this interface to docking
 *     your blockchain to AntChainBridge. And developers should know
 *     the follows points:
 * </p>
 *
 * <ol>
 *     <li>The implementation class must annotated with annotation {@code BBCService}. </li>
 *     <li>Only one {@code IBBCService} is supposed to be implemented in the plugin. </li>
 * </ol>
 *
 * <p>
 *     All {@code IBBCService} functions are split into two
 *     interfaces {@link IAntChainBridgeDataWriter} and {@link IAntChainBridgeDataReader}.
 *     {@link IAntChainBridgeDataWriter} and {@link IAntChainBridgeDataReader} need to communicate
 *     with the blockchain network, usually through the blockchain SDK.
 * </p>
 */
public interface IBBCService extends Pf4jMarker, IAntChainBridgeDataWriter, IAntChainBridgeDataReader {

    /**
     * Start up the service with the context of {@code IBBCService}.
     * A BBCService context should be initialized or reload from the
     * input {@code context}. Including system contracts objects,
     * blockchain client, etc.
     *
     * <p>
     *     From the actual situation, this method is usually
     *     used to initialize the SDK object of the blockchain
     *     and reload the context of a {@code IBBCService}.
     *     For example, the input {@code context} contains the configuration
     *     for SDK instantiate. And method {@code startup} would initialize
     *     the SDK object of the blockchain with the {@code context}.
     *     <b>Most important is that {@code IBBCService} should ready for request
     *       after the execution of method {@code startup}.</b>
     * </p>
     *
     * <p>
     *     Mostly, the {@code context}. consists of the following parts:
     * </p>
     *
     * <ol>
     *     <li>System contracts states like {@link com.alipay.antchain.bridge.commons.bbc.syscontract.PTCContract}, etc. </li>
     *     <li>Some configurations specific to current blockchain like the
     *     configuration to startup the SDK object. The format of this part
     *     configuration is up to developer. For example like following:
     *     <ol>
     *          <li>Network settings like blockchain nodes for connection of client, TLS certificates, etc.</li>
     *          <li>The blockchain account for this {@code IBBCService} to
     *              connect with the blockchain network. Like a private key.
     *              It's also the account of Relayer in AntChainBridge serving the blockchain.
     *              Mostly, the system contracts should be owned by this account.</li>
     *     </ol>
     *     </li>
     * </ol>
     *
     * @param context the context object.
     *                 please check the comments of interface {@link AbstractBBCContext}.
     */
    void startup(AbstractBBCContext context);

    /**
     * Shutdown the service and release all resources associated with the service.
     *
     * <p>
     *     This method requires the following jobs done:
     * </p>
     *
     * <ol>
     *     <li>If the {@code IBBCService} has some tasks in process,
     *     finish them before shutdown the service. </li>
     *     <li>If the SDK client needs to actively close, close the SDK
     *     client before shutdown the service. </li>
     * </ol>
     *
     * <b>After the execution of this method, </b>
     */
    void shutdown();

    /**
     * Get the {@link AbstractBBCContext} associated with the service.
     *
     * @return the context of the service
     */
    AbstractBBCContext getContext();
}
