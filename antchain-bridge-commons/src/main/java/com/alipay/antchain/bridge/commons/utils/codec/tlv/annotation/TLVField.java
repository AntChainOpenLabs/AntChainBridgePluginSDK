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

package com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;

/**
 * Annotation {@code TLVField} is used to mark a field of
 * a class that needs to be serialized and deserialized by TLV.
 *
 * <p>
 *     When the fields of the class is marked by {@code TLVField},
 *     then we can use the {@link com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils}
 *     to encode or decode the object of this class.
 * </p>
 * <p>
 *     An example is given below, all fields of class {@code MyClass}
 *     has been annotated with {@code TLVField}. And it indicates
 *     the tag, type and serialization order of these fields.
 * </p>
 * <pre>
 * public class MyClass {
 *
 *     &#64;TLVField(tag = 0, type = TLVTypeEnum.STRING, order = 0)
 *     private String myString;
 *
 *     &#64;TLVField(tag = 1, type = TLVTypeEnum.BYTES, order = 1)
 *     private byte[] myBytes;
 *
 *     &#64;TLVField(tag = 2, type = TLVTypeEnum.UINT8, order = 2)
 *     private byte myByte;
 *
 *     &#64;TLVField(tag = 3, type = TLVTypeEnum.UINT16, order = 3)
 *     private short myShort;
 *
 *     &#64;TLVField(tag = 4, type = TLVTypeEnum.UINT32, order = 4)
 *     private int myInt;
 *
 *     &#64;TLVField(tag = 5, type = TLVTypeEnum.UINT64, order = 5)
 *     private long myLong;
 *
 *     &#64;TLVField(tag = 6, type = TLVTypeEnum.BYTES_ARRAY, order = 6)
 *     private List&#60;byte[]&#62; myBytesArray;
 *
 *     &#64;TLVField(tag = 7, type = TLVTypeEnum.STRING_ARRAY, order = 7)
 *     private List&#60;String&#62; myStringArray;
 *
 *     &#64;TLVField(tag = 8, type = TLVTypeEnum.MAP, order = 8)
 *     private Map&#60;MapKeyClz, MapValueClz&#62; myMap;
 * }
 *
 * &#64;TLVMapping(fieldName = "key")
 * public class MapKeyClz {
 *     private String key;
 * }
 *
 * public class MapValueClz {
 *     &#64;TLVField(tag = 0, type = TLVTypeEnum.STRING, order = 0)
 *     private String value;
 * }
 * </pre>
 * <p>
 *     Following the example below, you can decode and encode the object of {@code MyClass}.
 *     There is more methods in the class {@link com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils},
 *     please check it.
 * </p>
 * <p>
 *     It is particularly important to emphasize that the key and value classes of the {@link java.util.Map}
 *     type field supposed to be wrapped primitive types, or their fields annotated with {@link TLVField},
 *     or they are annotated with {@link TLVMapping} annotation.
 * </p>
 * <pre>
 * public void main() {
 *     // assume that we have some raw data.
 *     byte[] rawData = new byte[];
 *
 *     // decode here.
 *     MyClass obj = TLVUtils.decode(rawData, MyClass.class);
 *
 *     // encode here.
 *     rawData = TLVUtils.encode(obj);
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface TLVField {

    /**
     * In the design of {@code TLV}, every field to be encoded
     * or decoded in {@code TLV} requires to have a <b>tag</b>.
     * This allows the code to map bytes to fields. And evey tag
     * should be unique in the scope of the class.
     *
     * @return the number represents the tag.
     */
    short tag();

    /**
     * In the design of {@code TLV}, the type of every field maps
     * itself to an enum value in {@link TLVTypeEnum} and they are one-to-one.
     *
     * @return the enum value mapping to the field type.
     */
    TLVTypeEnum type();

    /**
     * In the design of {@code TLV}, there is orders between
     * the fields need to encode. And {@link com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils}
     * would encode the fields into bytes by the order set here.
     *
     * @return the order set to encode the fields.
     */
    int order() default 0;
}
