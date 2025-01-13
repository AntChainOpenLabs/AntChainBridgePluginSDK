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

import java.util.concurrent.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadsConfig {

    @Value("${relayer.network.node.server.threads.core_size:4}")
    private int wsRelayerServerCoreSize;

    @Value("${relayer.network.node.server.threads.total_size:8}")
    private int wsRelayerServerTotalSize;

    @Value("${relayer.network.node.client.threads.core_size:4}")
    private int wsRelayerClientCoreSize;

    @Value("${relayer.network.node.client.threads.total_size:8}")
    private int wsRelayerClientTotalSize;

    @Value("${relayer.service.validation.threads.core_size:4}")
    private int validationServiceCoreSize;

    @Value("${relayer.service.validation.threads.total_size:8}")
    private int validationServiceTotalSize;

    @Value("${relayer.service.process.threads.core_size:4}")
    private int processServiceCoreSize;

    @Value("${relayer.service.process.threads.total_size:8}")
    private int processServiceTotalSize;

    @Value("${relayer.service.committer.threads.core_size:8}")
    private int committerServiceCoreSize;

    @Value("${relayer.service.committer.threads.total_size:8}")
    private int committerServiceTotalSize;

    @Value("${relayer.service.anchor.sync_task.threads.core_size:4}")
    private int blockSyncTaskCoreSize;

    @Value("${relayer.service.anchor.sync_task.threads.total_size:8}")
    private int blockSyncTaskTotalSize;

    @Value("${relayer.service.confirm.threads.core_size:4}")
    private int confirmServiceCoreSize;

    @Value("${relayer.service.confirm.threads.total_size:4}")
    private int confirmServiceTotalSize;

    @Value("${relayer.engine.duty.anchor.threads.core_size:4}")
    private int anchorScheduleTaskExecutorCoreSize;

    @Value("${relayer.engine.duty.anchor.threads.total_size:8}")
    private int anchorScheduleTaskExecutorTotalSize;

    @Value("${relayer.engine.duty.committer.threads.core_size:4}")
    private int committerScheduleTaskExecutorCoreSize;

    @Value("${relayer.engine.duty.committer.threads.total_size:8}")
    private int committerScheduleTaskExecutorTotalSize;

    @Value("${relayer.engine.duty.process.threads.core_size:4}")
    private int processScheduleTaskExecutorCoreSize;

    @Value("${relayer.engine.duty.process.threads.total_size:8}")
    private int processScheduleTaskExecutorTotalSize;

    @Value("${relayer.engine.duty.validation.threads.core_size:4}")
    private int validationScheduleTaskExecutorCoreSize;

    @Value("${relayer.engine.duty.validation.threads.total_size:8}")
    private int validationScheduleTaskExecutorTotalSize;

    @Value("${relayer.engine.duty.confirm.threads.core_size:4}")
    private int confirmScheduleTaskExecutorCoreSize;

    @Value("${relayer.engine.duty.confirm.threads.total_size:4}")
    private int confirmScheduleTaskExecutorTotalSize;

    @Value("${relayer.engine.duty.archive.threads.core_size:4}")
    private int archiveScheduleTaskExecutorCoreSize;

    @Value("${relayer.engine.duty.archive.threads.total_size:4}")
    private int archiveScheduleTaskExecutorTotalSize;

    @Value("${relayer.engine.duty.deploy.threads.core_size:4}")
    private int deployScheduleTaskExecutorCoreSize;

    @Value("${relayer.engine.duty.deploy.threads.total_size:8}")
    private int deployScheduleTaskExecutorTotalSize;

    @Value("${relayer.engine.duty.biz_base.threads.core_size:1}")
    private int baseScheduleBizTaskExecutorCoreSize;

    @Value("${relayer.engine.duty.biz_base.threads.total_size:4}")
    private int baseScheduleBizTaskExecutorTotalSize;

    @Value("${relayer.service.domain_router.threads.core_size:1}")
    private int domainRouterQueryCoreSize;

    @Value("${relayer.service.domain_router.threads.total_size:4}")
    private int domainRouterQueryTotalSize;

    @Value("${relayer.service.reliable_process.threads.core_size:1}")
    private int reliableProcessTaskExecutorCoreSize;

    @Value("${relayer.service.reliable_process.threads.total_size:4}")
    private int reliableProcessTaskExecutorTotalSize;

    @Bean(name = "wsRelayerServerExecutorService")
    public ExecutorService wsRelayerServerExecutorService() {
        return new ThreadPoolExecutor(
                wsRelayerServerCoreSize,
                wsRelayerServerTotalSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("ws-relayer-server-worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(name = "wsRelayerClientThreadsPool")
    public ExecutorService wsRelayerClientThreadsPool() {
        return new ThreadPoolExecutor(
                wsRelayerClientCoreSize,
                wsRelayerClientTotalSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("ws-relayer-client-worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(name = "validationServiceThreadsPool")
    public ExecutorService validationServiceThreadsPool() {
        return new ThreadPoolExecutor(
                validationServiceCoreSize,
                validationServiceTotalSize,
                1000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("validation-service-%d").build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(name = "processServiceThreadsPool")
    public ExecutorService processServiceThreadsPool() {
        return new ThreadPoolExecutor(
                processServiceCoreSize,
                processServiceTotalSize,
                1000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("Process-worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(name = "committerServiceThreadsPool")
    public ExecutorService committerServiceThreadsPool() {
        return new ThreadPoolExecutor(
                committerServiceCoreSize,
                committerServiceTotalSize,
                5000L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("Committer-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "blockSyncTaskThreadsPool")
    public ExecutorService blockSyncTaskThreadsPool() {
        return new ThreadPoolExecutor(
                blockSyncTaskCoreSize,
                blockSyncTaskTotalSize,
                5000L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1280),
                new ThreadFactoryBuilder().setNameFormat("BlockSyncTask-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "confirmServiceThreadsPool")
    public ExecutorService confirmServiceThreadsPool() {
        return new ThreadPoolExecutor(
                confirmServiceCoreSize,
                confirmServiceTotalSize,
                5000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadFactoryBuilder().setNameFormat("AMConfirm-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "distributedTaskEngineScheduleThreadsPool")
    public ScheduledExecutorService distributedTaskEngineScheduleThreadsPool() {
        return new ScheduledThreadPoolExecutor(
                6,
                new ThreadFactoryBuilder().setNameFormat("ScheduleEngine-Executor-%d").build()
        );
    }

    @Bean(name = "markTaskProcessEngineScheduleThreadsPool")
    public ScheduledExecutorService markTaskProcessEngineScheduleThreadsPool() {
        return new ScheduledThreadPoolExecutor(
                1,
                new ThreadFactoryBuilder().setNameFormat("ScheduleEngine-Executor-%d").build()
        );
    }

    @Bean(name = "anchorScheduleTaskExecutorThreadsPool")
    public ExecutorService anchorScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                anchorScheduleTaskExecutorCoreSize,
                anchorScheduleTaskExecutorTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("anchor_executor-worker-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    @Bean(name = "committerScheduleTaskExecutorThreadsPool")
    public ExecutorService committerScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                committerScheduleTaskExecutorCoreSize,
                committerScheduleTaskExecutorTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("committer_executor-worker-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    @Bean(name = "processScheduleTaskExecutorThreadsPool")
    public ExecutorService processScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                processScheduleTaskExecutorCoreSize,
                processScheduleTaskExecutorTotalSize,
                5000, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("process_executor-worker-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    @Bean(name = "validationScheduleTaskExecutorThreadsPool")
    public ExecutorService validationScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                validationScheduleTaskExecutorCoreSize,
                validationScheduleTaskExecutorTotalSize,
                5000, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("validation_executor-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "confirmScheduleTaskExecutorThreadsPool")
    public ExecutorService confirmScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                confirmScheduleTaskExecutorCoreSize,
                confirmScheduleTaskExecutorTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("confirm_executor-worker-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    @Bean(name = "archiveScheduleTaskExecutorThreadsPool")
    public ExecutorService archiveScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                archiveScheduleTaskExecutorCoreSize,
                archiveScheduleTaskExecutorTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("archive_executor-worker-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    @Bean(name = "deployScheduleTaskExecutorThreadsPool")
    public ExecutorService deployScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                deployScheduleTaskExecutorCoreSize,
                deployScheduleTaskExecutorTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactoryBuilder().setNameFormat("deploy_executor-worker-%d").build(),
                new ThreadPoolExecutor.DiscardPolicy()
        );
    }

    @Bean(name = "baseScheduleBizTaskExecutorThreadsPool")
    public ExecutorService baseScheduleBizTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                baseScheduleBizTaskExecutorCoreSize,
                baseScheduleBizTaskExecutorTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadFactoryBuilder().setNameFormat("base_executor-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "domainRouterScheduleTaskExecutorThreadsPool")
    public ExecutorService domainRouterScheduleTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                domainRouterQueryCoreSize,
                domainRouterQueryTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadFactoryBuilder().setNameFormat("domain_router_executor-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 可靠上链消息重试处理线程池
     * @return
     */
    @Bean(name = "reliableProcessTaskExecutorThreadsPool")
    public ExecutorService reliableProcessTaskExecutorThreadsPool() {
        return new ThreadPoolExecutor(
                reliableProcessTaskExecutorCoreSize,
                reliableProcessTaskExecutorTotalSize,
                0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10),
                new ThreadFactoryBuilder().setNameFormat("reliableProcess_executor-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
