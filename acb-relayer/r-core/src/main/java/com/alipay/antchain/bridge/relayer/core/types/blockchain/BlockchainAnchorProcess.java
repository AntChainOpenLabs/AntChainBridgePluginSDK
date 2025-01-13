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

package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.relayer.commons.model.AnchorProcessHeights;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockchainAnchorProcess {

    private static final String ANCHOR_PROCESS_LATEST_HEIGHT = "polling";

    private static final String ANCHOR_PROCESS_SPV_HEIGHT = "notify_CONTRACT_SYSTEM";

    private static final String ANCHOR_PROCESS_DATA_REQ_HEIGHT = "notify_CONTRACT_ORACLE";

    private static final String ANCHOR_PROCESS_MSG_HEIGHT = "notify_CONTRACT_AM_CLIENT";

    public static BlockchainAnchorProcess convertFrom(AnchorProcessHeights heights) {
        BlockchainAnchorProcess process = new BlockchainAnchorProcess();

        for (Map.Entry<String, Long> entry : heights.getProcessHeights().entrySet()) {
            TaskBlockHeight taskBlockHeight = new TaskBlockHeight(
                    entry.getValue(),
                    DateUtil.format(
                            new Date(heights.getModifiedTimeMap().get(entry.getKey())),
                            DatePattern.NORM_DATETIME_PATTERN
                    )
            );
            if (entry.getKey().equals(ANCHOR_PROCESS_LATEST_HEIGHT)) {
                process.setLatestBlockHeight(taskBlockHeight);
            } else if (entry.getKey().startsWith(ANCHOR_PROCESS_SPV_HEIGHT)) {
                process.addSpvTaskBlockHeight(
                        ConvertUtil.getTpBtaLaneFromHeightKey(entry.getKey()), taskBlockHeight
                );
            } else if (entry.getKey().equals(ANCHOR_PROCESS_DATA_REQ_HEIGHT)) {
                process.setDataReqTaskBlockHeight(taskBlockHeight);
            } else if (entry.getKey().equals(ANCHOR_PROCESS_MSG_HEIGHT)) {
                CrossChainLane tpbtaLane = ConvertUtil.getTpBtaLaneFromHeightKey(entry.getKey());
                process.addCrossChainTaskBlockHeight(
                        ObjectUtil.isNull(tpbtaLane) ? entry.getKey() : tpbtaLane.getLaneKey(), taskBlockHeight
                );
            }
        }
        return process;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TaskBlockHeight {
        private long height;
        private String gmtModified;
    }

    private TaskBlockHeight latestBlockHeight;

    private Map<String, TaskBlockHeight> spvTaskBlockHeights;

    private TaskBlockHeight dataReqTaskBlockHeight;

    private Map<String, TaskBlockHeight> crosschainTaskBlockHeights;

    public void addSpvTaskBlockHeight(CrossChainLane tpbtaLane, TaskBlockHeight taskBlockHeight) {
        if (spvTaskBlockHeights == null) {
            spvTaskBlockHeights = new HashMap<>();
        }
        spvTaskBlockHeights.put(tpbtaLane.getLaneKey(), taskBlockHeight);
    }

    public void addCrossChainTaskBlockHeight(String key, TaskBlockHeight taskBlockHeight) {
        if (crosschainTaskBlockHeights == null) {
            crosschainTaskBlockHeights = new HashMap<>();
        }
        crosschainTaskBlockHeights.put(key, taskBlockHeight);
    }
}
