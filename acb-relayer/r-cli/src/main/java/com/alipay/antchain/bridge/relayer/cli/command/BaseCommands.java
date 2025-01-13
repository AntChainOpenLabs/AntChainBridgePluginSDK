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

package com.alipay.antchain.bridge.relayer.cli.command;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.util.Arrays;

import cn.hutool.core.util.*;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellMethodAvailability;
import sun.security.x509.AlgorithmId;

public abstract class BaseCommands {

    public abstract String name();

    public abstract GrpcClient getGrpcClient();

    public String queryAPI(String command, String... args) {
        AdminRequest.Builder reqBuilder = AdminRequest.newBuilder();
        reqBuilder.setCommandNamespace(name());
        reqBuilder.setCommand(command);
        if (null != args) {
            reqBuilder.addAllArgs(Lists.newArrayList(args));
        }
        try {
            AdminResponse response = getGrpcClient().adminRequest(reqBuilder.build());
            if (response.getSuccess()) {
                return response.getResult();
            } else {
                return response.getErrorMsg();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ShellMethodAvailability
    public Availability baseAvailability() {
        if (!getGrpcClient().checkServerStatus()) {
            return Availability.unavailable(
                    StrUtil.format("Relayer admin server {}:{} is unreachable",
                            getGrpcClient().getHost(), getGrpcClient().getPort())
            );
        }
        return Availability.available();
    }

    public CrossChainIdentity getIdentity(String identity) {
        if (NumberUtil.isOdd(identity.length()) || !HexUtil.isHexNumber(identity)) {
            throw new RuntimeException("Invalid identity , must be hex format: " + identity);
        } else {
            byte[] rawId = HexUtil.decodeHex(StrUtil.removePrefix(identity, "0x"));
            if (ObjectUtil.isNull(rawId) || rawId.length > 32) {
                throw new RuntimeException("Invalid identity over 32B: " + identity);
            } else if (rawId.length < 32) {
                byte[] data = new byte[32 - rawId.length];
                Arrays.fill(data, (byte) 0);
                return new CrossChainIdentity(ArrayUtil.addAll(data, rawId));
            }
            return new CrossChainIdentity(rawId);
        }
    }

    public byte[] getAmId(String identity) {
        if (NumberUtil.isOdd(identity.length()) || !HexUtil.isHexNumber(identity)) {
            return identity.getBytes();
        } else {
            byte[] rawId = HexUtil.decodeHex(StrUtil.removePrefix(identity, "0x"));
            if (ObjectUtil.isNull(rawId) || rawId.length > 32) {
                throw new RuntimeException("Invalid identity over 32B: " + identity);
            } else if (rawId.length < 32) {
                byte[] data = new byte[32 - rawId.length];
                Arrays.fill(data, (byte) 0);
                return ArrayUtil.addAll(data, rawId);
            }
            return rawId;
        }
    }

    @SneakyThrows
    public PublicKey readPublicKeyFromPem(byte[] publicKeyPem) {
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(publicKeyPem)));
        return KeyUtil.generatePublicKey(
                AlgorithmId.get(keyInfo.getAlgorithm().getAlgorithm().getId()).getName(),
                keyInfo.getEncoded()
        );
    }
}
