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

package com.alipay.antchain.bridge.plugins.spi.ptc.core;

public interface ILedgerParser {

    /**
     * Parse raw cross-chain message from the given ledger data.
     *
     * <p>
     *     This {@code ledgerData} is supposed to be the ledger structure
     *     containing the message created by the cross-chain contracts like
     *     the {@code Receipt} or {@code Event}, etc. This method can deserialize
     *     the {@code ledgerData} into an instance and read the message out.
     * </p>
     *
     * @param ledgerData serialized ledger data
     * @return bytes raw message
     */
    byte[] parseMessageFromLedgerData(byte[] ledgerData);
}
