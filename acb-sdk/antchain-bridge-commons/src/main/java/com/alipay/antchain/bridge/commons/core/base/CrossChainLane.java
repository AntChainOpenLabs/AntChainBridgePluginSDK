package com.alipay.antchain.bridge.commons.core.base;

import java.util.List;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CrossChainLane {

    private static final int TAG_CROSS_CHAIN_CHANNEL = 0;

    private static final int TAG_SENDER_ID = 1;

    private static final int TAG_RECEIVER_ID = 2;

    public static CrossChainLane decode(byte[] data) {
        return TLVUtils.decode(data, CrossChainLane.class);
    }

    public static CrossChainLane fromLaneKey(String laneKey) {
        List<String> laneKeyList = StrUtil.split(laneKey, "@");
        return new CrossChainLane(
                new CrossChainDomain(laneKeyList.get(0)),
                ObjectUtil.isEmpty(laneKeyList.get(2)) ? null : new CrossChainDomain(laneKeyList.get(2)),
                ObjectUtil.isEmpty(laneKeyList.get(1)) ? null : new CrossChainIdentity(HexUtil.decodeHex(laneKeyList.get(1))),
                ObjectUtil.isEmpty(laneKeyList.get(3)) ? null : new CrossChainIdentity(HexUtil.decodeHex(laneKeyList.get(3)))
        );
    }

    @TLVField(tag = TAG_CROSS_CHAIN_CHANNEL, type = TLVTypeEnum.BYTES)
    private CrossChainChannel crossChainChannel;

    @TLVField(tag = TAG_SENDER_ID, type = TLVTypeEnum.BYTES, order = TAG_SENDER_ID)
    private CrossChainIdentity senderId;

    @TLVField(tag = TAG_RECEIVER_ID, type = TLVTypeEnum.BYTES, order = TAG_RECEIVER_ID)
    private CrossChainIdentity receiverId;

    public CrossChainLane(@NonNull CrossChainDomain senderDomain, CrossChainDomain receiverDomain, CrossChainIdentity senderId, CrossChainIdentity receiverId) {
        this.crossChainChannel = new CrossChainChannel(senderDomain, receiverDomain);
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public CrossChainLane(@NonNull CrossChainDomain senderDomain) {
        this.crossChainChannel = new CrossChainChannel(senderDomain, null);
    }

    public CrossChainLane(@NonNull CrossChainDomain senderDomain, CrossChainDomain receiverDomain) {
        this.crossChainChannel = new CrossChainChannel(senderDomain, receiverDomain);
    }

    public String getSenderIdHex() {
        return (ObjectUtil.isNull(this.senderId) || ObjectUtil.isEmpty(this.senderId.getRawID())) ? null : this.senderId.toHex();
    }

    public String getReceiverIdHex() {
        return (ObjectUtil.isNull(this.receiverId) || ObjectUtil.isEmpty(this.receiverId.getRawID())) ? null : this.receiverId.toHex();
    }

    public CrossChainDomain getSenderDomain() {
        return crossChainChannel.getSenderDomain();
    }

    public CrossChainDomain getReceiverDomain() {
        return crossChainChannel.getReceiverDomain();
    }

    public String getLaneKey() {
        return StrUtil.builder()
                .append(this.crossChainChannel.getSenderDomain().getDomain()).append("@")
                .append(ObjectUtil.isNull(this.getSenderId()) || ObjectUtil.isEmpty(this.getSenderId().getRawID()) ? "" : this.getSenderId().toHex()).append("@")
                .append(ObjectUtil.isNull(this.getReceiverDomain()) || StrUtil.isEmpty(this.getReceiverDomain().getDomain()) ? "" : this.getReceiverDomain().getDomain()).append("@")
                .append(ObjectUtil.isNull(this.getReceiverId()) || ObjectUtil.isEmpty(this.getReceiverId().getRawID()) ? "" : this.getReceiverId().toHex())
                .toString();
    }

    public boolean isValidated() {
        boolean basic = ObjectUtil.isNotNull(this.crossChainChannel) && ObjectUtil.isNotNull(this.crossChainChannel.getSenderDomain());
        if (!basic) {
            return false;
        }

        if (ObjectUtil.isNull(this.crossChainChannel.getReceiverDomain()) || ObjectUtil.isEmpty(this.crossChainChannel.getReceiverDomain().getDomain())) {
            return ObjectUtil.isAllEmpty(this.receiverId, this.senderId);
        }

        return (ObjectUtil.isAllNotEmpty(this.receiverId, this.senderId) && ObjectUtil.isAllNotEmpty(this.receiverId.getRawID(), this.senderId.getRawID()))
                || ObjectUtil.isAllEmpty(this.receiverId, this.senderId);
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    public boolean contains(CrossChainLane lane) {
        return (ObjectUtil.equals(this.getSenderDomain(), lane.getSenderDomain()) && ObjectUtil.isAllEmpty(this.getReceiverDomain(), this.getSenderId(), this.getReceiverId()))
                || (ObjectUtil.equals(this.getSenderDomain(), lane.getSenderDomain()) && ObjectUtil.equals(this.getReceiverDomain(), lane.getReceiverDomain()) && ObjectUtil.isAllEmpty(this.getSenderId(), this.getReceiverId()))
                || (this.equals(lane));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CrossChainLane)) {
            return false;
        }
        CrossChainLane lane = (CrossChainLane) obj;
        return ObjectUtil.equals(this.getSenderDomain(), lane.getSenderDomain())
                && ObjectUtil.equals(this.getReceiverDomain(), lane.getReceiverDomain())
                && ObjectUtil.equals(this.getSenderId(), lane.getSenderId())
                && ObjectUtil.equals(this.getReceiverId(), lane.getReceiverId());
    }
}
