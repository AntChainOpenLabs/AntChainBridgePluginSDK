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

package com.alipay.antchain.bridge.relayer.commons.utils;

import java.lang.reflect.Type;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.deserializer.JavaObjectDeserializer;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;

public class HeteroBBCContextDeserializer extends JavaObjectDeserializer {
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        if (parser.getLexer().token() == JSONToken.LITERAL_STRING) {
            type = String.class;
            String contextJson = super.deserialze(parser, type, fieldName);
            AbstractBBCContext bbcContext = new DefaultBBCContext();
            bbcContext.decodeFromBytes(contextJson.getBytes());
            return (T) bbcContext;
        }
        return super.deserialze(parser, type, fieldName);
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
