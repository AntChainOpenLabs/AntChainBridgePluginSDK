package com.alipay.antchain.bridge.bcdns.impl.bif.resp;

public class IssuerAuditResultRespDto {

    private String applyNo;
    private String issuerBid;
    private String issuerName;
    private long applyTime;
    private String issuerScope;
    private String issuerTrusted;
    private String audiorBid;
    private String audiorName;
    private String auditStatus;
    private long auditTime;
    private String auditRemark;
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIssuerBid() {
        return issuerBid;
    }

    public void setIssuerBid(String issuerBid) {
        this.issuerBid = issuerBid;
    }

    public String getAudiorBid() {
        return audiorBid;
    }

    public void setAudiorBid(String audiorBid) {
        this.audiorBid = audiorBid;
    }

    public String getApplyNo() {
        return applyNo;
    }

    public void setApplyNo(String applyNo) {
        this.applyNo = applyNo;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public long getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(long applyTime) {
        this.applyTime = applyTime;
    }

    public String getIssuerScope() {
        return issuerScope;
    }

    public void setIssuerScope(String issuerScope) {
        this.issuerScope = issuerScope;
    }

    public String getIssuerTrusted() {
        return issuerTrusted;
    }

    public void setIssuerTrusted(String issuerTrusted) {
        this.issuerTrusted = issuerTrusted;
    }

    public String getAudiorName() {
        return audiorName;
    }

    public void setAudiorName(String audiorName) {
        this.audiorName = audiorName;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    public long getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(long auditTime) {
        this.auditTime = auditTime;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }
}
