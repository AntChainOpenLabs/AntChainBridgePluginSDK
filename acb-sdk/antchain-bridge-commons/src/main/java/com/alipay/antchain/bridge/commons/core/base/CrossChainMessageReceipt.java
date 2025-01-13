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

package com.alipay.antchain.bridge.commons.core.base;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class CrossChainMessageReceipt {

    /**
     * the transaction hash to commit {@code AuthMessage}
     */
    private String txhash;

    /**
     * is the transaction confirmed on ledger
     */
    private boolean confirmed;

    /**
     * is the transaction executed successfully
     */
    private boolean successful;

    /**
     * the reason message if transaction executed failed
     */
    private String errorMsg;

    private long txTimestamp;

    /**
     * the original transaction information
     */
    private byte[] rawTx;
}
