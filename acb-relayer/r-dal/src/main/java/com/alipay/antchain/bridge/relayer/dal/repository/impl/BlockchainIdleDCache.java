package com.alipay.antchain.bridge.relayer.dal.repository.impl;

import java.util.Map;
import javax.annotation.Resource;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 区块链空闲状态分布式缓存
 *
 * <pre>
 *     该状态用于优化，当区块链空闲没有请求流量时，os相关的任务处理可以减低执行频率，减低系统负担。
 *
 *     该状态记录几个值
 *
 *     1. lastAMReceiveTime：最后一笔am消息的接收时间
 *     2. lastAMProcessTime：最后一笔am消息的处理时间
 *     3. lastAMResponseTime：最后一笔am消息提交回执的接收时间
 *     4. lastOracleReceiveTime：最后一笔oracle请求的接收时间
 *
 *     1. lastEmptyAMPoolTime：最后一次am poll为空的时间
 *     2. lastEmptyAMSendQueueTime：最后一次am send queue为空的时间
 *     3. lastEmptyAMArchiveTime：am archive为空的时间
 *     4. lastEmptyOraclePoolTime：最后一次oracle poll为空的时间
 *     5. lastEmptyOracleCommitterTime：最后一次oracle poll committer为空的时间
 *
 *     由于并发任务有乱序问题，且避免加悲观锁，使用无锁方案，设定一个乐观的脏读区间，可以设置为3S
 *
 *     即，AM Process停止轮询pool表时，条件为 lastEmptyAMPoolTime - lastAMReceiveTime > 3S
 * </pre>
 */
@Component
@Slf4j
public class BlockchainIdleDCache {

    private static final String LAST_UCP_RECEIVE_TIME = "last_ucp_receive_time";

    private static final String LAST_UCP_PROCESS_TIME = "last_ucp_process_time";

    private static final String LAST_EMPTY_UCP_POOL_TIME = "last_empty_ucp_pool_time";

    private static final String LAST_AM_RECEIVE_TIME = "last_am_receive_time";

    private static final String LAST_AM_RESPONSE_TIME = "last_am_response_time";

    private static final String LAST_AM_PROCESS_TIME = "last_am_process_time";

    private static final String LAST_ORACLE_RECEIVE_TIME = "last_oracle_receive_time";

    private static final String LAST_EMPTY_AM_POOL_TIME = "last_empty_am_pool_time";

    private static final String LAST_EMPTY_AM_SEND_QUEUE_TIME = "last_empty_am_send_queue_time";

    private static final String LAST_EMPTY_AM_ARCHIVE_TIME = "last_empty_am_archive_time";

    private static final String LAST_EMPTY_ORACLE_POOL_TIME = "last_empty_oracle_pool_time";

    private static final String LAST_EMPTY_ORACLE_COMMITTER_TIME = "last_empty_oracle_committer_time";

    private static final int COUNT_LIMIT = 1 << 8;

    /**
     * 对每个（func，product，blockchainID）记一个计数器，当整除COUNT_LIMIT的时候
     * 重置并无视空闲时间，执行任务。
     * 这是为了防止出现数据库有请求但是relayer处于空闲的情况。
     */
    private final Map<String, Integer> counterMap = MapUtil.newConcurrentHashMap();

    @Value("${relayer.blockchain.idle.time_limit:10000}")
    private long idleTime;

    @Resource
    private RedissonClient redisson;

