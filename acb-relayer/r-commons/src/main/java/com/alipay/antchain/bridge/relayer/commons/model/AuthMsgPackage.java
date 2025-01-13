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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

@Getter
@Setter
public class AuthMsgPackage {

    public static int UNORDERED_BIT_MASK = 0x00000001;
    public static byte UNORDERED_MSG = 1;
    public static byte ORDERED_MSG = 0;

    public static int TRUSTED_BIT_MASK = 0x00000002;
    public static byte TRUSTED_FLAG = 1;
    public static byte UNTRUSTED_FLAG = 0;

    public static int NOTARY_BIT_MASK = 0x00000004;
    public static byte NOTARY_FLAG = 1;
    public static byte NON_NOTARY_FLAG = 0;

    public static int FLAGS_LEN = 4;

    public static int RECEIVER_IDENTITY_BYTEARRAY_LEN = 32;
    public static int SENDER_DOMAIN_LEN = 128 + 1; // 1 byte for len ; 128 bytes for domain

    public static AuthMsgPackage convertFrom(List<SDPMsgWrapper> sdpMsgWrappers, List<PTCProofResult> ptcProofResults) {
        AuthMsgPackage authMsgPackage = new AuthMsgPackage();
        if (sdpMsgWrappers.size() != ptcProofResults.size()) {
            throw new RuntimeException("sdpMsgWrappers and ptcProofResult size not match");
        }

        for (int i = 0; i < sdpMsgWrappers.size(); i++) {
            authMsgPackage.addAmMsg(
                    Base64.getEncoder().encodeToString(
                            ObjectUtil.isNull(ptcProofResults.get(i)) ?
                                    PTCProofResult.generateEmptyProofForAM( // for now, we generate empty proof until PTC is ready
                                            sdpMsgWrappers.get(i).getAuthMsgWrapper()
                                    ).getPtcProof()
                                    : ptcProofResults.get(i).getPtcProof()
                    ),
                    sdpMsgWrappers.get(i).getAuthMsgWrapper()
                            .getLedgerInfo()
                            .getOrDefault(AuthMsgWrapper.AM_HINTS, StrUtil.EMPTY),
                    sdpMsgWrappers.get(i).getAuthMsgWrapper().getAuthMessage().encode()
            );
        }

        authMsgPackage.setSdpMsgWrapper(sdpMsgWrappers.get(0));

        return authMsgPackage;
    }

    private int flags = 0;

    private SDPMsgWrapper sdpMsgWrapper;

    private List<String> proofs;
    private List<String> hints;
    private List<byte[]> amPkgs;

    public AuthMsgPackage() {
        this.proofs = ListUtil.toList();
        this.hints = ListUtil.toList();
        this.amPkgs = ListUtil.toList();
    }

    public void addAmMsg(String proof, String hint, byte[] amPkg) {
        this.proofs.add(proof);
        this.hints.add(hint);
        this.amPkgs.add(amPkg);
    }

    public void setUnordered(byte flag) {
        this.flags = (flag == UNORDERED_MSG) ?
                this.flags | UNORDERED_BIT_MASK :
                this.flags & (~UNORDERED_BIT_MASK);
    }

    public byte getUnordered() {
        return (this.flags & UNORDERED_BIT_MASK) == 0 ? ORDERED_MSG : UNORDERED_MSG;
    }

    public byte getTrusted() {
        return (this.flags & TRUSTED_BIT_MASK) == 0 ? UNTRUSTED_FLAG : TRUSTED_FLAG;
    }

    public void setTrusted(byte flag) {
        this.flags = (flag == TRUSTED_FLAG) ?
                this.flags | TRUSTED_BIT_MASK :
                this.flags & (~TRUSTED_BIT_MASK);
    }

    public byte getNotary() {
        return (this.flags & NOTARY_BIT_MASK) == 0 ? NON_NOTARY_FLAG : NOTARY_FLAG;
    }

    public void setNotary(byte flag) {
        this.flags = (flag == NOTARY_FLAG) ?
                this.flags | NOTARY_BIT_MASK :
                this.flags & (~NOTARY_BIT_MASK);
    }

