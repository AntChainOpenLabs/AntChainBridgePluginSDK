package com.ali.antchain.abstarct;

import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;

// 需要不同测试工具实现的定制化测试操作
public interface ITester {
    public String getProtocol(AbstractBBCContext _context);

    public byte[] deployApp();

    public void waitForTxConfirmed(String txhash);

}