    public void setLastUCPReceiveTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_UCP_RECEIVE_TIME, System.currentTimeMillis());
    }

    public void setLastUCPProcessTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_UCP_PROCESS_TIME, System.currentTimeMillis());
    }

    public void setLastEmptyUCPPoolTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_EMPTY_UCP_POOL_TIME, System.currentTimeMillis());
    }

    public void setLastAMReceiveTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_AM_RECEIVE_TIME, System.currentTimeMillis());
    }

    public void setLastAMResponseTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_AM_RESPONSE_TIME, System.currentTimeMillis());
    }

    public void setLastAMProcessTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_AM_PROCESS_TIME, System.currentTimeMillis());
    }

    public void setLastOracleReceiveTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_ORACLE_RECEIVE_TIME, System.currentTimeMillis());
    }

    public void setLastEmptyAMPoolTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_EMPTY_AM_POOL_TIME, System.currentTimeMillis());
    }

    public void setLastEmptyAMSendQueueTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_EMPTY_AM_SEND_QUEUE_TIME, System.currentTimeMillis());
    }

    public void setLastEmptyAMArchiveTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_EMPTY_AM_ARCHIVE_TIME, System.currentTimeMillis());
    }

    public void setLastEmptyOraclePoolTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_EMPTY_ORACLE_POOL_TIME, System.currentTimeMillis());
    }

    public void setLastEmptyOracleCommitterTime(String product, String blockchainId) {
        setIdleState(product, blockchainId, LAST_EMPTY_ORACLE_COMMITTER_TIME, System.currentTimeMillis());
    }

    /**
     * 从counterMap获取对应的数值，检查是否整除COUNT_LIMIT，
     * 若整除，则返回false，代表本次不检查是否空闲，直接执行任务即可。
     *
     * @param funcName     函数名字
     * @param product      链的框架
     * @param blockchainId 链ID
     * @return 是否空闲
     */
    private boolean checkCounter(String funcName, String product, String blockchainId) {
        String key = String.format("%s-%s:%s", funcName, product, blockchainId);
        int cnt = counterMap.getOrDefault(key, 1) % COUNT_LIMIT;
        counterMap.put(key, cnt + 1);
        return cnt != 0;
    }

    public boolean ifUCPProcessIdle(String product, String blockchainId) {
        long lastUCPReceiveTime = getIdleState(product, blockchainId, LAST_UCP_RECEIVE_TIME);
        long lastEmptyUCPPoolTime = getIdleState(product, blockchainId, LAST_EMPTY_UCP_POOL_TIME);
        if (lastEmptyUCPPoolTime - lastUCPReceiveTime > idleTime) {
            return checkCounter("ifUCPProcessIdle", product, blockchainId);
        }
        return false;
    }

    public boolean ifAMProcessIdle(String product, String blockchainId) {

        long lastAMReceiveTime = getIdleState(product, blockchainId, LAST_AM_RECEIVE_TIME);
        long lastEmptyAMPoolTime = getIdleState(product, blockchainId, LAST_EMPTY_AM_POOL_TIME);
        if (lastEmptyAMPoolTime - lastAMReceiveTime > idleTime) {
            return checkCounter("ifAMProcessIdle", product, blockchainId);
        }
        return false;
    }

    public boolean ifAMCommitterIdle(String product, String blockchainId) {

        long lastAMProcessTime = getIdleState(product, blockchainId, LAST_AM_PROCESS_TIME);
        long lastEmptyAMSendQueueTime = getIdleState(product, blockchainId, LAST_EMPTY_AM_SEND_QUEUE_TIME);

        if (lastEmptyAMSendQueueTime - lastAMProcessTime > idleTime) {
            return checkCounter("ifAMCommitterIdle", product, blockchainId);
        }
        return false;
    }

    public boolean ifAMArchiveIdle(String product, String blockchainId) {

        long lastAMResponseTime = getIdleState(product, blockchainId, LAST_AM_RESPONSE_TIME);
        long lastEmptyAMArchiveTime = getIdleState(product, blockchainId, LAST_EMPTY_AM_ARCHIVE_TIME);

        if (lastEmptyAMArchiveTime - lastAMResponseTime > idleTime) {
            return checkCounter("ifAMArchiveIdle", product, blockchainId);
        }
        return false;
    }

    public boolean ifOracleProcessIdle(String product, String blockchainId) {

        long lastOracleReceiveTime = getIdleState(product, blockchainId, LAST_ORACLE_RECEIVE_TIME);
        long lastEmptyOraclePoolTime = getIdleState(product, blockchainId, LAST_EMPTY_ORACLE_POOL_TIME);

        if (lastEmptyOraclePoolTime - lastOracleReceiveTime > idleTime) {
            return checkCounter("ifOracleProcessIdle", product, blockchainId);
        }
        return false;
    }

    public boolean ifOracleCommitterIdle(String product, String blockchainId) {

        long lastOracleReceiveTime = getIdleState(product, blockchainId, LAST_ORACLE_RECEIVE_TIME);
        long lastEmptyOracleCommitterTime = getIdleState(product, blockchainId, LAST_EMPTY_ORACLE_COMMITTER_TIME);

        if (lastEmptyOracleCommitterTime - lastOracleReceiveTime > idleTime) {
            return checkCounter("ifOracleCommitterIdle", product, blockchainId);
        }
        return false;
    }

    private long getIdleState(String product, String blockchainId, String type) {

        RBucket<String> bucket = redisson.getBucket(genKey(product, blockchainId, type), StringCodec.INSTANCE);
        String rawTime = bucket.get();
        if (StrUtil.isEmpty(rawTime)) {
            return 0;
        }
        return Long.parseLong(rawTime);
    }

    private void setIdleState(String product, String blockchainId, String type, long time) {
        log.debug("set idle state : {}-{}-{}-{} ", product, blockchainId, type, time);
        redisson.getBucket(genKey(product, blockchainId, type), StringCodec.INSTANCE)
                .set(Long.valueOf(time).toString());
    }

    public static String genKey(String product, String blockchainId, String type) {
        return product + "^" + blockchainId + "^" + type;
    }

}
