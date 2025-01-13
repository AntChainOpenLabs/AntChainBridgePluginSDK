package com.alipay.antchain.bridge.relayer.engine.executor;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.bcdns.types.resp.ApplicationResult;
import com.alipay.antchain.bridge.relayer.commons.model.BizDistributedTask;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertApplicationDO;
import com.alipay.antchain.bridge.relayer.commons.model.IDistributedTask;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.engine.checker.IDistributedTaskChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
public class QueryDomainCertApplicationScheduleTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private IBCDNSManager bcdnsManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Autowired
    public QueryDomainCertApplicationScheduleTaskExecutor(
            @Qualifier("baseScheduleBizTaskExecutorThreadsPool") ExecutorService executorService,
            @Qualifier("localDistributedTaskChecker") IDistributedTaskChecker distributedTaskChecker
    ) {
        super(executorService, distributedTaskChecker);
    }

    @Override
    public Runnable genTask(IDistributedTask task) {
        return () -> {
            if (task instanceof BizDistributedTask) {
                try {
                    List<DomainCertApplicationDO> domainCertApplicationDOS = bcdnsManager.getAllApplyingDomainCertApplications();
                    if (ObjectUtil.isEmpty(domainCertApplicationDOS)) {
                        return;
                    }

                    domainCertApplicationDOS.forEach(
                            domainCertApplicationDO -> {
                                try {
                                    transactionTemplate.execute(
                                            new TransactionCallbackWithoutResult() {
                                                @Override
                                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                                    log.debug("querying result of domain cert application (domain: {}, receipt: {}) from BCDNS with domain space [{}]",
                                                            domainCertApplicationDO.getDomain(), domainCertApplicationDO.getApplyReceipt(), domainCertApplicationDO.getDomainSpace());
                                                    ApplicationResult result = bcdnsManager.getBCDNSService(domainCertApplicationDO.getDomainSpace())
                                                            .queryDomainNameCertificateApplicationResult(domainCertApplicationDO.getApplyReceipt());
                                                    if (!result.isFinalResult()) {
                                                        return;
                                                    }
                                                    log.info(
                                                            "result of domain cert application (domain: {}, receipt: {}) from BCDNS with domain space [{}] is success: {}",
                                                            domainCertApplicationDO.getDomain(),
                                                            domainCertApplicationDO.getApplyReceipt(),
                                                            domainCertApplicationDO.getDomainSpace(),
                                                            ObjectUtil.isNull(result.getCertificate())
                                                    );
                                                    bcdnsManager.saveDomainCertApplicationResult(
                                                            domainCertApplicationDO.getDomain(),
                                                            result.getCertificate()
                                                    );
                                                }
                                            }
                                    );
                                } catch (Exception e) {
                                    log.error(
                                            "failed to process domain cert application for domain {} with space {}",
                                            domainCertApplicationDO.getDomain(),
                                            domainCertApplicationDO.getDomainSpace(),
                                            e
                                    );
                                }
                            }
                    );
                } catch (Throwable e) {
                    log.error("QueryDomainCertApplicationScheduleTaskExecutor failed", e);
                }
            }
        };
    }
}
