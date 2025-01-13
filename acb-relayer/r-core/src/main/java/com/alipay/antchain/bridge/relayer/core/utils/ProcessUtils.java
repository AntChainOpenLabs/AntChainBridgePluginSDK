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

package com.alipay.antchain.bridge.relayer.core.utils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.hutool.core.collection.ListUtil;
import org.slf4j.Logger;

public class ProcessUtils {

    public static void waitAllFuturesDone(String blockchainProduct, String blockchainId, List<Future> futures, Logger log) {
        // 等待执行完成
        do {
            for (Future future : ListUtil.reverse(ListUtil.toList(futures))) {
                try {
                    future.get(30 * 1000L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.error("worker interrupted exception for blockchain {}-{}.", blockchainProduct, blockchainId, e);
                } catch (ExecutionException e) {
                    log.error("worker execution fail for blockchain {}-{}.", blockchainProduct, blockchainId, e);
                } catch (TimeoutException e) {
                    log.warn("worker query timeout exception for blockchain {}-{}.", blockchainProduct, blockchainId, e);
                } finally {
                    if (future.isDone()) {
                        futures.remove(future);
                    }
                }
            }
        } while (!futures.isEmpty());
    }

    public static void waitAllFuturesDone(List<Future> futures, Logger log) {
        // 等待执行完成
        do {
            for (Future future : ListUtil.reverse(ListUtil.toList(futures))) {
                try {
                    future.get(30 * 1000L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.error("worker interrupted exception.", e);
                } catch (ExecutionException e) {
                    log.error("worker execution failed.", e);
                } catch (TimeoutException e) {
                    log.warn("worker query timeout exception", e);
                } finally {
                    if (future.isDone()) {
                        futures.remove(future);
                    }
                }
            }
        } while (!futures.isEmpty());
    }


}
