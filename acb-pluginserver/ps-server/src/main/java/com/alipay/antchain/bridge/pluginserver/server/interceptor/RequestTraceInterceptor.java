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

package com.alipay.antchain.bridge.pluginserver.server.interceptor;

import java.net.InetSocketAddress;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "req-trace")
public class RequestTraceInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        InetSocketAddress clientAddr = (InetSocketAddress) call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (StrUtil.equalsIgnoreCase(call.getMethodDescriptor().getBareMethodName(), "heartbeat")) {
            if (ObjectUtil.isNull(clientAddr)) {
                log.info("heartbeat from relayer without address found");
            } else {
                log.info("heartbeat from relayer {}:{}", clientAddr.getHostString(), clientAddr.getPort());
            }
        } else if (StrUtil.equalsIgnoreCase(call.getMethodDescriptor().getBareMethodName(), "bbcCall")) {
            if (ObjectUtil.isNull(clientAddr)) {
                log.debug("bbc call from relayer without address found");
            } else {
                log.debug("bbc call from relayer {}:{}", clientAddr.getHostString(), clientAddr.getPort());
            }
        }

        return next.startCall(call, headers);
    }
}
