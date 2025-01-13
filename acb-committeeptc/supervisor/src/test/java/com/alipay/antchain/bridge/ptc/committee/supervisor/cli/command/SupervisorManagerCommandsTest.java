package com.alipay.antchain.bridge.ptc.committee.supervisor.cli.command;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.factory.BlockChainDomainNameServiceFactory;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentityType;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import com.alipay.antchain.bridge.ptc.committee.supervisor.cli.Launcher;
import jakarta.annotation.Resource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Launcher.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SupervisorManagerCommandsTest {

    private static final String certPath = "src/test/resources/certs/";

    private static final String pubKeyPath = certPath + "public_key.pem";

    private static final String bcdnsDir = "src/test/resources/bcdns/";

    private static final String committeeNodeConfigDir = "src/test/resources/committeeNodes/";

    private static final String ptcTrustRootStr = "AAAECAAAAAAHAAAAeHh4Lm9yZwEAoAAAAAAAmgAAAAAAAwAAADEuMAEADQAAAGNvbW1pdHRlZS1wdGMCAAEAAAABAwBrAAAAAABlAAAAAAABAAAAAAEAWAAAADBWMBAGByqGSM49AgEGBSuBBAAKA0IABAuFrr4a+xRRyeBamtx6MXrj9K8zCa9zwRSP54OYMVsYWyXFOa8H3YOJnLic5aY1mHujouKInzqW+/LrFCkJP8MEAAAAAAACAKAFAAB7ImNvbW1pdHRlZV9pZCI6ImNvbW1pdHRlZTEiLCJub2RlcyI6W3siZW5kcG9pbnQiOiJncnBjOi8vMC4wLjAuMDo4MDgwIiwibm9kZV9pZCI6Im5vZGUxIiwidGxzX2NlcnQiOiItLS0tLUJFR0lOIENFUlRJRklDQVRFLS0tLS1cbk1JSURuRENDQW9TZ0F3SUJBZ0lKQU5vUit1YmViaFFiTUEwR0NTcUdTSWIzRFFFQkN3VUFNSHd4RVRBUEJnTlZcbkJBb01DR0Z1ZEdOb1lXbHVNUTR3REFZRFZRUUxEQVZ2WkdGMGN6RWxNQ01HQTFVRUF3d2NZVzUwWTJoaGFXNHVcbmIyUmhkSE5mZEd4ekxuTnBkQzV2WkdGMGN6RVJNQThHQTFVRUJBd0lkR3h6TG5KdmIzUXhIVEFiQmdOVkJBa01cbkZFTk9MbHBvWldwcFlXNW5Ma2hoYm1kNmFHOTFNQjRYRFRJek1EWXdOVEUwTXpjME5sb1hEVE16TURZd01qRTBcbk16YzBObG93Y1RFTE1Ba0dBMVVFQmhNQ1EwNHhFVEFQQmdOVkJBZ01DRnBvWldwcFlXNW5NUkV3RHdZRFZRUUhcbkRBaElZVzVuZW1odmRURVJNQThHQTFVRUNnd0lZVzUwWTJoaGFXNHhEakFNQmdOVkJBc01CVzlrWVhSek1Sa3dcbkZ3WURWUVFEREJCa2N5NTBiSE11YzJsMExtOWtZWFJ6TUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFcbk1JSUJDZ0tDQVFFQXo0blV2K3lrQyt4WnVXZ3Vwa0tRZzNWSW9UTjR5ejBvOWxyaXFwdWZnK1F0ZWgyd25pQVJcbmRXamRya0hwTllCSnRNTXoxYkVMODR3OHlDK2txTTVJT0ZwRHNuUEhMUnFRYjJ5a3RjakJzc3lWZTcxQnZ1R1Vcbjd3VCtON0RRVWJJelRnN0YreXpDYk5pajdOalVhY1Y3MEVLZENrUkJxSVlTTEVLYnpNQkI0ci9odVpZdVBaYWRcbnJoSVh1b2g2Q2RvdFZPRTRpWkd3VXgwcHFKQ2pRSE45RFo1bFpYMzAyZEs2Y3FYdnRyNzlCa1JVZndVY3FtTlVcbnBsMThqTVVLaStWL1M1NHB5OTJZbkpGZHBkLy9RbWRhQVk3V2FESW10bnB1YWZ3K3RSTlowRlIwaXZlamljL3lcbmpmcVcySFRpN0dPdHlWbHZKYTJLSHEzb0t0R2lBMGpiandJREFRQUJveXd3S2pBSkJnTlZIUk1FQWpBQU1CMEdcbkExVWRKUVFXTUJRR0NDc0dBUVVGQndNQkJnZ3JCZ0VGQlFjREFqQU5CZ2txaGtpRzl3MEJBUXNGQUFPQ0FRRUFcbmxEbVVUM2V4cHNiRFBpREIwTDFSNEpWalJjayswS01HMGtVS3Q1R2tBcHd2UU9hVFhMV3BTOVhvWHhOMmo3SGZcblVHVkhXMThLbUczek1uM1p3VDVrb3lQY0hvaG5xOFNvRENOR2YwWENUOVdIYURwU25tWm1yd1kxemRUTmNDa01cbmtwaG5IZE5FUk04eEFIMWRYWCtNVzdvcXpJeFZrUVU5Tkk4TlJtK3UwYVJaVXMra01Bb3ovTk5IVWdSK3BQUXdcbkdVQXp3b0FTcCtMaVRZc1hNNlhCVzhPcEIzUE02bk9FWXpwbWJ6RTJMWWRIeHZTNG1rVWw3NEN5ejMxTDBQU3FcblE0NVlBOFMycWRxTkNXZ28rdklGSUpxaFpmOHltdzlWUkhHRnBncXVmWlJia2dBeE1Xa2FzdDJBWEdhT2pVdkJcbk45MmV1OXAzaHlJL2oxWE9MRDlDUkE9PVxuLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLSJ9XX0DAEIBAAABAAAAADkBAAAAADMBAAAAAAEAAAAAAQAmAQAAeyJhbmNob3JzIjpbeyJub2RlX2lkIjoibm9kZTEiLCJub2RlX3B1YmxpY19rZXlzIjpbeyJrZXlfaWQiOiJrZXkxIiwicHVibGljX2tleSI6Ii0tLS0tQkVHSU4gUFVCTElDIEtFWS0tLS0tXG5NRll3RUFZSEtvWkl6ajBDQVFZRks0RUVBQW9EUWdBRUM0V3V2aHI3RkZISjRGcWEzSG94ZXVQMHJ6TUpyM1BCXG5GSS9uZzVneFd4aGJKY1U1cndmZGc0bWN1SnpscGpXWWU2T2k0b2lmT3BiNzh1c1VLUWsvd3c9PVxuLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0tXG4ifV19XSwiY29tbWl0dGVlX2lkIjoiY29tbWl0dGVlMSJ9BAAWAAAAS2VjY2FrMjU2V2l0aFNlY3AyNTZrMQUAQQAAABnx7AwMmFqxSdCGuSKHUCDh6mig1yLmlh1ppY5XsAyERLKvUXa4r68z2/4IdcnOI/hdVdA2CAfmXtbj5Jm7gSYA";

    private static IBlockChainDomainNameService bcdns;

    private static MockedStatic<BlockChainDomainNameServiceFactory> mockedBCDNSFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        bcdns = mock(IBlockChainDomainNameService.class);

        mockedBCDNSFactory = Mockito.mockStatic(BlockChainDomainNameServiceFactory.class);
        mockedBCDNSFactory.when(() -> BlockChainDomainNameServiceFactory.create(Mockito.notNull(), Mockito.notNull())).thenReturn(bcdns);

        Mockito.doNothing().when(bcdns).addPTCTrustRoot(Mockito.any());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        mockedBCDNSFactory.close();
    }

    @Resource
    private SupervisorManagerCommands supervisorManagerCommands;

    @Test
    public void testGeneratePTCAccount() throws IOException {
        supervisorManagerCommands.generatePtcAccount("committee1", SignAlgoEnum.KECCAK256_WITH_SECP256K1.getName(), certPath);
        Assert.assertNotEquals(supervisorManagerCommands.getSupervisorCLIConfig().getPrivateKey(), "");
        Assert.assertNotEquals(supervisorManagerCommands.getSupervisorCLIConfig().getPublicKey(), "");
        Assert.assertNotEquals(supervisorManagerCommands.getSupervisorCLIConfig().getSignAlgo(), "");
    }

    @Test
    public void testGeneratePtcCSR() {
        String response = supervisorManagerCommands.generatePtcCSR("2",
                "myPTC",
                ObjectIdentityType.X509_PUBLIC_KEY_INFO,
                PTCTypeEnum.COMMITTEE,
                pubKeyPath);
        Assert.assertNotEquals(response, "please input the path to the correct applicant public key file");
    }

    @Test
    public void testStartEmbeddedBCDNSClient() {
        String domainSpace = ".org";
        Path bcdnsClientConfigPath = Paths.get(bcdnsDir, "client1.json");
        supervisorManagerCommands.startBcdnsClient(domainSpace, BCDNSTypeEnum.EMBEDDED, bcdnsClientConfigPath.toString());
        Assert.assertNotNull(supervisorManagerCommands.getDomainSpaceToBcdnsMap().get(domainSpace));
        Assert.assertNotNull(supervisorManagerCommands.getSupervisorCLIConfig().getBcdnsConfig().get(domainSpace));
    }

    @Test
    public void testStartBifBCDNSClient() {
        String domainSpace = ".com";
        Path bcdnsClientConfigPath = Paths.get(bcdnsDir, "client2.json");
        supervisorManagerCommands.startBcdnsClient(domainSpace, BCDNSTypeEnum.BIF, bcdnsClientConfigPath.toString());
        Assert.assertNotNull(supervisorManagerCommands.getDomainSpaceToBcdnsMap().get(domainSpace));
        Assert.assertNotNull(supervisorManagerCommands.getSupervisorCLIConfig().getBcdnsConfig().get(domainSpace));
    }

    @Test
    public void testGeneratePTCTrustRoot() {
        List<String> committeeNodes = new ArrayList<>();
        committeeNodes.add("node1");
        Object response = supervisorManagerCommands.generatePtcTrustRoot(
                ".org",
                committeeNodeConfigDir,
                committeeNodes
        );
        Assert.assertNotNull(response);
    }

    @Test
    public void testAddPTCTrustRoot() {
        String domainSpace = ".com";
        Object response = supervisorManagerCommands.addPtcTrustRoot(domainSpace, ptcTrustRootStr);
        Assert.assertNotNull(supervisorManagerCommands.getDomainSpaceToBcdnsMap().get(domainSpace));
        Assert.assertNotEquals(response, StrUtil.format("PTC service has not yet created domainSpace: {}'s BCDNS client", domainSpace));
    }
}
