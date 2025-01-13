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

package com.alipay.antchain.bridge.relayer.bootstrap.utils;

import java.io.IOException;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer;

public class MyRedisServer extends RedisServer {

    public MyRedisServer(RedisExecProvider redisExecProvider, Integer port) throws IOException {
        super(redisExecProvider, port);
    }

    @Override
    protected String redisReadyPattern() {
        if (SystemUtil.getOsInfo().isMac() && StrUtil.equalsAnyIgnoreCase(SystemUtil.getOsInfo().getArch(), "x86_64", "aarch64")) {
            return ".*Ready to accept connections tcp.*";
        }
        return super.redisReadyPattern();
    }
}
