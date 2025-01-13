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

package com.alipay.antchain.bridge.commons.core.ptc;

import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public enum PTCTypeEnum {

    /**
     * {@code EXTERNAL_VERIFIER} means no PTC involved in AntChain Bridge cross-chain procedure.
     * AntChain Bridge will process cross-chain message without TPBTA and
     * TP-Proof. So to AntChain Bridge, this kind messages are unsafe.
     * Cross-chain security needs to be agreed upon between the sending chain and the receiving chain.
     * The cross-chain safety counts on the {@code BBCService} implementation, for example,
     * Auth Message contract use some other protocol to make sure the message is
     * safe to execute.
     */
    EXTERNAL_VERIFIER,
    /**
     * Multi-nodes cluster with multi-keys to endorse the cross-chain stuff
     * and the keys would never change.
     */
    COMMITTEE,

    /**
     * Relay chain type
     */
    RELAY_CHAIN;

    @TLVCreator
    public static PTCTypeEnum valueOf(Byte value) {
        switch (value) {
            case 0:
                return EXTERNAL_VERIFIER;
            case 1:
                return COMMITTEE;
            case 3:
                return RELAY_CHAIN;
            default:
                return null;
        }
    }

    public static PTCTypeEnum parseFrom(@NonNull String name) {
        switch (name.toUpperCase()) {
            case "EXTERNAL_VERIFIER":
                return EXTERNAL_VERIFIER;
            case "COMMITTEE":
                return COMMITTEE;
            case "RELAY_CHAIN":
                return RELAY_CHAIN;
            default:
                return null;
        }
    }
}
