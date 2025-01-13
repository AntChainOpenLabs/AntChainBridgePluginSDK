package com.alipay.antchain.bridge.ptc.committee.types.network;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProtocolHeaderEnum {

    GRPC("grpc://"),

    GRPCS("grpcs://");

    private final String header;

    public static ProtocolHeaderEnum parseFrom(String header) {
        for (ProtocolHeaderEnum headerEnum : values()) {
            if (headerEnum.getHeader().equals(header)) {
                return headerEnum;
            }
        }
        throw new IllegalArgumentException("Invalid header: " + header);
    }
}
