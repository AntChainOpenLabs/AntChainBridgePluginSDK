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

package com.alipay.antchain.bridge.relayer.core.manager.blockchain;

import java.util.List;
import java.util.Map;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainLane;
import com.alipay.antchain.bridge.commons.core.bta.IBlockchainTrustAnchor;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainAnchorProcess;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.TpBtaDesc;

/**
 * 该Manager提供接入区块链的系列生命周期管理接口，通过该接口可以添加区块链、更新查询区块链元信息、管理链上合约等业务操作
 */
public interface IBlockchainManager {

    /**
     * 添加一条需要监听的区块链，该接口要求传入区块链必要的客户端配置，如节点ip、节点连接证书、区块链账号及私钥等。
     * <p>
     * 该接口仅保存区块链的元信息，具体启动监听的动作需要通过另外一个接口触发。
     *
     * <pre>
     *
     *  该接口会做以下事情
     *   1. 检查提供的区块链节点是否可达
     *   2. 检查提供的区块链连接证书是否可用
     *
     *  完成以上检查，则保存一条可连接的区块链的信息，否则返回配置信息有误。
     *
     * </pre>
     *
     * @param product        区块链产品类型
     * @param blockchainId   区块链id
     * @param pluginServerId 插件服务的ID，空字符串意味着未指定
     * @param alias          区块链别名
     * @param desc           描述
     * @param clientConfig   区块链的客户端配置
     * @return true：配置信息可用 false：配置信息不可用
     */
    void addBlockchain(String product, String blockchainId, String pluginServerId, String alias, String desc,
                       Map<String, String> clientConfig);

    void addBlockchain(BlockchainMeta blockchainMeta);

    /**
     * 更新区块链信息，该接口要求传入区块链必要的客户端配置，如节点ip、节点连接证书、区块链账号及私钥等。
     * <p>
     * 该接口仅更新区块链的元信息，具体启动、重启监听等动作需要通过另外一个接口触发。
     *
     * <pre>
     *
     *  该接口会做以下事情
     *   1. 检查提供的区块链节点是否可达
     *   2. 检查提供的区块链连接证书是否可用
     *
     *  完成以上检查，则保存一条可连接的区块链的信息，否则返回配置信息有误。
     *
     * </pre>
     *
     * @param product      区块链产品类型
     * @param blockchainId 区块链id
     * @param alias        区块链别名
     * @param desc         描述
     * @param clientConfig 区块链的客户端配置
     * @return true：配置信息可用 false：配置信息不可用
     */
    void updateBlockchain(String product, String blockchainId, String pluginServerId, String alias, String desc,
                             Map<String, String> clientConfig);

    /**
     * 更新区块链单个配置
     *
     * @param product
     * @param blockchainId
     * @param confKey
     * @param confValue
     * @return
     */
    void updateBlockchainProperty(String product, String blockchainId, String confKey, String confValue);

    boolean hasBlockchain(String domain);

    DomainCertWrapper getDomainCert(String domain);

    /**
     * 为指定区块链部署 AM合约
     *
     * @param product      区块链产品类型
     * @param blockchainId 区块链id
     * @return true：部署成功 false：部署失败
     */
    void deployAMClientContract(String product, String blockchainId);

    void deployBBCContractAsync(String product, String blockchainId);

    /**
     * 启动指定的区块链anchor（区块链监听程序）
     * <p>
     * 启动区块链的anchor后，anchor会持续监听区块链的每一个区块链上的请求
     *
     * @param product      区块链产品类型
     * @param blockchainId 区块链id
     * @return true：启动成功 false：启动失败
     */
    void startBlockchainAnchor(String product, String blockchainId);

    /**
     * 暂停指定的区块链anchor（区块链监听程序）
     * <p>
     * 暂停区块链的anchor后，anchor会停止监听区块链，
     * 历史已监听的进度会持久化保存，再次启动anchor会从上次暂停的进度继续监听
     *
     * @param product      区块链产品类型
     * @param blockchainId 区块链id
     * @return true：暂停成功 false：暂停失败
     */
    void stopBlockchainAnchor(String product, String blockchainId);

