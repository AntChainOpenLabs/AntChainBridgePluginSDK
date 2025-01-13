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

package com.alipay.antchain.bridge.relayer.bootstrap.manager;

import javax.annotation.Resource;

import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.ptc.service.IPTCService;
import com.alipay.antchain.bridge.ptc.types.PtcFeatureDescriptor;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.PtcServiceDO;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IPtcContract;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.PtcManager;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.PtcServiceFactory;
import com.alipay.antchain.bridge.relayer.core.service.anchor.tasks.BlockTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.core.service.anchor.tasks.NotifyTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeteroBlockchainClient;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.IPtcServiceRepository;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigInteger;

import static org.mockito.Mockito.*;

public class BlockchainManagerV1Test extends TestBase {

    @MockBean
    private ICrossChainMessageRepository crossChainMessageRepository;

    @MockBean
    private BlockchainClientPool blockchainClientPool;

    @MockBean
    private IBlockchainRepository blockchainRepository;

    @MockBean
    private IPtcServiceRepository ptcServiceRepository;

    @MockBean
    private IBCDNSManager bcdnsManager;

    @Resource
    private PtcManager ptcManager;

    @Resource
    private IBlockchainManager blockchainManager;

    @Test
    public void testCheckTpBtaReadyOnReceivingChain() {

        IPtcContract ptcContract = mock(IPtcContract.class);
        when(ptcContract.checkIfTpBtaOnChain(notNull(), anyInt())).thenReturn(false);
        when(ptcContract.checkIfVerifyAnchorOnChain(notNull(), notNull())).thenReturn(false);
        when(ptcContract.checkIfPtcTypeSupportOnChain(any())).thenReturn(true);
        doNothing().when(ptcContract).updatePtcTrustRoot(any());
        doNothing().when(ptcContract).addTpBtaOnChain(any());

        AbstractBlockchainClient blockchainClient = mock(HeteroBlockchainClient.class);
        when(blockchainClient.getPtcContract()).thenReturn(ptcContract);

        IBlockChainDomainNameService bcdnsService = mock(IBlockChainDomainNameService.class);
        when(bcdnsService.queryThirdPartyBlockchainTrustAnchor(any())).thenReturn(tpbtaWithChannelLevel);
        when(bcdnsService.queryPTCTrustRoot(any())).thenReturn(ptcTrustRoot);

        when(crossChainMessageRepository.getTpProof(anyString())).thenReturn(thirdPartyProof);
        when(blockchainClientPool.getClient(anyString(), anyString())).thenReturn(blockchainClient);
        when(ptcServiceRepository.hasTpBta(notNull(), anyInt())).thenReturn(false);
        when(bcdnsManager.getBCDNSService(anyString())).thenReturn(bcdnsService);

        blockchainManager.checkTpBtaReadyOnReceivingChain(sdpMsgWrapper);
    }

    @Test
    public void testInitTpBta() {
        MockedStatic<PtcServiceFactory> ptcServiceFactoryMockedStatic = null;
        try {
            PtcFeatureDescriptor ptcFeatureDescriptor = new PtcFeatureDescriptor();
            ptcFeatureDescriptor.enableStorage();

            IPTCService ptcService = mock(IPTCService.class);
            when(ptcService.verifyBlockchainTrustAnchor(any(), any())).thenReturn(tpbtaWithChannelLevel);
            when(ptcService.commitAnchorState(any(), any(), any())).thenReturn(anchorVcs);
            when(ptcService.getPtcFeatureDescriptor()).thenReturn(ptcFeatureDescriptor);

            ptcServiceFactoryMockedStatic = mockStatic(PtcServiceFactory.class);
            ptcServiceFactoryMockedStatic.when(() -> PtcServiceFactory.createPtcServiceStub(any(), any()))
                    .thenReturn(ptcService);

            AbstractBlockchainClient blockchainClient = mock(HeteroBlockchainClient.class);
            when(blockchainClient.getConsensusState(any())).thenReturn(anchorState);

            when(blockchainClientPool.getClient(anyString(), anyString())).thenReturn(blockchainClient);
            when(ptcServiceRepository.hasPtcServiceData(any())).thenReturn(true);
            when(ptcServiceRepository.getPtcServiceData(anyString())).thenReturn(PTC_SERVICE_DO);
            when(blockchainRepository.hasBlockchain(anyString())).thenReturn(true);
            when(blockchainRepository.getBlockchainMetaByDomain(anyString())).thenReturn(testchain1Meta);
            when(blockchainRepository.getDomainCert(anyString())).thenReturn(new DomainCertWrapper(antchainDotCommCert));
            doNothing().when(blockchainRepository).saveBta(any());
            doNothing().when(blockchainRepository).setValidatedConsensusState(any());
            doNothing().when(ptcServiceRepository).setTpBta(any());

            blockchainManager.initTpBta(PTC_SERVICE_ID1, antchainBta);
        } finally {
            if (ptcServiceFactoryMockedStatic != null) {
                ptcServiceFactoryMockedStatic.close();
            }
        }
    }

