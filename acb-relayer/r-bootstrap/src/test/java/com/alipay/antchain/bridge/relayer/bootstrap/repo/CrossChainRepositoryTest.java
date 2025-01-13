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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageTrustLevelEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.core.ptc.ThirdPartyProof;
import com.alipay.antchain.bridge.commons.core.rcc.IdempotentInfo;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMessage;
import com.alipay.antchain.bridge.commons.core.rcc.ReliableCrossChainMsgProcessStateEnum;
import com.alipay.antchain.bridge.commons.core.sdp.AbstractSDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.AtomicFlagEnum;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageId;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV2;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import com.alipay.antchain.bridge.relayer.dal.entities.*;
import com.alipay.antchain.bridge.relayer.dal.mapper.*;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@Slf4j
public class CrossChainRepositoryTest extends TestBase {

    @BeforeClass
    public static void before() throws Exception {
        AuthMessageV2 authMessageV2 = new AuthMessageV2();
        authMessageV2.setIdentity(CrossChainIdentity.fromHexStr(DigestUtil.sha256Hex("01")));
        authMessageV2.setTrustLevel(AuthMessageTrustLevelEnum.POSITIVE_TRUST);
        authMessageV2.setUpperProtocol(UpperProtocolTypeBeyondAMEnum.SDP.ordinal());
        authMessageV2.setPayload("".getBytes());

        authMessagePositiveTrust = authMessageV2;

        AuthMessageV2 authMessageV2Neg = new AuthMessageV2();
        authMessageV2Neg.setIdentity(CrossChainIdentity.fromHexStr(DigestUtil.sha256Hex("01")));
        authMessageV2Neg.setTrustLevel(AuthMessageTrustLevelEnum.NEGATIVE_TRUST);
        authMessageV2Neg.setUpperProtocol(UpperProtocolTypeBeyondAMEnum.SDP.ordinal());
        authMessageV2Neg.setPayload("".getBytes());

        authMessageNegativeTrust = authMessageV2Neg;

        AuthMessageV2 authMessageV2Zero = new AuthMessageV2();
        authMessageV2Zero.setIdentity(CrossChainIdentity.fromHexStr(DigestUtil.sha256Hex("01")));
        authMessageV2Zero.setTrustLevel(AuthMessageTrustLevelEnum.ZERO_TRUST);
        authMessageV2Zero.setUpperProtocol(UpperProtocolTypeBeyondAMEnum.SDP.ordinal());
        authMessageV2Zero.setPayload("".getBytes());

        authMessageZeroTrust = authMessageV2Zero;

        SDPMessageV2 sdpMessageV2 = new SDPMessageV2();
        sdpMessageV2.setAtomicFlag(AtomicFlagEnum.ATOMIC_REQUEST);
        sdpMessageV2.setSdpPayload(new SDPMessageV2.SDPPayloadV2("".getBytes()));
        sdpMessageV2.setTargetDomain(new CrossChainDomain("dest"));
        sdpMessageV2.setSequence(100);
        sdpMessageV2.setTargetIdentity(CrossChainIdentity.fromHexStr(DigestUtil.sha256Hex("02")));
        sdpMessageV2.setNonce(-1);
        sdpMessageV2.setMessageId(new SDPMessageId(RandomUtil.randomBytes(32)));

        sdpMessage = sdpMessageV2;
    }

    private static IAuthMessage authMessagePositiveTrust;

    private static IAuthMessage authMessageNegativeTrust;

    private static IAuthMessage authMessageZeroTrust;

    private static AbstractSDPMessage sdpMessage;

    private boolean ifAlreadyWriteAM = false;

    private boolean ifAlreadyWriteSDP = false;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private AuthMsgArchiveMapper authMsgArchiveMapper;

    @Resource
    private AuthMsgPoolMapper authMsgPoolMapper;

    @Resource
    private SDPMsgPoolMapper sdpMsgPoolMapper;

    @Resource
    private SDPMsgArchiveMapper sdpMsgArchiveMapper;

    @Resource
    private ReliableCrossChainMsgMapper reliableCrossChainMsgMapper;

