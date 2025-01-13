package com.alipay.antchain.bridge.relayer.engine.core;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务框架上下文
 */
@Getter
@Slf4j
public class ScheduleContext {

    public final static String NODE_ID_MODE_IP = "IP";

    public final static String NODE_ID_MODE_UUID = "UUID";

    private final String nodeIp;

    private final String nodeId;

    public ScheduleContext(String mode) {
        Set<InetAddress> inetAddresses = NetUtil.localAddressList(
                networkInterface -> {
                    try {
                        return !networkInterface.isLoopback() && !StrUtil.containsAny(networkInterface.getName(), "docker");
                    } catch (SocketException e) {
                        throw new RuntimeException(e);
                    }
                },
                inetAddress -> !inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4
        );
        InetAddress localAddress;
        if (ObjectUtil.isEmpty(inetAddresses)) {
            log.warn("none inet addresses satisfy the requirements and just use one of the localhost");
            localAddress = NetUtil.getLocalhost();
        } else {
            log.debug("all inet addresses satisfied is [ {} ]",
                    inetAddresses.stream().map(InetAddress::getHostAddress).collect(Collectors.joining(",")));
            localAddress = inetAddresses.iterator().next();
        }

        if (ObjectUtil.isNull(localAddress)) {
            throw new RuntimeException("null local ip");
        }
        this.nodeIp = localAddress.getHostAddress();

        if (StrUtil.equalsIgnoreCase(mode, NODE_ID_MODE_IP)) {
            this.nodeId = this.nodeIp;
        } else {
            this.nodeId = UUID.randomUUID().toString();
        }

        log.info("relayer node id for distribute tasks is {}", nodeId);
    }
}
