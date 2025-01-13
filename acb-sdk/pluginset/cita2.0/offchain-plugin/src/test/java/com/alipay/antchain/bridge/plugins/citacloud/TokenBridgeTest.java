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

package com.alipay.antchain.bridge.plugins.citacloud;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.HexUtil;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.cita.cloud.constant.ContractParam;
import com.cita.cloud.response.TransactionReceipt;
import com.citahub.cita.abi.FunctionEncoder;
import com.citahub.cita.abi.TypeReference;
import com.citahub.cita.abi.datatypes.*;
import com.citahub.cita.abi.datatypes.generated.Bytes32;
import com.citahub.cita.abi.datatypes.generated.Uint256;
import com.citahub.cita.utils.Numeric;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TokenBridgeTest {

    private static CITACloudBBCService citaCloudBBCService;

    private static String appContractAddress;

    @BeforeClass
    public static void setUp() throws Exception {

        DefaultBBCContext bbcContext = new DefaultBBCContext();
        bbcContext.setConfForBlockchainClient(FileUtil.readBytes("wjs.json"));

        citaCloudBBCService = new CITACloudBBCService();
        citaCloudBBCService.startup(bbcContext);
    }

    @Test
    public void deployTBContract() {
        ContractParam contractParam = new ContractParam();
        contractParam.setType("address");
        contractParam.setValue("0x3d65e6d2981e06eb04b518fa373fd0f64ad71c5c");
        String address = citaCloudBBCService.getCitaCloudClient().deployContractWithConstructor(
                FileUtil.readString("tbsw.bin", StandardCharsets.UTF_8),
                ListUtil.of(contractParam),
                true
        );

        System.out.println("address is " + address);
    }

    @Test
    public void addRegisteredAsset() {
        String tbAddr = "0x6dfc602b032274cfc238d719bb3262c476d38e47";

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String("digitalasset.sit.test.oracle.chain"));
        inputParameters.add(new Bytes32(HexUtil.decodeHex("9440574887f07322a219612f9fc7f2298b84f23c3e2daa2978bd62a3fd100e36")));
        inputParameters.add(new DynamicArray(new Utf8String("20230814")));
        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                tbAddr,
                "addRegisteredAsset",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());
    }

    @Test
    public void setupTB() {

        String tbAddr = "0x06449b13302022b5d49d6bf43283ddece7867aaf";

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String("openchain.sit.test.oracle.chain"));
        inputParameters.add(new Bytes32(HexUtil.decodeHex("6fbbdb30a91c82e2cfc14aaf8a2ce2f1e119b2b1e833a87d006086e308163639")));
        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                tbAddr,
                "setDomainTokenBridgeAddress",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());

        inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String("digitalasset.sit.test.oracle.chain"));
        inputParameters.add(new Bytes32(HexUtil.decodeHex("6fbbdb30a91c82e2cfc14aaf8a2ce2f1e119b2b1e833a87d006086e308163639")));
        receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                tbAddr,
                "setDomainTokenBridgeAddress",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(Bytes32.class));
        List<Type> result =citaCloudBBCService.getCitaCloudClient().localCallContract(
                tbAddr,
                "token_bridges",
                ListUtil.of(new Utf8String("digitalasset.sit.test.oracle.chain")),
                outputParameters
        );
        byte[] otherTB = ((Bytes32) result.get(0)).getValue();
        System.out.println(HexUtil.encodeHexStr(otherTB));

    }

    @Test
    public void registerRouter() {
        String tbAddr = "0xfa8a30a2dd9a6e483f7b9d2bf7a5e7fe8889b169";

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address("0xc71259f4c71a07ed8d4fb1a26988d29792606409"));
        inputParameters.add(new Utf8String("openchain.sit.test.oracle.chain"));
        inputParameters.add(new Bytes32(HexUtil.decodeHex("9440574887f07322a219612f9fc7f2298b84f23c3e2daa2978bd62a3fd100e36")));
        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                tbAddr,
                "registerRouter",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());

        inputParameters = new ArrayList<>();
        inputParameters.add(new Address("0xc71259f4c71a07ed8d4fb1a26988d29792606409"));
        inputParameters.add(new Utf8String("digitalasset.sit.test.oracle.chain"));
        inputParameters.add(new Bytes32(HexUtil.decodeHex("9440574887f07322a219612f9fc7f2298b84f23c3e2daa2978bd62a3fd100e36")));
        receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                tbAddr,
                "registerRouter",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());

    }

    @Test
    public void registerIndirectRouter() {
        String tbAddr = "0x06449b13302022b5d49d6bf43283ddece7867aaf";

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String("digitalasset.sit.test.oracle.chain"));
        inputParameters.add(new Bytes32(HexUtil.decodeHex("9440574887f07322a219612f9fc7f2298b84f23c3e2daa2978bd62a3fd100e36")));
        inputParameters.add(new Utf8String("swchain004.test.sit.oracle.chain"));
        inputParameters.add(new Utf8String(""));
        inputParameters.add(new Utf8String("openchain.sit.test.oracle.chain"));

        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                tbAddr,
                "registerIndirectRouter",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());

        inputParameters = new ArrayList<>();
        inputParameters.add(new Utf8String("swchain004.test.sit.oracle.chain"));
        inputParameters.add(new Bytes32(HexUtil.decodeHex("000000000000000000000000c71259f4c71a07ed8d4fb1a26988d29792606409")));
        inputParameters.add(new Utf8String("digitalasset.sit.test.oracle.chain"));
        inputParameters.add(new Utf8String("openchain.sit.test.oracle.chain"));
        inputParameters.add(new Utf8String(""));

        receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                tbAddr,
                "registerIndirectRouter",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());
    }

    @Test
    public void queryTx() {

        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().queryTxInfo("0x196a79e244454c90646ae07b44dda6ad1e8ff2649a6e769a32e2e7077059b043");
// CrossChainAssetReceived(string,bytes32,address,uint256[],uint256[],bytes32,address)
        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());

        // List<TypeReference<?>> outputParameters = new ArrayList<>();
        // outputParameters.add(TypeReference.create(Bytes32.class));

        // List<Type> res = citaCloudBBCService.getCitaCloudClient().localCallContract(
        //         "0x3d65e6d2981e06eb04b518fa373fd0f64ad71c5c",
        //         "getLocalDomain",
        //         ListUtil.empty(),
        //         outputParameters
        // );
    }

    @Test
    public void deployERC1155StrContract() {
        ContractParam tbAddrParam = new ContractParam();
        tbAddrParam.setType("address");
        tbAddrParam.setValue("0x06449b13302022b5d49d6bf43283ddece7867aaf");

        ContractParam nameParam = new ContractParam();
        nameParam.setType("string");
        nameParam.setValue("cc-mapping");

        ContractParam symbolParam = new ContractParam();
        symbolParam.setType("string");
        symbolParam.setValue("TEST");

        ContractParam ifNFTParam = new ContractParam();
        ifNFTParam.setType("bool");
        ifNFTParam.setValue("false");

        String address = citaCloudBBCService.getCitaCloudClient().deployContractWithConstructor(
                FileUtil.readString("erc1155str.bin", StandardCharsets.UTF_8),
                ListUtil.of(tbAddrParam, nameParam, symbolParam, ifNFTParam),
                true
        );

        System.out.println("address is " + address);
    }

    @Test
    public void queryBalance() {

        List<Type> inputParameters = new ArrayList<>() ;
        inputParameters.add(new Address("0xa6b016c59ef254d532b50d5e88e53a695bc5ef57"));
        inputParameters.add(new Utf8String("20230814"));

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(Uint256.class));

        List<Type> res = citaCloudBBCService.getCitaCloudClient().localCallContract(
                "0xc71259f4c71a07ed8d4fb1a26988d29792606409",
                "balanceOf",
                inputParameters,
                outputParameters
        );

        System.out.println(res.get(0).getValue());
    }

    @Test
    public void safeTransferFrom() {
        String erc1155Str = "0xc71259f4c71a07ed8d4fb1a26988d29792606409";

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address("0xa6b016c59ef254d532b50d5e88e53a695bc5ef57"));
        inputParameters.add(new Address("0x06449b13302022b5d49d6bf43283ddece7867aaf"));
        inputParameters.add(new Utf8String("20230814"));
        inputParameters.add(new Uint256(10));
        inputParameters.add(new DynamicBytes(HexUtil.decodeHex("000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000001200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002eb98ac2d71e79dc5dc30b680ebfae0b39d6df61184e976b0a6876d743a8429700000000000000000000000000000000000000000000000000000000000000207377636861696e3030342e746573742e7369742e6f7261636c652e636861696e00000000000000000000000000000000000000000000000000000000000000226469676974616c61737365742e7369742e746573742e6f7261636c652e636861696e000000000000000000000000000000000000000000000000000000000000")));

        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                erc1155Str,
                "safeTransferFrom",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());
    }

    @Test
    public void deployTBProxy() {
        List<Type> parameters = new ArrayList<>();
        parameters.add(new Address("0x06449b13302022b5d49d6bf43283ddece7867aaf"));
        String initCode = FunctionEncoder.encode(new Function("init", parameters, ListUtil.empty()));

        ContractParam callData = new ContractParam();
        callData.setType("bytes");
        callData.setValue(new String(HexUtil.decodeHex(Numeric.cleanHexPrefix(initCode))));

        String address = citaCloudBBCService.getCitaCloudClient().deployContractWithoutConstructor(
                FileUtil.readString("tb_proxy_with_constructor.bin", StandardCharsets.UTF_8),
                true
        );

        System.out.println("address is " + address);
    }

    @Test
    public void upgradeImpl() {
        String newLogic = "";
        String proxy = "";

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(newLogic));

        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                proxy,
                "upgradeTo",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());
    }

    @Test
    public void grantRole() {

        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Bytes32(HexUtil.decodeHex("a49807205ce4d355092ef5a8a18f56e8913cf4a201fbe287825b095693c21775")));
        inputParameters.add(new Address("0x5ccdf6116b46f55e5a558670c8092192964a684d"));

        TransactionReceipt receipt = citaCloudBBCService.getCitaCloudClient().callContract(
                "0x5ccdf6116b46f55e5a558670c8092192964a684d",
                "grantRole",
                inputParameters,
                true
        );

        System.out.printf("tx %s , blk height: %d , res: %s , err: %s\n",
                receipt.getTransactionHash(), receipt.getBlockNumber(), receipt.getStatus(), receipt.getErrorMessage());
    }

    @Test
    public void querySDP() {

        List<Type> inputParameters = new ArrayList<>();

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(Address.class));

        List<Type> res = citaCloudBBCService.getCitaCloudClient().localCallContract(
                "0x5ccdf6116b46f55e5a558670c8092192964a684d",
                "sdp_msg_address",
                inputParameters,
                outputParameters
        );

        System.out.println(res.get(0).getValue());
    }

    @Test
    public void queryImpl() {

        List<Type> inputParameters = new ArrayList<>();

        List<TypeReference<?>> outputParameters = new ArrayList<>();
        outputParameters.add(TypeReference.create(Address.class));

        List<Type> res = citaCloudBBCService.getCitaCloudClient().localCallContract(
                "0x6dfc602b032274cfc238d719bb3262c476d38e47",
                "implementation",
                inputParameters,
                outputParameters
        );

        System.out.println(res.get(0).getValue());
    }

    @Test
    public void encodeFuncData() {
        List<Type> parameters = new ArrayList<>();
        parameters.add(new Address("0x3d65e6d2981e06eb04b518fa373fd0f64ad71c5c"));
        System.out.println(FunctionEncoder.encode(new Function("init", parameters, ListUtil.empty())));
    }
}
