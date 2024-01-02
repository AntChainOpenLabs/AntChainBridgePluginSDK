package com.alipay.antchain.bridge.plugins.mychain.contract;

/**
 * 抽象的P2PMsgClient服务合约，定义了MYOracle的P2PMsgClient合约模型。
 * <p>
 * 不同的链有不同的实现。
 */
public interface AbstractSDPContractClient {

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //  合约接口
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 设置AM协议合约和本地域名，本地域名已经记录在sdp合约中了
     *
     * @param amContractName
     * @return
     */
    boolean setAmContractAndDomain(String amContractName);

    /**
     * 查询指定跨链元组的P2P消息序号，to合约为当前mychain上的合约
     *
     * @param senderDomain
     * @param from
     * @param receiverDomain
     * @param to
     * @return
     */
    long queryP2PMsgSeqOnChain(String senderDomain, String from, String receiverDomain, String to);


    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // 合约管理接口，部署、升级合约及合约数据解析
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 部署合约
     *
     * @return
     */
    boolean deployContract();

}