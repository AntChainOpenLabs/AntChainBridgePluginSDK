package com.alipay.antchain.bridge.ptc.committee.node.server.interceptor;

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
                log.info("heartbeat from client without address found");
            } else {
                log.info("heartbeat from client {}:{}", clientAddr.getHostString(), clientAddr.getPort());
            }
        } else {
            if (ObjectUtil.isNull(clientAddr)) {
                log.debug("crosschain biz call from client without address found");
            } else {
                log.debug("crosschain biz call from client {}:{}", clientAddr.getHostString(), clientAddr.getPort());
            }
        }

        return next.startCall(call, headers);
    }
}
