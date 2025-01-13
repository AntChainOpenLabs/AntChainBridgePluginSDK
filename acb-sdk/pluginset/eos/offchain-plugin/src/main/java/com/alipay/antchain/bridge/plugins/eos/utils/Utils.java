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

package com.alipay.antchain.bridge.plugins.eos.utils;

import java.math.BigInteger;

import cn.hutool.core.util.StrUtil;

public class Utils {

    private static final String EOS_BASE32_ALPHABET = ".12345abcdefghijklmnopqrstuvwxyz";

    public static boolean isEosBase32Name(String name) {
        if (StrUtil.isEmpty(name)) {
            return false;
        }
        if (name.length() > 12) {
            return false;
        }
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            if (-1 == EOS_BASE32_ALPHABET.indexOf(c)) {
                return false;
            }
        }
        return true;
    }

    public static BigInteger convertEosBase32NameToNum(String b32Name) {
        BigInteger value = new BigInteger("0");

        for (int i = 0; i < b32Name.length(); ++i) {
            value = value.add(BigInteger.valueOf(EOS_BASE32_ALPHABET.indexOf(b32Name.charAt(i))).shiftLeft(64 - (i + 1) * 5));
        }

        return value;
    }

}