    @Test
    public void testSaveAuthMessages() {
        saveElevenAM(getAMCurrentId());
    }

    @Test
    public void testPeekAuthMessages() {
        saveElevenAM(getAMCurrentId());

        List<AuthMsgWrapper> authMsgWrappers = crossChainMessageRepository.peekAuthMessages("test", 10, 10);
        Assert.assertEquals(10, authMsgWrappers.size());
    }

    @Test
    public void testPeekAuthMessagesWithDifferentTrustLevel() {
        saveElevenAM(getAMCurrentId());
        saveElevenNegativeTrustAM(getAMCurrentId());
        saveElevenZeroTrustAM(getAMCurrentId());

        List<AuthMsgWrapper> authMsgWrappers = crossChainMessageRepository.peekAuthMessages("test", 33, 10);
        Assert.assertEquals(32, authMsgWrappers.size());
    }

    @Test
    public void testPeekAuthMessagesOutOfRetryTimes() {
        saveElevenAM(getAMCurrentId());
        saveElevenNegativeTrustAM(getAMCurrentId());
        saveElevenZeroTrustAM(getAMCurrentId());

        AuthMsgPoolEntity entity = new AuthMsgPoolEntity();
        entity.setId(1L);
        entity.setFailCount(10);
        authMsgPoolMapper.updateById(entity);

        List<AuthMsgWrapper> authMsgWrappers = crossChainMessageRepository.peekAuthMessages("test", 33, 10);
        Assert.assertEquals(31, authMsgWrappers.size());
    }

    @Test
    public void testArchiveAuthMessages() {
        saveElevenAM(getAMCurrentId());

        Assert.assertEquals(
                3,
                crossChainMessageRepository.archiveAuthMessages(ListUtil.of(1L, 3L, 5L))
        );

        List<AuthMsgArchiveEntity> archiveEntities = authMsgArchiveMapper.selectBatchIds(ListUtil.of(1L, 3L, 5L));
        Assert.assertEquals(
                new Long(1L),
                archiveEntities.get(0).getId()
        );
        Assert.assertEquals(
                new Long(3L),
                archiveEntities.get(1).getId()
        );
    }

    @Test
    public void testPutAuthMessages() {
        List<AuthMsgWrapper> authMsgWrappers = new ArrayList<>();
        for (int i = 990; i < 1000; i++) {
            authMsgWrappers.add(
                    new AuthMsgWrapper(
                            "test",
                            "test",
                            "test",
                            HexUtil.encodeHexStr(ByteUtil.intToBytes(i)),
                            "am",
                            AuthMsgProcessStateEnum.PROCESSED, 0,
                            authMessagePositiveTrust)
            );
        }

        Assert.assertEquals(10, crossChainMessageRepository.putAuthMessages(authMsgWrappers));
    }

    @Test
    public void testPutSDPMessage() throws Exception {
        sdpMsgPoolMapper.selectList(null);

        saveSomeSDP();

        System.out.println(sdpMsgPoolMapper.update(
                SDPMsgPoolEntity.builder().build(),
                new LambdaUpdateWrapper<SDPMsgPoolEntity>().eq(BaseEntity::getId, 100)));
    }

    @Test
    public void testArchiveSDPMessages() {
        saveSomeSDP();

        Assert.assertEquals(
                3,
                crossChainMessageRepository.archiveSDPMessages(ListUtil.of(1L, 3L, 5L))
        );

        List<SDPMsgArchiveEntity> archiveEntities = sdpMsgArchiveMapper.selectBatchIds(ListUtil.of(1L, 3L, 5L));
        Assert.assertEquals(
                new Long(1L),
                archiveEntities.get(0).getId()
        );
        Assert.assertEquals(
                new Long(3L),
                archiveEntities.get(1).getId()
        );
    }