    @Test
    public void testCheckAndProcessMessageTimeouts() {
        IPtcContract ptcContract = mock(IPtcContract.class);
        when(ptcContract.checkIfTpBtaOnChain(notNull(), anyInt())).thenReturn(false);
        when(ptcContract.checkIfVerifyAnchorOnChain(notNull(), notNull())).thenReturn(false);
        when(ptcContract.checkIfPtcTypeSupportOnChain(any())).thenReturn(true);
        doNothing().when(ptcContract).updatePtcTrustRoot(any());
        doNothing().when(ptcContract).addTpBtaOnChain(any());

        AbstractBlockchainClient blockchainClient = mock(HeteroBlockchainClient.class);
        when(blockchainClient.getPtcContract()).thenReturn(ptcContract);

        IBlockChainDomainNameService bcdnsService = mock(IBlockChainDomainNameService.class);
        when(bcdnsService.queryThirdPartyBlockchainTrustAnchor(any())).thenReturn(tpbtaWithChannelLevel);
        when(bcdnsService.queryPTCTrustRoot(any())).thenReturn(ptcTrustRoot);

        when(crossChainMessageRepository.getTpProof(anyString())).thenReturn(thirdPartyProof);
        when(blockchainClientPool.getClient(anyString(), anyString())).thenReturn(blockchainClient);
        when(ptcServiceRepository.hasTpBta(notNull(), anyInt())).thenReturn(false);
        when(bcdnsManager.getBCDNSService(anyString())).thenReturn(bcdnsService);

        IPTCService ptcService = mock(IPTCService.class);
        when(ptcService.getPtcFeatureDescriptor()).thenReturn(new PtcFeatureDescriptor());
        when(ptcService.endorseBlockState(tpBtaDO_cat2mychain.getTpbta(),
                tpBtaDO_cat2mychain.getCrossChainLane().getSenderDomain(),
                validatedConsensusStateDO_cat.getValidatedConsensusState())).thenReturn(thirdPartyProof_catVcs);

        when(ptcServiceRepository.getMatchedTpBta(any())).thenReturn(tpBtaDO_cat2mychain);
        when(ptcServiceRepository.hasPtcServiceData(tpBtaDO_cat2mychain.getPtcServiceId())).thenReturn(true);
        when(blockchainRepository.getAnchorProcessHeight(
                sdpMsgWrapper.getReceiverBlockchainProduct(),
                sdpMsgWrapper.getReceiverBlockchainId(),
                BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(NotifyTaskTypeEnum.SYSTEM_WORKER.getCode()),
                tpbtaLaneChannelLevel_cat2mychain
        )).thenReturn(currHeight.get());
        when(blockchainRepository.getValidatedConsensusState(
                tpBtaDO_cat2mychain.getPtcServiceId(),
                tpBtaDO_cat2mychain.getCrossChainLane().getSenderDomain().getDomain(),
                tpBtaDO_cat2mychain.getCrossChainLane(),
                BigInteger.valueOf(currHeight.incrementAndGet()))).thenReturn(validatedConsensusStateDO_cat);
        when(crossChainMessageRepository.putAuthMessageWithIdReturned(any())).thenReturn(0L);

        blockchainManager.checkAndProcessMessageTimeouts(sdpMsgWrapperV3);
    }
}
