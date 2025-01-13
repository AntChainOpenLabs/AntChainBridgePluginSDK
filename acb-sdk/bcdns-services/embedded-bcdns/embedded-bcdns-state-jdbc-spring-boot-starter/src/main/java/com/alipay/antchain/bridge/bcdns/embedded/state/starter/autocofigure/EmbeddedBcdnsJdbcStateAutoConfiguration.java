/*
 * Copyright 2024 Ant Group
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

package com.alipay.antchain.bridge.bcdns.embedded.state.starter.autocofigure;

import com.alipay.antchain.bridge.bcdns.embedded.server.IBcdnsState;
import com.alipay.antchain.bridge.bcdns.embedded.state.starter.config.EmbeddedBcdnsJdbcStateProperties;
import com.alipay.antchain.bridge.bcdns.embedded.state.starter.mbp.MyBatisPlusBcdnsState;
import com.alipay.antchain.bridge.bcdns.embedded.state.starter.mbp.mapper.*;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmbeddedBcdnsJdbcStateProperties.class)
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@MapperScan(basePackages = "com.alipay.antchain.bridge.bcdns.embedded.state.starter.mbp.mapper")
@ConditionalOnMissingBean(name = {"bcdnsState"})
@ConditionalOnProperty(prefix = "acb.bcdns.embedded", name = "server-on", havingValue = "true")
@Slf4j
public class EmbeddedBcdnsJdbcStateAutoConfiguration {

    @ConditionalOnProperty(prefix = "acb.bcdns.embedded", name = "state-type", havingValue = EmbeddedBcdnsJdbcStateProperties.STATE_TYPE_JDBC, matchIfMissing = true)
    @Bean
    IBcdnsState bcdnsState(
            @Autowired EmbeddedBcdnsDomainCertMapper embeddedBcdnsDomainCertMapper,
            @Autowired EmbeddedBcdnsDomainSpaceCertMapper embeddedBcdnsDomainSpaceCertMapper,
            @Autowired EmbeddedBcdnsCertApplicationMapper embeddedBcdnsCertApplicationMapper,
            @Autowired EmbeddedBcdnsRelayerCertMapper embeddedBcdnsRelayerCertMapper,
            @Autowired EmbeddedBcdnsPtcCertMapper embeddedBcdnsPtcCertMapper,
            @Autowired EmbeddedBcdnsDomainRouterMapper embeddedBcdnsDomainRouterMapper,
            @Autowired EmbeddedBcdnsTpBtaMapper embeddedBcdnsTpBtaMapper,
            @Autowired EmbeddedBcdnsPtcTrustRootMapper embeddedBcdnsPtcTrustRootMapper,
            @Autowired EmbeddedBcdnsPtcVerifyAnchorMapper embeddedBcdnsPtcVerifyAnchorMapper
    ) {
        log.info("start jdbc bcdns state");
        return new MyBatisPlusBcdnsState(
                embeddedBcdnsCertApplicationMapper,
                embeddedBcdnsDomainCertMapper,
                embeddedBcdnsDomainSpaceCertMapper,
                embeddedBcdnsRelayerCertMapper,
                embeddedBcdnsPtcCertMapper,
                embeddedBcdnsDomainRouterMapper,
                embeddedBcdnsTpBtaMapper,
                embeddedBcdnsPtcTrustRootMapper,
                embeddedBcdnsPtcVerifyAnchorMapper
        );
    }
}
