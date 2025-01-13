package com.alipay.antchain.bridge.ptc.committee.types.network;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EndpointAddress {

    private String host;

    private int port;

    private ProtocolHeaderEnum protocolHeader;

    public EndpointAddress(String url) {
        url = StrUtil.trim(url);
        int headerIdx = StrUtil.lastIndexOfIgnoreCase(url, "/");
        protocolHeader = ProtocolHeaderEnum.parseFrom(StrUtil.sub(url, 0, headerIdx + 1));
        String address = StrUtil.sub(url, headerIdx + 1, url.length());
        String[] arr = address.split(":");
        if (arr.length != 2) {
            throw new IllegalArgumentException("Invalid url: " + url);
        }
        host = arr[0];
        port = Integer.parseInt(arr[1]);
    }

    public String getUrl() {
        return protocolHeader.getHeader() + host + ":" + port;
    }
}
