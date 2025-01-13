package com.alipay.antchain.bridge.ptc.committee.supervisor.cli;


import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication(scanBasePackages = {"com.alipay.antchain.bridge.ptc.committee.supervisor.cli"})
public class Launcher {

    public static void main(String[] args) {
        List<String> propList = ListUtil.toList("logging.level.root=ERROR");
        if (ObjectUtil.isNotEmpty(args)) {
            List<String> argsList = ListUtil.toList(args);
            String configFilePath = argsList.stream().filter(x -> StrUtil.startWith(x, "--conf")).findAny().orElse("");
            if (StrUtil.isNotEmpty(configFilePath)) {
                propList.add(StrUtil.format("supervisor.cli.config-file-path={}", StrUtil.split(configFilePath, "=").get(1)));
            }
            argsList = argsList.stream().filter(x -> !StrUtil.startWithAny(x, "--conf")).collect(Collectors.toList());
            args = argsList.toArray(new String[0]);
        }
        new SpringApplicationBuilder(Launcher.class)
                .web(WebApplicationType.NONE)
                .properties(propList.toArray(new String[0]))
                .run(args);
    }
}
