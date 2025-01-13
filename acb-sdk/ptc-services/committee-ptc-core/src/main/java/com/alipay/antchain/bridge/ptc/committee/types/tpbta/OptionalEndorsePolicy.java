package com.alipay.antchain.bridge.ptc.committee.types.tpbta;

import java.io.IOException;
import java.lang.reflect.Type;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVMapping;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OptionalEndorsePolicy {

    private static final short TAG_THRESHOLD = 0x00;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Threshold {
        private static final short TAG_OPERATOR = 0x00;
        private static final short TAG_THRESHOLD_NUM = 0x01;

        @TLVField(tag = TAG_OPERATOR, type = TLVTypeEnum.STRING)
        private OperatorEnum operator;

        @TLVField(tag = TAG_THRESHOLD_NUM, type = TLVTypeEnum.UINT32, order = TAG_THRESHOLD_NUM)
        private int threshold;

        public boolean check(int n) {
            return operator.check(n, threshold);
        }
    }

    public static class ThresholdSerializer implements ObjectSerializer {

        @Override
        public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
            Threshold t = (Threshold) object;
            serializer.write(t.getOperator().symbol + t.getThreshold());
        }
    }

    public static class ThresholdDeserializer implements ObjectDeserializer {

        @Override
        public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            String val = parser.parseObject(String.class);
            OperatorEnum operatorEnum = OperatorEnum.fromThreshold(val);
            return (T) new Threshold(operatorEnum, Integer.parseInt(StrUtil.removePrefix(val, operatorEnum.getSymbol())));
        }

        @Override
        public int getFastMatchToken() {
            return 0;
        }
    }

    @Getter
    @TLVMapping(fieldName = "symbol")
    public enum OperatorEnum {

        EQUALS("=="),
        NOT_EQUALS("!="),
        LESS_THAN("<"),
        GREATER_THAN(">"),
        GREATER_OR_EQUALS(">="),
        LESS_OR_EQUALS("<=");

        @TLVCreator
        public static OperatorEnum fromThreshold(String rawThreshold) {
            String op = rawThreshold;
            if (rawThreshold.length() > 1) {
                if (NumberUtil.isNumber(StrUtil.sub(rawThreshold, 1, rawThreshold.length()))) {
                    op = StrUtil.sub(rawThreshold, 0, 1);
                } else {
                    op = StrUtil.sub(rawThreshold, 0, 2);
                }
            }
            for (OperatorEnum e : values()) {
                if (StrUtil.equals(op, e.getSymbol())) {
                    return e;
                }
            }
            throw new IllegalArgumentException();
        }

        private final String symbol;

        OperatorEnum(String symbol) {
            this.symbol = symbol;
        }

        public boolean check(int n, int threshold) {
            switch (this) {
                case EQUALS:
                    return n == threshold;
                case NOT_EQUALS:
                    return n != threshold;
                case LESS_THAN:
                    return n < threshold;
                case GREATER_THAN:
                    return n > threshold;
                case GREATER_OR_EQUALS:
                    return n >= threshold;
                case LESS_OR_EQUALS:
                    return n <= threshold;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    @JSONField(name = "threshold", serializeUsing = ThresholdSerializer.class, deserializeUsing = ThresholdDeserializer.class)
    @TLVField(tag = TAG_THRESHOLD, type = TLVTypeEnum.BYTES, order = TAG_THRESHOLD)
    private Threshold threshold;
}
