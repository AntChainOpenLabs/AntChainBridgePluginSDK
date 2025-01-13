package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.List;

import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PluginServerInfo {

    private PluginServerStateEnum state;

    private List<String> products;

    private List<String> domains;

    public PluginServerInfo(List<String> products, List<String> domains){
        this.products = products;
        this.domains = domains;
    }
}