    @Test
    public void testUpdateSDPMessage() {
        saveSomeSDP();
        String txHash = HexUtil.encodeHexStr(RandomUtil.randomBytes(32));
        Assert.assertTrue(
                crossChainMessageRepository.updateSDPMessage(
                        new SDPMsgWrapper(
                                10L,
                                new AuthMsgWrapper(
                                        10,
                                        "test",
                                        "test",
                                        "test",
                                        HexUtil.encodeHexStr(ByteUtil.intToBytes(9)),
                                        "am",
                                        AuthMsgProcessStateEnum.PENDING,
                                        0,
                                        new byte[]{},
                                        authMessagePositiveTrust
                                ),
                                "eth",
                                "ethid",
                                null,
                                SDPMsgProcessStateEnum.TX_SUCCESS,
                                txHash,
                                true,
                                "",
                                sdpMessage,
                                Long.MAX_VALUE,
                                Long.MAX_VALUE
                        )
                )
        );

        SDPMsgWrapper sdpMsgWrapper = crossChainMessageRepository.getSDPMessage(10, false);
        Assert.assertNotNull(sdpMsgWrapper.getReceiverAMClientContract());

        Assert.assertEquals(
                txHash,
                sdpMsgWrapper.getTxHash()
        );
        Assert.assertEquals(
                SDPMsgProcessStateEnum.TX_SUCCESS,
                sdpMsgWrapper.getProcessState()
        );
        Assert.assertTrue(sdpMsgWrapper.isTxSuccess());
    }

    @Test
    public void testUpdateSDPMessageResults() {
        saveSomeSDP();

        List<SDPMsgCommitResult> results = new ArrayList<>();

        SDPMsgCommitResult sdpMsgCommitResult = new SDPMsgCommitResult();
        sdpMsgCommitResult.setCommitSuccess(true);
        sdpMsgCommitResult.setConfirmed(true);
        sdpMsgCommitResult.setTxHash(DigestUtil.sha256Hex(Integer.toString(10)));
        sdpMsgCommitResult.setFailReason("test!");
        sdpMsgCommitResult.setBlockTimestamp(System.currentTimeMillis());
        sdpMsgCommitResult.setReceiveProduct("eth");
        sdpMsgCommitResult.setReceiveBlockchainId("ethid");

        results.add(sdpMsgCommitResult);

        crossChainMessageRepository.updateSDPMessageResults(results);

        SDPMsgWrapper sdpMsgWrapper = crossChainMessageRepository.getSDPMessage(DigestUtil.sha256Hex(Integer.toString(10)));
        Assert.assertEquals(
                SDPMsgProcessStateEnum.TX_SUCCESS,
                sdpMsgWrapper.getProcessState()
        );
    }

    @Test
    public void testPeekTxPendingSDPMessageIds() {
        saveSomeSDP();

        Assert.assertTrue(
                crossChainMessageRepository.updateSDPMessage(
                        new SDPMsgWrapper(
                                10L,
                                new AuthMsgWrapper(
                                        10 + 1,
                                        "test",
                                        "test",
                                        "test",
                                        HexUtil.encodeHexStr(ByteUtil.intToBytes(10)),
                                        "am",
                                        AuthMsgProcessStateEnum.PENDING,
                                        0,
                                        new byte[]{},
                                        authMessagePositiveTrust
                                ),
                                "eth",
                                "ethid",
                                "am",
                                SDPMsgProcessStateEnum.TX_SUCCESS,
                                DigestUtil.sha256Hex(Integer.toString(10)),
                                true,
                                "",
                                sdpMessage,
                                Long.MAX_VALUE,
                                Long.MAX_VALUE
                        )
                )
        );

        Assert.assertTrue(
                crossChainMessageRepository.updateSDPMessage(
                        new SDPMsgWrapper(
                                9L,
                                new AuthMsgWrapper(
                                        9 + 1,
                                        "test",
                                        "test",
                                        "test",
                                        HexUtil.encodeHexStr(ByteUtil.intToBytes(9)),
                                        "am",
                                        AuthMsgProcessStateEnum.PENDING,
                                        0,
                                        new byte[]{},
                                        authMessagePositiveTrust
                                ),
                                "eth",
                                "ethid",
                                "am",
                                SDPMsgProcessStateEnum.TX_FAILED,
                                DigestUtil.sha256Hex(Integer.toString(9)),
                                false,
                                "test!",
                                sdpMessage,
                                Long.MAX_VALUE,
                                Long.MAX_VALUE
                        )
                )
        );

        List<SDPMsgWrapper> res = crossChainMessageRepository.peekTxFinishedSDPMessageIds(
                "eth",
                "ethid",
                10
        );

        Assert.assertEquals(2, res.size());
        Assert.assertTrue(res.stream().anyMatch(x -> x.getId() == 10));
        Assert.assertTrue(res.stream().anyMatch(x -> x.getId() == 9));
    }