    /**
     * 获取区块链元信息，包括以下信息
     * <li>区块链的客户端配置信息</li>
     * <li>anchor的运行状态</li>
     * <li>链上合约地址</li>
     *
     * @param product      区块链产品类型
     * @param blockchainId 区块链id
     * @return 元信息模型
     */
    BlockchainMeta getBlockchainMeta(String product, String blockchainId);

    BlockchainMeta getBlockchainMetaByDomain(String domain);

    String getBlockchainDomain(String product, String blockchainId);

    /**
     * 更新区块链元信息
     *
     * @param blockchainMeta
     * @return
     */
    boolean updateBlockchainMeta(BlockchainMeta blockchainMeta);

    /**
     * 获取所有区块链元信息
     *
     * @return
     */
    List<BlockchainMeta> getAllBlockchainMeta();

    /**
     * 查询区块链同步信息
     *
     * @param product
     * @param blockchainId
     * @return
     */
    BlockchainAnchorProcess getBlockchainAnchorProcess(String product, String blockchainId);

    /**
     * 查询当前正在服务的区块链，已经注册跨链还没stop的链
     *
     * @return
     */
    List<BlockchainMeta> getAllServingBlockchains();

    List<BlockchainMeta> getAllStoppedBlockchains();

    /**
     * 检查domain是否具有blockchain数据
     *
     * @param domain
     * @return
     */
    boolean checkIfDomainPrepared(String domain);

    /**
     * 检查domain对应的链在运行中
     *
     * @param domain
     * @return
     */
    boolean checkIfDomainRunning(String domain);

    /**
     * 检查domain是否完成了AM合约的部署
     *
     * @param domain
     * @return
     */
    boolean checkIfDomainAMDeployed(String domain);

    /**
     * 获取绑定指定插件服务的所有链
     *
     * @param pluginServerId
     * @return
     */
    List<String> getBlockchainsByPluginServerId(String pluginServerId);

    /**
     * Update the sdp msg sequence for the channel identified
     * by tuple ( senderDomain, from, receiverDomain, to ).
     *
     * @param receiverProduct      product of receiver blockchain
     * @param receiverBlockchainId id of receiver blockchain
     * @param senderDomain         sender domain
     * @param from                 sender contract of the msg
     * @param to                   receiver contract of the msg
     * @param newSeq               new sequence number
     */
    void updateSDPMsgSeq(String receiverProduct, String receiverBlockchainId, String senderDomain, String from, String to, long newSeq);

    /**
     * Query the sdp msg sequence for the channel identified
     * by tuple ( senderDomain, from, receiverDomain, to ).
     *
     * @param receiverProduct      product of receiver blockchain
     * @param receiverBlockchainId id of receiver blockchain
     * @param senderDomain         sender domain
     * @param from                 sender contract of the msg
     * @param to                   receiver contract of the msg
     */
    long querySDPMsgSeq(String receiverProduct, String receiverBlockchainId, String senderDomain, String from, String to);

    /**
     * Check if the tpBta is ready on receiving chain.
     *
     * @param sdpMsgWrapper the sdp msg wrapper
     */
    void checkTpBtaReadyOnReceivingChain(SDPMsgWrapper sdpMsgWrapper);

    /**
     * setup bta for blockchain and send it to ptc to get it endorsed.
     *
     * @param ptcServiceId the ptc service id
     * @param bta the bta
     */
    void initTpBta(String ptcServiceId, IBlockchainTrustAnchor bta);

    List<TpBtaDesc> getMatchedTpBtaDescList(CrossChainLane crossChainLane);

    void upgradeTpBta(String ptcServiceId, CrossChainLane tpbtaLane, IBlockchainTrustAnchor newBta);

    BtaDO queryLatestVersionBta(CrossChainDomain domain);

    boolean checkAndProcessMessageTimeouts(SDPMsgWrapper sdpMsgWrapper);
}

