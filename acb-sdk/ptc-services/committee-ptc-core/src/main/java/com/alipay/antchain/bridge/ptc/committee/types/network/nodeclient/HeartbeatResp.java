package com.alipay.antchain.bridge.ptc.committee.types.network.nodeclient;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HeartbeatResp {

    private String committeeId;

    private String nodeId;

    private List<String> products;
}
