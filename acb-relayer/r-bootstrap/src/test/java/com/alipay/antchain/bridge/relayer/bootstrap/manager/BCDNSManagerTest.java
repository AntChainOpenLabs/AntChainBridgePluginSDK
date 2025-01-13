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

package com.alipay.antchain.bridge.relayer.bootstrap.manager;

import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import com.alipay.antchain.bridge.bcdns.factory.BlockChainDomainNameServiceFactory;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.bcdns.types.base.Relayer;
import com.alipay.antchain.bridge.bcdns.types.resp.ApplyDomainNameCertificateResponse;
import com.alipay.antchain.bridge.bcdns.types.resp.QueryBCDNSTrustRootCertificateResponse;
import com.alipay.antchain.bridge.bcdns.types.resp.QueryDomainNameCertificateResponse;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.core.base.BIDInfoObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.BCDNSStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.DomainCertApplicationStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.model.BCDNSServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertApplicationDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class BCDNSManagerTest extends TestBase {

    private static final String APPLICATION_RECEIPT = "0102";

    @Resource
    private IBCDNSManager bcdnsManager;

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Mock
    private IBlockChainDomainNameService bcdnsService;

    MockedStatic<BlockChainDomainNameServiceFactory> mockedStatic = Mockito.mockStatic(BlockChainDomainNameServiceFactory.class);

    @Test
    public void testRegisterBCDNSServiceWithLocalRootCert() {
        bcdnsManager.registerBCDNSService(
                CrossChainDomain.ROOT_DOMAIN_SPACE,
                BCDNSTypeEnum.BIF,
                "src/test/resources/bcdns/root_bcdns.json",
                "src/test/resources/cc_certs/trust_root.crt"
        );
        Assert.assertNotNull(bcdnsManager.getBCDNSService(CrossChainDomain.ROOT_DOMAIN_SPACE));
        BCDNSServiceDO bcdnsServiceDO = bcdnsManager.getBCDNSServiceData(CrossChainDomain.ROOT_DOMAIN_SPACE);
        Assert.assertNotNull(bcdnsServiceDO);
        Assert.assertEquals(CrossChainDomain.ROOT_DOMAIN_SPACE, bcdnsServiceDO.getDomainSpace());
        Assert.assertEquals(BCDNSStateEnum.WORKING, bcdnsServiceDO.getState());
    }

    @Test
    public void testRegisterBCDNSServiceQueryRemoteRootCert() {
        initRootBCDNS();
        BCDNSServiceDO bcdnsServiceDO = bcdnsManager.getBCDNSServiceData(CrossChainDomain.ROOT_DOMAIN_SPACE);
        Assert.assertNotNull(bcdnsServiceDO);
        Assert.assertEquals(CrossChainDomain.ROOT_DOMAIN_SPACE, bcdnsServiceDO.getDomainSpace());
        Assert.assertEquals(BCDNSStateEnum.WORKING, bcdnsServiceDO.getState());
    }

    @Test
    public void testStopBCDNSService() {
        initRootBCDNS();
        bcdnsManager.stopBCDNSService(CrossChainDomain.ROOT_DOMAIN_SPACE);
        Assert.assertEquals(BCDNSStateEnum.FROZEN, bcdnsManager.getBCDNSServiceData(CrossChainDomain.ROOT_DOMAIN_SPACE).getState());
        Assert.assertThrows(
                AntChainBridgeRelayerException.class,
                () -> bcdnsManager.getBCDNSService(CrossChainDomain.ROOT_DOMAIN_SPACE)
        );
    }

    @Test
    public void testGetAllBCDNSDomainSpace() {
        initRootBCDNS();
        List<String> res = bcdnsManager.getAllBCDNSDomainSpace();
        Assert.assertEquals(2, res.size());
    }

    @Test
    public void testValidateCrossChainCertificate() {
        initRootBCDNS();
        Assert.assertTrue(bcdnsManager.validateCrossChainCertificate(relayerCert));
        Assert.assertFalse(bcdnsManager.validateCrossChainCertificate(relayerCertWrongIssuer));
    }

    @Test
    public void testValidateCrossChainCertificateWithPath() {
        initRootBCDNS();
        Assert.assertTrue(
                bcdnsManager.validateCrossChainCertificate(
                        relayerCert,
                        MapUtil.builder(dotComDomainSpace, dotComDomainSpaceCert)
                                .put(CrossChainDomain.ROOT_DOMAIN_SPACE, trustRootCert)
                                .build()
                )
        );

        Assert.assertFalse(
                bcdnsManager.validateCrossChainCertificate(
                        relayerCertWrongIssuer,
                        MapUtil.builder(dotComDomainSpace, dotComDomainSpaceCert)
                                .put(CrossChainDomain.ROOT_DOMAIN_SPACE, trustRootCert)
                                .build()
                )
        );

        Assert.assertFalse(
                bcdnsManager.validateCrossChainCertificate(
                        relayerCert,
                        MapUtil.builder(dotComDomainSpace, dotComDomainSpaceCertWrongIssuer)
                                .put(CrossChainDomain.ROOT_DOMAIN_SPACE, trustRootCert)
                                .build()
                )
        );
    }

    @Test
    public void testApplyDomainCertificate() {
        initRootBCDNS();
        Mockito.when(bcdnsService.applyDomainNameCertificate(Mockito.any())).thenReturn(
                new ApplyDomainNameCertificateResponse(APPLICATION_RECEIPT)
        );
        Mockito.when(bcdnsService.queryDomainNameCertificate(Mockito.any())).thenReturn(
                new QueryDomainNameCertificateResponse(false, null)
        );

        Assert.assertEquals(
                APPLICATION_RECEIPT,
                bcdnsManager.applyDomainCertificate(
                        CrossChainDomain.ROOT_DOMAIN_SPACE,
                        antChainDotComDomain,
                        new BIDInfoObjectIdentity(bid),
                        rawBIDDocument
                )
        );
        Mockito.verify(bcdnsService).applyDomainNameCertificate(Mockito.any());
    }

    @Test
    public void testQueryAndSaveDomainCertificateFromBCDNS() {
        initRootBCDNS();
        Mockito.when(bcdnsService.queryDomainNameCertificate(Mockito.any())).thenReturn(
                new QueryDomainNameCertificateResponse(true, antchainDotCommCert)
        );

        AbstractCrossChainCertificate certificate = bcdnsManager.queryDomainCertificateFromBCDNS(
                antChainDotComDomain, CrossChainDomain.ROOT_DOMAIN_SPACE, true
        );
        Assert.assertNotNull(certificate);
        Assert.assertEquals(
                antchainDotCommCert.encodeToBase64(),
                certificate.encodeToBase64()
        );
        Assert.assertNotNull(blockchainManager.getDomainCert(antChainDotComDomain));
    }

    @Test
    public void testGetDomainCertApplication() {
        initRootBCDNS();
        Mockito.when(bcdnsService.applyDomainNameCertificate(Mockito.any())).thenReturn(
                new ApplyDomainNameCertificateResponse(APPLICATION_RECEIPT)
        );
        Mockito.when(bcdnsService.queryDomainNameCertificate(Mockito.any())).thenReturn(
                new QueryDomainNameCertificateResponse(false, null)
        );
        Mockito.when(domainCertWrapperCache.containsKey(Mockito.any())).thenReturn(false);

        Assert.assertEquals(
                APPLICATION_RECEIPT,
                bcdnsManager.applyDomainCertificate(
                        CrossChainDomain.ROOT_DOMAIN_SPACE,
                        antChainDotComDomain,
                        new BIDInfoObjectIdentity(bid),
                        rawBIDDocument
                )
        );

        DomainCertApplicationDO domainCertApplicationDO = bcdnsManager.getDomainCertApplication(antChainDotComDomain);
        Assert.assertNotNull(domainCertApplicationDO);
        Assert.assertEquals(
                APPLICATION_RECEIPT,
                domainCertApplicationDO.getApplyReceipt()
        );
        Assert.assertEquals(
                antChainDotComDomain,
                domainCertApplicationDO.getDomain()
        );
        Assert.assertEquals(
                DomainCertApplicationStateEnum.APPLYING,
                domainCertApplicationDO.getState()
        );
    }

    @Test
    public void testRegisterDomainRouter() {
        initRootBCDNS();
        Mockito.doNothing().when(bcdnsService).registerDomainRouter(Mockito.any());
        blockchainRepository.saveDomainCert(new DomainCertWrapper(antchainDotCommCert));
        bcdnsManager.registerBCDNSService(
                dotComDomainSpace,
                BCDNSTypeEnum.BIF,
                "src/test/resources/bcdns/root_bcdns.json",
                "src/test/resources/cc_certs/x.com.crt"
        );

        bcdnsManager.registerDomainRouter(antChainDotComDomain);
    }

    @Test
    public void testGetDomainRouter() {
        initRootBCDNS();
        Mockito.when(bcdnsService.queryDomainRouter(Mockito.any())).thenReturn(
                new DomainRouter(
                        new CrossChainDomain(antChainDotComDomain),
                        new Relayer(
                                relayerCert.getId(),
                                relayerCert,
                                ListUtil.toList("https://0.0.0.0:8082")
                        )
                )
        );

        Assert.assertNotNull(
                bcdnsManager.getDomainRouter(antChainDotComDomain)
        );
    }

    @Before
    public void initMock() {
        Mockito.when(bcdnsService.queryBCDNSTrustRootCertificate()).thenReturn(
                new QueryBCDNSTrustRootCertificateResponse(trustRootCert)
        );
        mockedStatic.when(
                () -> BlockChainDomainNameServiceFactory.create(Mockito.any(), Mockito.any())
        ).thenReturn(bcdnsService);
    }

    @After
    public void clearMock() {
        mockedStatic.close();
    }

    private void initRootBCDNS() {
        bcdnsManager.registerBCDNSService(
                CrossChainDomain.ROOT_DOMAIN_SPACE,
                BCDNSTypeEnum.BIF,
                "src/test/resources/bcdns/root_bcdns.json",
                null
        );
        bcdnsManager.saveDomainSpaceCerts(
                MapUtil.builder(dotComDomainSpace, dotComDomainSpaceCert).build()
        );
    }
}
