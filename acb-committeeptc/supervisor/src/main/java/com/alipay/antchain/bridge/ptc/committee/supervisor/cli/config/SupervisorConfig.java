package com.alipay.antchain.bridge.ptc.committee.supervisor.cli.config;

import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.bcdns.factory.BlockChainDomainNameServiceFactory;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
public class SupervisorConfig {

    @Value("${supervisor.cli.config-file-path}")
    private Resource configFile;

    @Bean
    public Map<String, IBlockChainDomainNameService> domainSpaceToBcdnsMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    @SneakyThrows
    public SupervisorCLIConfig supervisorCLIConfig(@Autowired Map<String, IBlockChainDomainNameService> domainSpaceToBcdnsMap) {
        SupervisorCLIConfig config = JSON.parseObject(configFile.getContentAsByteArray(), SupervisorCLIConfig.class);
        config.getBcdnsConfig().forEach(
                (s, bcdnsConfig) -> {
                    try {
                        log.info("init bcdns client for ( domain_space: {}, type: {}, config_path: {} )", s, bcdnsConfig.getBcdnsType(), bcdnsConfig.getBcdnsPath().toString());
                        domainSpaceToBcdnsMap.put(
                                s,
                                BlockChainDomainNameServiceFactory.create(bcdnsConfig.getBcdnsType(), Files.readAllBytes(bcdnsConfig.getBcdnsPath()))
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return config;
    }
}