    // return byte[] with format: {identity, [proof], [proof]}
    //
    // identity: 32 byte
    // proof:
    // - <hint_len>(32) 4bytes
    // - <hint>
    // - <proof_len>(32) 4bytes
    // - <udagProof>
    //
    public byte[] encode() throws Exception {
        if (this.proofs.size() == 0 || this.hints.size() == 0) {
            return null;
        }
        if (this.proofs.size() != this.hints.size()) {
            return null;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // 标志
        ByteBuffer dbuf = ByteBuffer.allocate(FLAGS_LEN);
        dbuf.putInt(this.flags);
        stream.write(dbuf.array());

        byte[] rawReceiver = HexUtil.decodeHex(sdpMsgWrapper.getMsgReceiver());
        // 增加接受方identity
        if (rawReceiver.length == RECEIVER_IDENTITY_BYTEARRAY_LEN) {
            try {
                stream.write(rawReceiver);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 填充发送方域名
        byte sender_domain_len = (byte) sdpMsgWrapper.getSenderBlockchainDomain().length();
        ByteBuffer dbuf2 = ByteBuffer.allocate(SENDER_DOMAIN_LEN);
        dbuf2.put(sender_domain_len);
        dbuf2.put(sdpMsgWrapper.getSenderBlockchainDomain().getBytes());
        stream.write(dbuf2.array());

        for (int i = 0; i < this.hints.size(); i++) {
            String hint = this.hints.get(i);
            String proof = this.proofs.get(i);

            // Encode hint into pkg
            byte[] hintBytes = hint.getBytes();
            int hintLen = hintBytes.length;
            stream.write((hintLen >>> 24) & 0xFF);
            stream.write((hintLen >>> 16) & 0xFF);
            stream.write((hintLen >>> 8) & 0xFF);
            stream.write((hintLen) & 0xFF);
            try {
                stream.write(hintBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Encode proof into pkg
            byte[] proofBytes = Base64.getDecoder().decode(proof); // proof: base64 decode before encode into pkg
            int proofLen = proofBytes.length;
            stream.write((proofLen >>> 24) & 0xFF);
            stream.write((proofLen >>> 16) & 0xFF);
            stream.write((proofLen >>> 8) & 0xFF);
            stream.write((proofLen) & 0xFF);

            try {
                stream.write(proofBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Encode am pkt
            if (this.getNotary() == NOTARY_FLAG) {
                byte[] amPkg = this.amPkgs.get(i);

                // Encode proof into pkg
                int amPkg_len = amPkg.length;
                stream.write((amPkg_len >>> 24) & 0xFF);
                stream.write((amPkg_len >>> 16) & 0xFF);
                stream.write((amPkg_len >>> 8) & 0xFF);
                stream.write((amPkg_len) & 0xFF);

                try {
                    stream.write(amPkg);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return stream.toByteArray();
    }

    @SneakyThrows
    public byte[] extractProofs() {
        byte[] encodedAmMsgPkg = this.encode();
        int offset = RECEIVER_IDENTITY_BYTEARRAY_LEN + SENDER_DOMAIN_LEN + FLAGS_LEN;
        int proofs_len = encodedAmMsgPkg.length - offset;
        byte[] proofs = new byte[proofs_len];
        System.arraycopy(encodedAmMsgPkg, offset, proofs, 0, proofs_len);
        return proofs;
    }

    public static byte[] extractReceiverIdentity(byte[] encodedAuthMsgPackage) {
        int offset = FLAGS_LEN;
        byte[] identity = new byte[RECEIVER_IDENTITY_BYTEARRAY_LEN];
        System.arraycopy(encodedAuthMsgPackage, offset, identity, 0, RECEIVER_IDENTITY_BYTEARRAY_LEN);
        return identity;
    }

    public static String extractSenderDomain(byte[] encodedAuthMsgPackage) {
        int offset = RECEIVER_IDENTITY_BYTEARRAY_LEN + FLAGS_LEN;
        byte[] domain = new byte[SENDER_DOMAIN_LEN - 1];
        byte domain_len = encodedAuthMsgPackage[offset];
        System.arraycopy(encodedAuthMsgPackage, offset + 1, domain, 0, domain_len);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < domain_len; i++) {
            out.append((char) (domain[i]));
        }
        return out.toString();
    }


    public static int extractFlags(byte[] encodedAuthMsgPackage) {
        int offset = 0;
        byte[] flags = new byte[FLAGS_LEN];
        System.arraycopy(encodedAuthMsgPackage, offset, flags, 0, FLAGS_LEN);
        ByteBuffer wrapped = ByteBuffer.wrap(flags);
        return wrapped.getInt();
    }

    public static byte extractUnorderedFlag(byte[] encodedAuthMsgPackage) {
        int flags = extractFlags(encodedAuthMsgPackage);
        return (flags & UNORDERED_BIT_MASK) == 0 ? ORDERED_MSG : UNORDERED_MSG;
    }


    public static byte extractTrustedFlag(byte[] encodedAuthMsgPackage) {
        int flags = extractFlags(encodedAuthMsgPackage);
        return (flags & TRUSTED_BIT_MASK) == 0 ? UNTRUSTED_FLAG : TRUSTED_FLAG;
    }

    public static byte extractNotaryFlag(byte[] encodedAuthMsgPackage) {
        int flags = extractFlags(encodedAuthMsgPackage);
        return (flags & NOTARY_BIT_MASK) == 0 ? NON_NOTARY_FLAG : NOTARY_FLAG;
    }

    static private int extractUint32(ByteArrayInputStream in) {
        int i = in.read();
        i = (i << 8) + in.read();
        i = (i << 8) + in.read();
        i = (i << 8) + in.read();
        return i;
    }

    static private String extractString(ByteArrayInputStream in, int len) {
        String s = "";
        while (len > 0) {
            s = s + (char) (in.read());
            len--;
        }
        return s;
    }

    static private byte[] extractBytes(ByteArrayInputStream in, int len) {
        byte[] s = new byte[len];
        in.read(s, 0, len);
        return s;
    }
}
