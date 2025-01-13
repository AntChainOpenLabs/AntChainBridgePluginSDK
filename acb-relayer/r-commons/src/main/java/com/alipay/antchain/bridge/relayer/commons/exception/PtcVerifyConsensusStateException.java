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

package com.alipay.antchain.bridge.relayer.commons.exception;

public class PtcVerifyConsensusStateException extends AntChainBridgeRelayerException {

    public PtcVerifyConsensusStateException(String longMsg) {
        super(RelayerErrorCodeEnum.CORE_PTC_SERVICE_VERIFY_CONSENSUS_STATE_FAILED, longMsg);
    }

    public PtcVerifyConsensusStateException(String formatStr, Object... objects) {
        super(RelayerErrorCodeEnum.CORE_PTC_SERVICE_VERIFY_CONSENSUS_STATE_FAILED, formatStr, objects);
    }

    public PtcVerifyConsensusStateException(Throwable throwable, String formatStr, Object... objects) {
        super(RelayerErrorCodeEnum.CORE_PTC_SERVICE_VERIFY_CONSENSUS_STATE_FAILED, throwable, formatStr, objects);
    }

    public PtcVerifyConsensusStateException(String longMsg, Throwable throwable) {
        super(RelayerErrorCodeEnum.CORE_PTC_SERVICE_VERIFY_CONSENSUS_STATE_FAILED, longMsg, throwable);
    }
}
