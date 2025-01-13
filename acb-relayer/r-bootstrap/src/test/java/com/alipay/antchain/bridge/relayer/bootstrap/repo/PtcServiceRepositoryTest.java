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

package com.alipay.antchain.bridge.relayer.bootstrap.repo;

import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.util.ArrayUtil;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.PtcServiceStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.PtcServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcTrustRootDO;
import com.alipay.antchain.bridge.relayer.commons.model.PtcVerifyAnchorDO;
import com.alipay.antchain.bridge.relayer.commons.model.TpBtaDO;
import com.alipay.antchain.bridge.relayer.dal.repository.IPtcServiceRepository;
import org.junit.Assert;
import org.junit.Test;

public class PtcServiceRepositoryTest extends TestBase {

    @Resource
    private IPtcServiceRepository ptcServiceRepository;

    @Test
    public void testPtcServiceData() {
        ptcServiceRepository.savePtcServiceData(PTC_SERVICE_DO);

        PtcServiceDO ptcServiceDO = ptcServiceRepository.getPtcServiceData(PTC_SERVICE_ID1);
        Assert.assertNotNull(ptcServiceDO);
        Assert.assertArrayEquals(NODE_PTC_CERT.encode(), ptcServiceDO.getPtcCert().encode());

        ptcServiceDO = ptcServiceRepository.getPtcServiceData(ptcOid);
        Assert.assertNotNull(ptcServiceDO);
        Assert.assertArrayEquals(NODE_PTC_CERT.encode(), ptcServiceDO.getPtcCert().encode());

        Assert.assertTrue(ptcServiceRepository.hasPtcServiceData(PTC_SERVICE_ID1));

        Assert.assertEquals(PTC_SERVICE_DO.getState(), ptcServiceRepository.queryPtcServiceState(PTC_SERVICE_ID1));

        ptcServiceRepository.updatePtcServiceState(PTC_SERVICE_ID1, PtcServiceStateEnum.FROZEN);
        Assert.assertEquals(PtcServiceStateEnum.FROZEN, ptcServiceRepository.queryPtcServiceState(PTC_SERVICE_ID1));

        ptcServiceRepository.removePtcServiceData(PTC_SERVICE_ID1);
        Assert.assertFalse(ptcServiceRepository.hasPtcServiceData(PTC_SERVICE_ID1));
    }

    @Test
    public void testTpBta() {
        TpBtaDO tpBtaDO = TpBtaDO.builder().tpbta(tpbtaWithChannelLevel).build();
        ptcServiceRepository.setTpBta(tpBtaDO);

        TpBtaDO res = ptcServiceRepository.getExactTpBta(tpbtaWithChannelLevel.getCrossChainLane(), tpbtaWithChannelLevel.getTpbtaVersion());
        Assert.assertNotNull(res);
        Assert.assertArrayEquals(tpbtaWithChannelLevel.encode(), res.getTpbta().encode());

        res = ptcServiceRepository.getExactTpBta(tpbtaWithChannelLevel.getCrossChainLane());
        Assert.assertNotNull(res);
        Assert.assertArrayEquals(tpbtaWithChannelLevel.encode(), res.getTpbta().encode());

        Assert.assertTrue(ptcServiceRepository.hasTpBta(tpbtaWithChannelLevel.getCrossChainLane(), tpbtaWithChannelLevel.getTpbtaVersion()));
    }

    @Test
    public void testGetMatchedTpBta() {
        ptcServiceRepository.setTpBta(TpBtaDO.builder().tpbta(tpbtaWithLaneLevel1).build());
        ptcServiceRepository.setTpBta(TpBtaDO.builder().tpbta(tpbtaWithLaneLevel2).build());

        TpBtaDO res = ptcServiceRepository.getMatchedTpBta(ucp.getCrossChainLane());
        Assert.assertNotNull(res);
        Assert.assertArrayEquals(tpbtaWithLaneLevel1.encode(), res.getTpbta().encode());

        List<TpBtaDO> tpBtaDOs = ptcServiceRepository.getAllValidTpBtaForDomain(ucp.getSrcDomain());
        Assert.assertEquals(2, tpBtaDOs.size());
        Assert.assertTrue(tpBtaDOs.stream().anyMatch(
                tpBtaDO -> ArrayUtil.equals(tpBtaDO.getTpbta().encode(), tpbtaWithLaneLevel1.encode())
        ));
        Assert.assertTrue(tpBtaDOs.stream().anyMatch(
                tpBtaDO -> ArrayUtil.equals(tpBtaDO.getTpbta().encode(), tpbtaWithLaneLevel2.encode())
        ));

        ptcServiceRepository.setTpBta(TpBtaDO.builder().tpbta(tpbtaWithChannelLevel).build());
        res = ptcServiceRepository.getMatchedTpBta(ucp.getCrossChainLane());
        Assert.assertNotNull(res);
        Assert.assertArrayEquals(tpbtaWithChannelLevel.encode(), res.getTpbta().encode());

        tpBtaDOs = ptcServiceRepository.getAllValidTpBtaForDomain(ucp.getSrcDomain());
        Assert.assertEquals(1, tpBtaDOs.size());
        Assert.assertArrayEquals(tpbtaWithChannelLevel.encode(), tpBtaDOs.get(0).getTpbta().encode());
    }

    @Test
    public void testPtcTrustRoot() {
        ptcServiceRepository.savePtcTrustRoot(PtcTrustRootDO.builder().ptcServiceId(PTC_SERVICE_ID1).ptcTrustRoot(ptcTrustRoot).build());

        PtcTrustRootDO ptcTrustRootDO = ptcServiceRepository.getPtcTrustRoot(PTC_SERVICE_ID1);
        Assert.assertNotNull(ptcTrustRootDO);
        Assert.assertArrayEquals(ptcTrustRoot.encode(), ptcTrustRootDO.getPtcTrustRoot().encode());

        ptcTrustRootDO = ptcServiceRepository.getPtcTrustRoot(NODE_PTC_CERT.getCredentialSubjectInstance().getApplicant());
        Assert.assertNotNull(ptcTrustRootDO);
        Assert.assertArrayEquals(ptcTrustRoot.encode(), ptcTrustRootDO.getPtcTrustRoot().encode());

        Assert.assertTrue(ptcServiceRepository.hasPtcTrustRoot(NODE_PTC_CERT.getCredentialSubjectInstance().getApplicant()));

        PtcVerifyAnchorDO ptcVerifyAnchorDO = ptcServiceRepository.getPtcVerifyAnchor(ptcOid, ptcVerifyAnchor.getVersion());
        Assert.assertEquals(PTC_SERVICE_ID1, ptcVerifyAnchorDO.getPtcServiceId());
        Assert.assertArrayEquals(ptcVerifyAnchor.encode(), ptcVerifyAnchorDO.getPtcVerifyAnchor().encode());
    }
}
