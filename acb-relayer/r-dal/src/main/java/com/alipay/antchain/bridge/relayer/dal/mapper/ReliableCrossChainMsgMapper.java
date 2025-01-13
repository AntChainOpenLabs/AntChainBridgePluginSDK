package com.alipay.antchain.bridge.relayer.dal.mapper;

import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMessage;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.dal.entities.ReliableCrossChainMsgEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReliableCrossChainMsgMapper extends BaseMapper<ReliableCrossChainMsgEntity> {
    void saveRCCMessages(List<ReliableCrossChainMessage> rccMsgs);

    int deleteExpiredMessages(int validPeriod);

}
