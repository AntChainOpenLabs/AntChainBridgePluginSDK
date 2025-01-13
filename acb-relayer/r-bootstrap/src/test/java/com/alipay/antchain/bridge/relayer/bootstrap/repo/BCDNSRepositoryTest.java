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

package com.alipay.antchain.bridge.relayer.bootstrap.repo;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.BCDNSStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainCertApplicationStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BCDNSServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertApplicationDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainSpaceCertWrapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBCDNSRepository;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BCDNSRepositoryTest extends TestBase {

    public static DomainCertApplicationDO antchainDotComApplication = new DomainCertApplicationDO(
            antChainDotComDomain,
            dotComDomainSpace,
            "0x0001",
            DomainCertApplicationStateEnum.APPLYING
    );

    @BeforeClass
    public static void setup() {
    }

    @Resource
    private IBCDNSRepository bcdnsRepository;

    @Test
    public void testSaveBCDNSServiceDO() {
        saveRoots();

        Assert.assertNotNull(bcdnsRepository.getDomainSpaceCert(CrossChainDomain.ROOT_DOMAIN_SPACE));
        Assert.assertNotNull(
                bcdnsRepository.getDomainSpaceCert(
                        dotComDomainSpace)
        );
    }

    @Test
    public void testHasBCDNSService() {
        saveRoots();

        Assert.assertTrue(bcdnsRepository.hasBCDNSService(dotComDomainSpace));
        Assert.assertFalse(bcdnsRepository.hasBCDNSService(".org"));
    }

    @Test
    public void testUpdateBCDNSServiceState() {
        saveRoots();
        BCDNSServiceDO bcdnsServiceDO = bcdnsRepository.getBCDNSServiceDO(dotComDomainSpace);
        Assert.assertEquals(BCDNSStateEnum.WORKING, bcdnsServiceDO.getState());
        bcdnsRepository.updateBCDNSServiceState(
                dotComDomainSpace,
                BCDNSStateEnum.FROZEN
        );
        Assert.assertEquals(BCDNSStateEnum.FROZEN, bcdnsRepository.getBCDNSServiceDO(dotComDomainSpace).getState());
    }

    @Test
    public void testGetDomainSpaceChain() {
        saveRoots();

        List<String> res = bcdnsRepository.getDomainSpaceChain(
                dotComDomainSpace
        );
        Assert.assertEquals(2, res.size());
        Assert.assertEquals(CrossChainDomain.ROOT_DOMAIN_SPACE, res.get(1));
    }

    @Test
    public void testGetDomainSpaceCertChain() {
        saveRoots();

        Map<String, DomainSpaceCertWrapper> res = bcdnsRepository.getDomainSpaceCertChain(dotComDomainSpace);
        Assert.assertNotNull(res);
        Assert.assertEquals(2, res.size());
        Assert.assertNotNull(dotComDomainSpace, res.get(dotComDomainSpace));
        Assert.assertEquals(CrossChainDomain.ROOT_DOMAIN_SPACE, res.get(dotComDomainSpace).getParentDomainSpace());
        Assert.assertEquals(
                HexUtil.encodeHexStr(dotComDomainSpaceCert.getCredentialSubjectInstance().getApplicant().encode()),
                HexUtil.encodeHexStr(res.get(dotComDomainSpace).getOwnerOid().encode())
        );
    }

    @Test
    public void testSaveDomainSpaceCert() {
        bcdnsRepository.saveDomainSpaceCert(
                new DomainSpaceCertWrapper(dotComDomainSpaceCert)
        );
        Assert.assertTrue(bcdnsRepository.hasDomainSpaceCert(dotComDomainSpace));
        DomainSpaceCertWrapper domainSpaceCertWrapper = bcdnsRepository.getDomainSpaceCert(dotComDomainSpace);
        Assert.assertNotNull(domainSpaceCertWrapper);
        Assert.assertEquals(dotComDomainSpace, domainSpaceCertWrapper.getDomainSpace());
        Assert.assertEquals(CrossChainDomain.ROOT_DOMAIN_SPACE, domainSpaceCertWrapper.getParentDomainSpace());
        Assert.assertEquals(dotComDomainSpaceCert.encodeToBase64(), domainSpaceCertWrapper.getDomainSpaceCert().encodeToBase64());
    }

    public void testSaveDomainCertApplicationEntry() {
        bcdnsRepository.saveDomainCertApplicationEntry(antchainDotComApplication);
        Assert.assertTrue(bcdnsRepository.hasDomainCertApplicationEntry(antChainDotComDomain));
        DomainCertApplicationDO applicationDO = bcdnsRepository.getDomainCertApplicationEntry(antChainDotComDomain);
        Assert.assertNotNull(applicationDO);
        Assert.assertEquals(DomainCertApplicationStateEnum.APPLYING, applicationDO.getState());
        Assert.assertEquals(antChainDotComDomain, applicationDO.getDomain());
    }

    public void testUpdateDomainCertApplicationState() {
        bcdnsRepository.saveDomainCertApplicationEntry(antchainDotComApplication);
        List<DomainCertApplicationDO> applicationDOS = bcdnsRepository.getDomainCertApplicationsByState(DomainCertApplicationStateEnum.APPLYING);
        Assert.assertEquals(1, applicationDOS.size());

        bcdnsRepository.updateDomainCertApplicationState(antChainDotComDomain, DomainCertApplicationStateEnum.APPLY_SUCCESS);
        applicationDOS = bcdnsRepository.getDomainCertApplicationsByState(DomainCertApplicationStateEnum.APPLY_SUCCESS);
        Assert.assertEquals(1, applicationDOS.size());
    }

    private void saveRoots() {
        bcdnsRepository.saveBCDNSServiceDO(rootBcdnsServiceDO);
        bcdnsRepository.saveBCDNSServiceDO(dotComBcdnsServiceDO);
    }
}
