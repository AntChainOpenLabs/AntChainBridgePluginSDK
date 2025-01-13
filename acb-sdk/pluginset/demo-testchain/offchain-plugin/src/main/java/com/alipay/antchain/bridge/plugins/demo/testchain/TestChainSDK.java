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

package com.alipay.antchain.bridge.plugins.demo.testchain;

import java.util.Arrays;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * SDK implementation here.
 * Just a demo here.
 */
public class TestChainSDK {

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TestChainBlock {

        private Long height;

        private Long timestamp;

        private List<TestChainReceipt> receipts;

        public byte[] getBlockHash() {
            return DigestUtil.sha256(this.height.toString());
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TestChainReceipt {

        private String txhash;

        private String contract;

        private String topic;

        private String logValue;

        // a result when you call some contract
        private Object result;

        public byte[] toBytes() {
            return JSON.toJSONBytes(this);
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TestChainTransaction {

        private String txhash;

        private boolean confirmed;

        private boolean successToExecute;

        private String errorMsg;
    }

    public void initSDK(byte[] conf) {
        System.out.println("sdk init with: " + new String(conf));
        // do something
    }

    public void shutdown() {
        // do something
    }

    public TestChainBlock queryABlock(Long height) {
        return new TestChainBlock(
                height,
                System.currentTimeMillis(),
                ListUtil.of(
                        new TestChainReceipt(
                                DigestUtil.sha256Hex("txhash"),
                                "am",
                                "SendAuthMessage",
                                HexUtil.encodeHexStr(MockDataUtils.generateAM().encode()),
                                ""
                        )
                )
        );
    }

    public TestChainTransaction queryTx(String txhash) {
        return new TestChainTransaction(
                txhash,
                true,
                true,
                "success"
        );
    }

    public Long queryLatestHeight() {
        return System.currentTimeMillis() / 10000;
    }

    public TestChainReceipt syncCallContract(String contract, String method, List<Object> args) {
        System.out.printf("you call contract %s::%s with args %s\n", contract, method, Arrays.toString(args.toArray()));
        return new TestChainReceipt(
                DigestUtil.sha256Hex("txhash"),
                contract,
                "some_topic",
                "",
                "0"
        );
    }
}
