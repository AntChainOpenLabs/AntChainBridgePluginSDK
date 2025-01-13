package com.alipay.antchain.bridge.bcdns.embedded.state.starter.mbp.entities;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@TableName("embedded_bcdns_tpbta")
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
@Builder
public class TpBtaEntity extends BaseEntity {

    @TableField("version")
    private Integer version;

    @TableField("bta_subject_version")
    private Integer btaSubjectVersion;

    @TableField("sender_domain")
    private String senderDomain;

    @TableField("sender_id")
    private String senderId;

    @TableField("receiver_domain")
    private String receiverDomain;

    @TableField("receiver_id")
    private String receiverId;

    @TableField("tpbta_version")
    private Integer tpbtaVersion;

    @TableField("ptc_oid_hex")
    private String ptcOidHex;

    @TableField("ptc_verify_anchor_version")
    private String ptcVerifyAnchorVersion;

    @TableField("raw_tpbta")
    private byte[] rawTpbta;
}
