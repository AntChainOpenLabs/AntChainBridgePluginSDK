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

package com.alipay.antchain.bridge.plugins.spi.bbc.core.read;

/**
 * Through {@code ISDPReader}, you can query the state of the SDPContract.
 */
public interface ISDPReader {

    /**
     * Method {@code querySDPMessageSeq} query the sequence number
     * of ordered message for the channel identified by the
     * parameters {@code senderDomain}, {@code fromAddress}, {@code receiverDomain}
     * and {@code toAddress}.
     *
     * @param senderDomain   blockchain domain where sender from
     * @param fromAddress    sender address in 32B hex
     * @param receiverDomain blockchain domain where receiver from
     * @param toAddress      receiver address in 32B hex
     * @return long sequence number start from zero.
     */
    long querySDPMessageSeq(String senderDomain, String fromAddress, String receiverDomain, String toAddress);
}
