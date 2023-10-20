package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

//import cn.caict.model.response.result.data.TransactionHistory;

public class TxQueryRespDto {
    private String txStatus;
//    private TransactionHistory txContent;

    public String getTxStatus() {
        return txStatus;
    }

    public void setTxStatus(String txStatus) {
        this.txStatus = txStatus;
    }

//    public TransactionHistory getTxContent() {
//        return txContent;
//    }

//    public void setTxContent(TransactionHistory txContent) {
//        this.txContent = txContent;
//    }
}