    @Test
    public void testCountSDPMessagesByState() {
        saveSomeSDP();
        Assert.assertTrue(
                crossChainMessageRepository.updateSDPMessage(
                        new SDPMsgWrapper(
                                9L,
                                new AuthMsgWrapper(
                                        9 + 1,
                                        "test",
                                        "test",
                                        "test",
                                        HexUtil.encodeHexStr(ByteUtil.intToBytes(9)),
                                        "am",
                                        AuthMsgProcessStateEnum.PENDING,
                                        0,
                                        new byte[]{},
                                        authMessagePositiveTrust
                                ),
                                "eth",
                                "ethid",
                                "am",
                                SDPMsgProcessStateEnum.TX_FAILED,
                                DigestUtil.sha256Hex(Integer.toString(9)),
                                false,
                                "test!",
                                sdpMessage,
                                Long.MAX_VALUE,
                                Long.MAX_VALUE
                        )
                )
        );
        Assert.assertEquals(
                10,
                crossChainMessageRepository.countSDPMessagesByState(
                        "eth",
                        "ethid",
                        SDPMsgProcessStateEnum.PENDING
                )
        );
    }

    @Test
    public void testGetAuthMessage() {
        saveElevenAM(getAMCurrentId());

        AuthMsgWrapper authMsgWrapper = crossChainMessageRepository.getAuthMessage(1, true);
        Assert.assertNotNull(authMsgWrapper);
        Assert.assertEquals(authMsgWrapper.getDomain(), "test");
        Assert.assertNotNull(authMsgWrapper.getAuthMessage());
    }

    @Test
    public void testTpProof() {
        crossChainMessageRepository.putUniformCrosschainPacket(ucpContext);

        UniformCrosschainPacketContext uniformCrosschainPacketContext = crossChainMessageRepository.getUniformCrosschainPacket(ucpContext.getUcpId(), false);
        Assert.assertArrayEquals(
                ucpContext.getUcp().encode(),
                uniformCrosschainPacketContext.getUcp().encode()
        );
        Assert.assertEquals(
                testchain1Meta.getProduct(),
                uniformCrosschainPacketContext.getProduct()
        );
        Assert.assertEquals(
                testchain1Meta.getBlockchainId(),
                uniformCrosschainPacketContext.getBlockchainId()
        );

        crossChainMessageRepository.putTpProof(ucpContext.getUcpId(), thirdPartyProof);

        ThirdPartyProof tpProof = crossChainMessageRepository.getTpProof(ucpContext.getUcpId());
        Assert.assertArrayEquals(
                thirdPartyProof.encode(),
                tpProof.encode()
        );
    }

    @Test
    public void testPutReliableMessage() {
        reliableCrossChainMsgMapper.selectList(null);

        saveSomeRCC();

        System.out.println(reliableCrossChainMsgMapper.update(
                ReliableCrossChainMsgEntity.builder().build(),
                new LambdaUpdateWrapper<ReliableCrossChainMsgEntity>().eq(BaseEntity::getId, 100)));
    }

