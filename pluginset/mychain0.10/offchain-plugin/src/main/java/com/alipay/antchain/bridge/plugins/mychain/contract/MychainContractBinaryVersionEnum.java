package com.alipay.antchain.bridge.plugins.mychain.contract;

import lombok.Getter;

@Getter
public enum MychainContractBinaryVersionEnum {

    V1_4_0(
            "v1.4.0",
            "/contract/1.4.0_ALPHA/auth_message.wasc", "/contract/1.1.0_BETA/am_client_mychain010_0_0_1.bin", "/contract/1.4.0_ALPHA/auth_message_tee.wasc",
            "/contract/1.4.0_ALPHA/p2p_message.wasc", "/contract/1.1.0_BETA/am_p2p_msg_mychain010_0_0_1.bin", "/contract/1.4.0_ALPHA/p2p_message.wasc"
    ),

    V1_5_0(
            "v1.5.0",
            "/contract/1.5.0/wasm/auth_message.wasc", "/contract/1.5.0/solidity/am_client_mychain010_0_0_1.bin", "/contract/1.5.0/wasm/auth_message_tee.wasc",
            "/contract/1.5.0/wasm/p2p_message.wasc", "/contract/1.5.0/solidity/am_p2p_msg_mychain010_0_0_1.bin", "/contract/1.5.0/wasm/p2p_message.wasc",
            "/contract/1.5.0/wasm/jit/crosschain_sys.wasc"
    );

    public static MychainContractBinaryVersionEnum selectBinaryByVersion(String version) {
        switch (version.toLowerCase()) {
            case "v1.4.0":
                return V1_4_0;
            case "v1.5.0":
            default:
                return V1_5_0;
        }
    }

    MychainContractBinaryVersionEnum(
            String version,
            String amClientWasm, String amClientEvm, String amClientTeeWasm,
            String sdpWasm, String sdpEvm, String sdpTeeWasm
    ) {
        this(
                version,
                amClientWasm, amClientEvm, amClientTeeWasm,
                sdpWasm, sdpEvm, sdpTeeWasm,
                ""
        );
    }

    MychainContractBinaryVersionEnum(
            String version,
            String amClientWasm, String amClientEvm, String amClientTeeWasm,
            String sdpWasm, String sdpEvm, String sdpTeeWasm,
            String sysContractAllInOneWasm
    ) {
        this.version = version;

        this.amClientWasm = amClientWasm;
        this.amClientEvm = amClientEvm;
        this.amClientTeeWasm = amClientTeeWasm;

        this.sdpWasm = sdpWasm;
        this.sdpEvm = sdpEvm;
        this.sdpTeeWasm = sdpTeeWasm;

        this.sysContractAllInOneWasm = sysContractAllInOneWasm;
    }

    private String version;

    private String amClientWasm;

    private String amClientEvm;

    private String sdpWasm;

    private String sdpEvm;

    private String amClientTeeWasm;

    private String sdpTeeWasm;

    private String sysContractAllInOneWasm;
}
