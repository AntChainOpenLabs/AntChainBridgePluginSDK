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

package com.alipay.antchain.bridge.relayer.bootstrap.config;

import java.util.Map;

import cn.hutool.core.map.MapUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.BizDistributedTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainDistributedTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.engine.core.ScheduleContext;
import com.alipay.antchain.bridge.relayer.engine.executor.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class EngineConfig {

    @Value("${relayer.engine.node_id_mode:IP}")
    private String nodeIdMode;

    @Bean
    public ScheduleContext scheduleContext() {
        return new ScheduleContext(nodeIdMode);
    }

    @Bean
    @Autowired
    public Map<BlockchainDistributedTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap(
            AnchorScheduleTaskExecutor anchorScheduleTaskExecutor,
            CommitterScheduleTaskExecutor committerScheduleTaskExecutor,
            ValidationScheduleTaskExecutor validationScheduleTaskExecutor,
            ProcessScheduleTaskExecutor processScheduleTaskExecutor,
            TxConfirmScheduleTaskExecutor txConfirmScheduleTaskExecutor,
            ReliableProcessTaskExecutor reliableProcessTaskExecutor,
            ArchiveScheduleTaskExecutor archiveScheduleTaskExecutor,
            AsyncDeployScheduleTaskExecutor asyncDeployScheduleTaskExecutor
    ) {
        Map<BlockchainDistributedTaskTypeEnum, BaseScheduleTaskExecutor> res = MapUtil.newHashMap();
        res.put(BlockchainDistributedTaskTypeEnum.ANCHOR_TASK, anchorScheduleTaskExecutor);
        res.put(BlockchainDistributedTaskTypeEnum.COMMIT_TASK, committerScheduleTaskExecutor);
        res.put(BlockchainDistributedTaskTypeEnum.VALIDATION_TASK, validationScheduleTaskExecutor);
        res.put(BlockchainDistributedTaskTypeEnum.PROCESS_TASK, processScheduleTaskExecutor);
        res.put(BlockchainDistributedTaskTypeEnum.AM_CONFIRM_TASK, txConfirmScheduleTaskExecutor);
        res.put(BlockchainDistributedTaskTypeEnum.RELIABLE_RELAY_TASK, reliableProcessTaskExecutor);
        res.put(BlockchainDistributedTaskTypeEnum.ARCHIVE_TASK, archiveScheduleTaskExecutor);
        res.put(BlockchainDistributedTaskTypeEnum.DEPLOY_SERVICE_TASK, asyncDeployScheduleTaskExecutor);
        return res;
    }

    @Bean
    @Autowired
    public Map<BizDistributedTaskTypeEnum, BaseScheduleTaskExecutor> scheduleBizTaskExecutorMap(
            QueryDomainCertApplicationScheduleTaskExecutor queryDomainCertApplicationScheduleTaskExecutor
    ) {
        Map<BizDistributedTaskTypeEnum, BaseScheduleTaskExecutor> res = MapUtil.newHashMap();
        res.put(BizDistributedTaskTypeEnum.DOMAIN_APPLICATION_QUERY, queryDomainCertApplicationScheduleTaskExecutor);
        return res;
    }
}