    @Test
    public void testpeekPendingReliableMessages() {
        saveSomeRCC();

        int[] pendingNum = {7, 9};

        Assert.assertTrue(
                crossChainMessageRepository.updateRCCMessage(
                        new ReliableCrossChainMessage(
                                new IdempotentInfo(
                                        antChainDotComDomain,
                                        antchainIdentity.getRawID(),
                                        catChainDotComDomain,
                                        catchainIdentity.getRawID(),
                                        pendingNum[0]
                                ),
                                ReliableCrossChainMsgProcessStateEnum.PENDING,
                                HexUtil.encodeHexStr(ByteUtil.intToBytes(pendingNum[0])),
                                RandomUtil.randomString(32),
                                1,
                                System.currentTimeMillis(),
                                "errMsg",
                                RandomUtil.randomBytes(32)
                        )
                )
        );

        Assert.assertTrue(
                crossChainMessageRepository.updateRCCMessage(
                        new ReliableCrossChainMessage(
                                new IdempotentInfo(
                                        antChainDotComDomain,
                                        antchainIdentity.getRawID(),
                                        catChainDotComDomain,
                                        catchainIdentity.getRawID(),
                                        pendingNum[1]
                                ),
                                ReliableCrossChainMsgProcessStateEnum.PENDING,
                                HexUtil.encodeHexStr(ByteUtil.intToBytes(pendingNum[1])),
                                RandomUtil.randomString(32),
                                1,
                                System.currentTimeMillis(),
                                "errMsg",
                                RandomUtil.randomBytes(32)
                        )
                )
        );

        List<ReliableCrossChainMessage> res = crossChainMessageRepository.peekPendingReliableMessages(
                catChainDotComDomain,
                10
        );

        Assert.assertEquals(2, res.size());
        Assert.assertTrue(res.stream().anyMatch(x -> x.getIdempotentInfo().getNonce() == 7));
        Assert.assertTrue(res.stream().anyMatch(x -> x.getIdempotentInfo().getNonce() == 9));
    }

    @Test
    public void testGetReliableMessagesByIdempotentInfo() {
        saveSomeRCC();

        ReliableCrossChainMessage rcc = crossChainMessageRepository.getReliableMessagesByIdempotentInfo(new IdempotentInfo(
                antChainDotComDomain,
                antchainIdentity.getRawID(),
                catChainDotComDomain,
                catchainIdentity.getRawID(),
                7
        ));
        Assert.assertNotNull(rcc);
        Assert.assertEquals(rcc.getIdempotentInfo().getSenderDomain(), antChainDotComDomain);
        Assert.assertNotNull(rcc.getOriginalHash(), HexUtil.encodeHexStr(ByteUtil.intToBytes(7)));
    }

    @Test
    public void testUpdateRCCMessageBatch(){
        saveSomeRCC();

        int[] updateNum = {3, 4};

        crossChainMessageRepository.updateRCCMessageBatch(new ArrayList<>(Arrays.asList(
                new ReliableCrossChainMessage(new IdempotentInfo(
                        antChainDotComDomain,
                        antchainIdentity.getRawID(),
                        catChainDotComDomain,
                        catchainIdentity.getRawID(),
                        updateNum[0]
                ),
                        ReliableCrossChainMsgProcessStateEnum.FAILED,
                        HexUtil.encodeHexStr(ByteUtil.intToBytes(updateNum[0])),
                        RandomUtil.randomString(32),
                        1,
                        System.currentTimeMillis(),
                        "errMsg",
                        RandomUtil.randomBytes(32)),
                new ReliableCrossChainMessage(new IdempotentInfo(
                        antChainDotComDomain,
                        antchainIdentity.getRawID(),
                        catChainDotComDomain,
                        catchainIdentity.getRawID(),
                        updateNum[1]
                ),
                        ReliableCrossChainMsgProcessStateEnum.FAILED,
                        HexUtil.encodeHexStr(ByteUtil.intToBytes(updateNum[1])),
                        RandomUtil.randomString(32),
                        1,
                        System.currentTimeMillis(),
                        "errMsg",
                        RandomUtil.randomBytes(32))
        ))).forEach(Assert::assertTrue);
    }

    @Test
    public void testDeleteExpiredReliableMessages() {
        saveSomeRCC();

        Assert.assertEquals(11, crossChainMessageRepository.deleteExpiredReliableMessages(0));;
    }

