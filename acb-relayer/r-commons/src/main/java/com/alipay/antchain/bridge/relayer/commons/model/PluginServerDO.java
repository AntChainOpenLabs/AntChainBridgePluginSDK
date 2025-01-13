package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PluginServerDO {
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PluginServerProperties {
        public static PluginServerProperties decode(byte[] data) {
            return JSON.parseObject(data, PluginServerProperties.class);
        }

        @JSONField(name = "server_cert")
        private String pluginServerCert;

        public byte[] encode() {
            return JSON.toJSONBytes(this);
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }
    }

    private Integer id;

    private String psId;

    private String address;

    private PluginServerStateEnum state;

    private List<String> productsSupported;

    private List<String> domainsServing;

    private PluginServerProperties properties;

    private Date gmtCreate;

    private Date gmtModified;
}
