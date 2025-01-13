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

package com.alipay.antchain.bridge.relayer.cli;

import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackages = {"com.alipay.antchain.bridge.relayer.cli"})
public class Launcher {
    public static void main(String[] args) {
        List<String> propList = ListUtil.toList("logging.level.root=ERROR");
        if (ObjectUtil.isNotEmpty(args)) {
            List<String> argsList = ListUtil.toList(args);
            String port = argsList.stream().filter(x -> StrUtil.startWith(x, "--port")).findAny().orElse("");
            if (StrUtil.isNotEmpty(port)) {
                propList.add(StrUtil.format("port={}", StrUtil.split(port, "=").get(1)));
            }
            String host = argsList.stream().filter(x -> StrUtil.startWith(x, "--host")).findAny().orElse("");
            if (StrUtil.isNotEmpty(host)) {
                propList.add(StrUtil.format("host={}", StrUtil.split(host, "=").get(1)));
            }
            argsList = argsList.stream().filter(x -> !StrUtil.startWithAny(x, "--port", "--host")).collect(Collectors.toList());
            args = argsList.toArray(new String[0]);
        }
        new SpringApplicationBuilder(Launcher.class)
                .web(WebApplicationType.NONE)
                .properties(propList.toArray(new String[0]))
                .run(args);
    }
}