    private long getAMCurrentId() {
        AuthMsgPoolEntity entity = new AuthMsgPoolEntity();
        entity.setId(0L);
        return ObjectUtil.defaultIfNull(
                authMsgPoolMapper.selectOne(new QueryWrapper<AuthMsgPoolEntity>().select("max(id) as id")),
                entity
        ).getId();
    }

    private void saveElevenAM(long startId) {

        for (int i = 0; i < 11; i++) {
            Assert.assertEquals(
                    startId + i + 1,
                    crossChainMessageRepository.putAuthMessageWithIdReturned(
                            new AuthMsgWrapper(
                                    "test",
                                    "test",
                                    "test",
                                    HexUtil.encodeHexStr(ByteUtil.longToBytes(startId + i + 1)),
                                    "am",
                                    AuthMsgProcessStateEnum.PENDING, 0,
                                    authMessagePositiveTrust)
                    )
            );
        }
    }

    private void saveElevenNegativeTrustAM(long startId) {

        for (int i = 0; i < 11; i++) {
            Assert.assertEquals(
                    startId + i + 1,
                    crossChainMessageRepository.putAuthMessageWithIdReturned(
                            new AuthMsgWrapper(
                                    "test",
                                    "test",
                                    "test",
                                    HexUtil.encodeHexStr(ByteUtil.longToBytes(startId + i + 1)),
                                    "am",
                                    i == 10 ? AuthMsgProcessStateEnum.PENDING : AuthMsgProcessStateEnum.PROVED,
                                    0,
                                    authMessageNegativeTrust)
                    )
            );
        }
    }

    private void saveElevenZeroTrustAM(long startId) {

        for (int i = 0; i < 11; i++) {
            Assert.assertEquals(
                    startId + i + 1,
                    crossChainMessageRepository.putAuthMessageWithIdReturned(
                            new AuthMsgWrapper(
                                    "test",
                                    "test",
                                    "test",
                                    HexUtil.encodeHexStr(ByteUtil.longToBytes(startId + i + 1)),
                                    "am",
                                    AuthMsgProcessStateEnum.PROVED,
                                    0,
                                    authMessageNegativeTrust)
                    )
            );
        }
    }

    private long getSDPCurrentId() {
        SDPMsgPoolEntity entity = SDPMsgPoolEntity.builder().build();
        entity.setId(0L);
        return ObjectUtil.defaultIfNull(
                sdpMsgPoolMapper.selectOne(new QueryWrapper<SDPMsgPoolEntity>().select("max(id) as id")),
                entity
        ).getId();
    }

    private void saveSomeSDP() {

        for (int i = 0; i < 11; i++) {
            crossChainMessageRepository.putSDPMessage(
                    new SDPMsgWrapper(
                            "eth",
                            "ethid",
                            "am",
                            SDPMsgProcessStateEnum.PENDING,
                            DigestUtil.sha256Hex(Integer.toString(i)),
                            false,
                            "",
                            new AuthMsgWrapper(
                                    i + 1,
                                    "test",
                                    "test",
                                    "test",
                                    HexUtil.encodeHexStr(ByteUtil.intToBytes(i)),
                                    "am",
                                    AuthMsgProcessStateEnum.PENDING,
                                    0,
                                    new byte[]{},
                                    authMessagePositiveTrust
                            ),
                            sdpMessage
                    )
            );
        }
    }

    private void saveSomeRCC() {
        for (int i = 0; i < 11; i++) {
            crossChainMessageRepository.putReliableMessage(
                    new ReliableCrossChainMessage(
                            new IdempotentInfo(
                                    antChainDotComDomain,
                                    antchainIdentity.getRawID(),
                                    catChainDotComDomain,
                                    catchainIdentity.getRawID(),
                                    i
                            ),
                            ReliableCrossChainMsgProcessStateEnum.SUCCESS,
                            HexUtil.encodeHexStr(ByteUtil.intToBytes(i)),
                            RandomUtil.randomString(32),
                            0,
                            System.currentTimeMillis(),
                            "errMsg",
                            RandomUtil.randomBytes(32)
                    )
            );
        }
    }
}
