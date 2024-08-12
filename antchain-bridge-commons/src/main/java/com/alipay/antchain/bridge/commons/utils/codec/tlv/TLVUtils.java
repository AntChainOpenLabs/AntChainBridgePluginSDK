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

package com.alipay.antchain.bridge.commons.utils.codec.tlv;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVCreator;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVMapping;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

@SuppressWarnings("all")
public class TLVUtils {

    public static String encodeToHex(Object obj) {
        return HexUtil.encodeHexStr(encode(obj));
    }

    public static byte[] encode(Object obj) {
        return encode(obj, Integer.MAX_VALUE);
    }

    public static byte[] encode(Object obj, int maxOrder) {
        return toTLVPacket(obj, maxOrder, null).encode();
    }

    public static byte[] encode(Object obj, List<Integer> requiredOrderList) {
        if (ObjectUtil.isEmpty(requiredOrderList)) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                    "empty required orders"
            );
        }
        return toTLVPacket(obj, Integer.MAX_VALUE, requiredOrderList).encode();
    }

    public static TLVPacket toTLVPacket(Object obj) {
        return toTLVPacket(obj, Integer.MAX_VALUE, null);
    }

    private static TLVPacket toTLVPacket(Object obj, int maxOrder, List<Integer> requiredOrderList) {
        if (ObjectUtil.isEmpty(obj)) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                    "empty object"
            );
        }

        List<Field> fieldList = getAnnotatedFields(obj.getClass());
        if (ObjectUtil.isEmpty(fieldList)) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                    String.format("no fields of class %s annotated by TLVField", obj.getClass().getName())
            );
        }

        List<TLVItem> items = new ArrayList<>();
        try {
            for (Field field : orderFields(fieldList, maxOrder, requiredOrderList)) {
                field.setAccessible(true);
                if (ObjectUtil.isNull(field.get(obj))) {
                    continue;
                }
                TLVField tlvField = field.getAnnotation(TLVField.class);
                if (
                        field.getType().isPrimitive()
                                || field.getType().isArray()
                                || String.class.isAssignableFrom(field.getType())
                                || "java.util.List<byte[]>".equalsIgnoreCase(field.getGenericType().getTypeName())
                                || "java.util.List<java.lang.String>".equalsIgnoreCase(field.getGenericType().getTypeName())
                ) {
                    items.add(getItem(tlvField.type(), tlvField.tag(), field.get(obj)));
                } else if (field.getType().isEnum() && tlvField.type() == TLVTypeEnum.UINT8) {
                    Method getValueM = field.getType().getMethod("ordinal");
                    if (ObjectUtil.isEmpty(getValueM)) {
                        throw new AntChainBridgeCommonsException(
                                CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                                String.format("enum field %s doesn't have method `original()`", field.getName())
                        );
                    }
                    int ordinal = (int) getValueM.invoke(field.get(obj));
                    items.add(getItem(tlvField.type(), tlvField.tag(), ordinal));
                } else if (field.getType() == Map.class) {
                    if (tlvField.type() != TLVTypeEnum.MAP) {
                        throw new AntChainBridgeCommonsException(
                                CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                                StrUtil.format(
                                        "map field {}@{} must be annotated with type MAP on TLVField.type",
                                        field.getClass().getTypeName(), field.getName()
                                )
                        );
                    }
                    items.add(getItem(tlvField.type(), tlvField.tag(), field.get(obj)));
                } else {
                    Object value = field.get(obj);
                    Class fieldClz = value.getClass();
                    TLVMapping mapping = (TLVMapping) fieldClz.getAnnotation(TLVMapping.class);
                    if (ObjectUtil.isEmpty(mapping)) {
                        if (tlvField.type() == TLVTypeEnum.BYTES) {
                            items.add(getItem(tlvField.type(), tlvField.tag(), encode(value)));
                        } else if (tlvField.type() == TLVTypeEnum.BYTES_ARRAY && List.class.isAssignableFrom(fieldClz)) {
                            List listVal = (List) value;
                            List<byte[]> listBytesVal = new ArrayList<>();
                            for (Object o : listVal) {
                                listBytesVal.add(encode(o));
                            }
                            items.add(getItem(tlvField.type(), tlvField.tag(), listBytesVal));
                        } else {
                            throw new AntChainBridgeCommonsException(
                                    CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                                    String.format(
                                            "field class %s has no annotation @TLVMapping must be TLV type BYTES or BYTES_ARRAY",
                                            fieldClz.getName()
                                    )
                            );
                        }

                    } else {
                        items.add(getItem(tlvField.type(), tlvField.tag(), getFieldInMapping(fieldClz, mapping).get(value)));
                    }
                }
            }
        } catch (Exception e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                    "failed to generate tlv item list",
                    e
            );
        }

        return new TLVPacket((short) 0x00, items);
    }

    protected static Field getFieldInMapping(Class clz, TLVMapping mapping) {
        Field fieldInMapping = null;
        for (Field declaredField : clz.getDeclaredFields()) {
            if (StrUtil.equals(declaredField.getName(), mapping.fieldName())) {
                fieldInMapping = declaredField;
                break;
            }
        }
        if (ObjectUtil.isEmpty(fieldInMapping)) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                    String.format("field class %s has no field %s mentioned in @TLVMapping",
                            clz.getName(), mapping.fieldName()));
        }
        fieldInMapping.setAccessible(true);

        return fieldInMapping;
    }

    private static List<Field> orderFields(List<Field> fields, int maxOrder, List<Integer> requiredOrderList) {

        if (ObjectUtil.isNotEmpty(requiredOrderList)) {
            if (requiredOrderList.stream().distinct().count() != requiredOrderList.size()) {
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                        "requiredOrderList expects unique elements: " + Arrays.toString(requiredOrderList.toArray())
                );
            }
            List<Field> fieldList = new ArrayList<>();
            for (Integer order : requiredOrderList) {
                fieldList.add(
                        fields.stream()
                                .filter(field -> field.getAnnotation(TLVField.class).order() == order)
                                .findFirst()
                                .orElseThrow(
                                        () -> new AntChainBridgeCommonsException(
                                                CommonsErrorCodeEnum.CODEC_TLV_ENCODE_ERROR,
                                                String.format("none order %d found in fields", order)
                                        )
                                )
                );
            }
            return fieldList;
        }

        return fields.stream()
                .sorted(
                        (o1, o2) -> {
                            TLVField tlvField1 = o1.getAnnotation(TLVField.class);
                            TLVField tlvField2 = o2.getAnnotation(TLVField.class);
                            if (tlvField1.order() < tlvField2.order()) {
                                return -1;
                            } else if (tlvField1.order() == tlvField2.order()) {
                                return 0;
                            }
                            return 1;
                        }
                ).filter(
                        field -> field.getAnnotation(TLVField.class).order() <= maxOrder
                ).collect(Collectors.toList());
    }

    private static List<Field> getAnnotatedFields(Class clz) {
        return fillFields(ListUtil.toList(), clz).stream()
                .filter(field -> ObjectUtil.isNotEmpty(field.getAnnotation(TLVField.class)))
                .collect(Collectors.toList());
    }

    private static List<Field> fillFields(List<Field> fields, Class clz) {
        if (ObjectUtil.isNull(clz) || clz.getName().equalsIgnoreCase("java.lang.object")) {
            return fields;
        }
        fields.addAll(ListUtil.of(clz.getDeclaredFields()));
        return fillFields(fields, clz.getSuperclass());
    }

    public static <T> T decodeFromHex(String hex, Class<T> clz) {
        return decode(HexUtil.decodeHex(hex), clz);
    }

    public static <T> T decode(byte[] raw, Class<T> clz) {
        TLVPacket packet = TLVPacket.decode(raw);
        try {
            T obj = clz.newInstance();

            List<Field> fieldList = getAnnotatedFields(clz);
            if (ObjectUtil.isEmpty(fieldList)) {
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.CODEC_TLV_DECODE_ERROR,
                        String.format("no fields of class %s annotated by TLVField", clz.getName())
                );
            }

            for (Field field : fieldList) {
                TLVField tlvField = field.getAnnotation(TLVField.class);
                TLVItem tlvItem = packet.getItemForTag(tlvField.tag());
                if (ObjectUtil.isNotEmpty(tlvItem)) {
                    field.setAccessible(true);
                    Object val = parseValueFromItem(
                            tlvItem,
                            tlvField.type(),
                            field.getGenericType()
                    );
                    if (
                            field.getType().isPrimitive()
                                    || field.getType().isArray()
                                    || String.class.isAssignableFrom(field.getType())
                                    || "java.util.List<byte[]>".equalsIgnoreCase(field.getGenericType().getTypeName())
                                    || "java.util.List<java.lang.String>".equalsIgnoreCase(field.getGenericType().getTypeName())
                    ) {
                        field.set(obj, val);
                    } else if (field.getType().isEnum()) {
                        field.set(obj, objectFromCreator(field, val));
                    } else if (field.getType() == Map.class) {
                        field.set(obj, val);
                    } else {
                        Class fieldClz = field.getType();
                        TLVMapping mapping = (TLVMapping) fieldClz.getAnnotation(TLVMapping.class);
                        if (ObjectUtil.isEmpty(mapping)) {
                            if (StrUtil.equals(byte[].class.getTypeName(), val.getClass().getTypeName())) {
                                field.set(obj, decode((byte[]) val, fieldClz));
                            } else if (List.class.isAssignableFrom(val.getClass())) {
                                Class elementClz = Class.forName(((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0].getTypeName());
                                List<Object> fieldVal = new ArrayList<>();
                                for (Object o : (List) val) {
                                    fieldVal.add(decode((byte[]) o, elementClz));
                                }
                                field.set(obj, fieldVal);
                            } else {
                                throw new AntChainBridgeCommonsException(
                                        CommonsErrorCodeEnum.CODEC_TLV_DECODE_ERROR,
                                        String.format("expect value with type byte[] or List<byte[]> but get %s",
                                                val.getClass().getTypeName())
                                );
                            }
                        } else {
                            Field fieldInMapping = getFieldInMapping(fieldClz, mapping);
                            Object fieldObj = fieldClz.newInstance();
                            fieldInMapping.set(fieldObj, val);
                            field.set(obj, fieldObj);
                        }
                    }
                }
            }
            return obj;

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException |
                 ClassNotFoundException e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.CODEC_TLV_DECODE_ERROR,
                    "unexpected exception while decoding TLV bytes. ",
                    e
            );
        }
    }

    private static Object objectFromCreator(Field field, Object val) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        for (Method method : field.getType().getMethods()) {
            TLVCreator tlvCreator = method.getAnnotation(TLVCreator.class);
            if (ObjectUtil.isNull(tlvCreator)) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (!method.getParameterTypes()[0].isAssignableFrom(val.getClass())) {
                continue;
            }
            method.setAccessible(true);
            return method.invoke(null, val);
        }

        // Deprecated :
        // TLVField requires that enum class has static method
        // `valueOf` with type `val.getClass()` parameter
        return field.getType().getMethod("valueOf", val.getClass()).invoke(null, val);
    }

    private static Object parseValueFromItem(TLVItem item, TLVTypeEnum type, Type fieldGenericType) {
        switch (type) {
            case UINT8:
                return item.getUint8Value();
            case UINT16:
                return item.getUint16Value();
            case UINT32:
                return item.getUint32Value();
            case UINT64:
                return item.getUint64Value();
            case STRING:
                return item.getUtf8String();
            case BYTES:
                return item.getValue();
            case BYTES_ARRAY:
                return item.getBytesArray();
            case STRING_ARRAY:
                return item.getStringArray();
            case MAP:
                Type[] kvTypes = ((ParameterizedTypeImpl) fieldGenericType).getActualTypeArguments();
                if (ObjectUtil.isEmpty(kvTypes) || kvTypes.length != 2) {
                    throw new RuntimeException("incorrect kv types for MAP");
                }
                return item.getMap((Class) kvTypes[0], (Class) kvTypes[1]);
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.CODEC_TLV_DECODE_ERROR,
                        "type of item not support: " + type
                );
        }
    }

    private static TLVItem getItem(TLVTypeEnum type, short tag, Object value) {
        TLVItem item;
        switch (type) {
            case UINT8:
                if (value instanceof Integer) {
                    item = TLVItem.fromUint8(tag, ByteUtil.intToByte((Integer) value));
                } else {
                    item = TLVItem.fromUint8(tag, (Byte) value);
                }
                break;
            case UINT16:
                item = TLVItem.fromUint16(tag, (short) value);
                break;
            case UINT32:
                item = TLVItem.fromUint32(tag, (int) value);
                break;
            case UINT64:
                item = TLVItem.fromUint64(tag, (long) value);
                break;
            case STRING:
                item = TLVItem.fromUTF8String(tag, (String) value);
                break;
            case BYTES:
                item = TLVItem.fromBytes(tag, (byte[]) value);
                break;
            case BYTES_ARRAY:
                item = TLVItem.fromBytesArray(tag, (List<byte[]>) value);
                break;
            case STRING_ARRAY:
                item = TLVItem.fromStringArray(tag, (List<String>) value);
                break;
            case MAP:
                item = TLVItem.fromMap(tag, (Map) value);
                break;
            default:
                throw new AntChainBridgeCommonsException(
                        CommonsErrorCodeEnum.CODEC_TLV_UNSUPPORTED_TYPE,
                        "type of item not support: " + type
                );
        }

        return item;
    }
}
