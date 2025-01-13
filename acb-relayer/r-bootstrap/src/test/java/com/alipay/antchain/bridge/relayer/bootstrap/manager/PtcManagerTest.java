/*
 * Copyright 2024 Ant Group
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

package com.alipay.antchain.bridge.relayer.bootstrap.manager;

import javax.annotation.Resource;

import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.ptc.committee.config.CommitteePtcConfig;
import com.alipay.antchain.bridge.ptc.committee.types.network.CommitteeNetworkInfo;
import com.alipay.antchain.bridge.ptc.service.IPTCService;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.IPtcManager;
import com.alipay.antchain.bridge.relayer.core.manager.ptc.PtcServiceFactory;
import com.alipay.antchain.bridge.relayer.dal.repository.IPtcServiceRepository;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.*;

public class PtcManagerTest extends TestBase {

    @Resource
    private IPtcManager ptcManager;

    @MockBean
    private IPtcServiceRepository ptcServiceRepository;

    @Test
    public void testRegisterPtcService() {
        MockedStatic<PtcServiceFactory> ptcServiceFactoryMockedStatic = null;
        try {
            IPTCService ptcServiceMock = mock(IPTCService.class);

            ptcServiceFactoryMockedStatic = mockStatic(PtcServiceFactory.class);
            ptcServiceFactoryMockedStatic.when(() -> PtcServiceFactory.buildPtcConfig(any(), any(), any())).thenCallRealMethod();
            ptcServiceFactoryMockedStatic.when(() -> PtcServiceFactory.createPtcServiceStub(any(), any())).thenReturn(ptcServiceMock);

            CommitteePtcConfig committeePtcConfig = new CommitteePtcConfig();
            committeePtcConfig.setTlsClientPemPkcs8Key("-----BEGIN PRIVATE KEY-----\n" +
                    "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDPidS/7KQL7Fm5\n" +
                    "aC6mQpCDdUihM3jLPSj2WuKqm5+D5C16HbCeIBF1aN2uQek1gEm0wzPVsQvzjDzI\n" +
                    "L6Sozkg4WkOyc8ctGpBvbKS1yMGyzJV7vUG+4ZTvBP43sNBRsjNODsX7LMJs2KPs\n" +
                    "2NRpxXvQQp0KREGohhIsQpvMwEHiv+G5li49lp2uEhe6iHoJ2i1U4TiJkbBTHSmo\n" +
                    "kKNAc30NnmVlffTZ0rpype+2vv0GRFR/BRyqY1SmXXyMxQqL5X9LninL3ZickV2l\n" +
                    "3/9CZ1oBjtZoMia2em5p/D61E1nQVHSK96OJz/KN+pbYdOLsY63JWW8lrYoeregq\n" +
                    "0aIDSNuPAgMBAAECggEAUH0rFrgnMzyZ269NEEwWkfVFksdMnL3+ifTbncE3T0aK\n" +
                    "YKbtHZZgTwG5n+COGqLDcyiVjNXaRb1owVbA7Hr8RWa0hJwkbhi0VZJ0GtBeVwLD\n" +
                    "IrdWrTn9selk0qJvWI/dF/Pg0rYcPWyTvsKlNtRRXYbIMvgf4sUEfUfj9rfFlbOT\n" +
                    "6yewZklkPqS1fMXM0D8kZuHyMz/uHnb7LCKq1iBwExQSKMYZ8RWcxzfdTIBCn/fS\n" +
                    "XUUNeXXR6xVJxLlZFz9Ul5h3v9R5B50iiznm30+slwCsfaj32dSjj9aDLJfFW39R\n" +
                    "RQyx+NDyg5eI8JaJfbHfrH1U0Ei8zCgtIpZ6N6QmGQKBgQD6JAKqVU9d93vkCJb9\n" +
                    "Altksktk+JgFxsCTrKlb7m4iSb/ADxkPa2SgLmVZW31GSFwpsNhhvxIKRmPF4w+G\n" +
                    "5XltQmCjbZLp9eWaJthnId2xonC2AHMUbSQisog2+HI5Fb8Q4fqxYFCNgvNSW1Ht\n" +
                    "CRowzrb2kUCdZaMTUuiKQVOPWwKBgQDUZlpCq9aeUbS/vcuPuPG1cpvb1VzhupIs\n" +
                    "LS1hRyutLdiHndeegKJ6d0psJq9v2lDFimLPLczCZNFoKWFqOOzaRzueJ8ejCYQR\n" +
                    "fXld1pqPPC9yCRM1MB831vpUg75JasGT10JaFHAaIddKDB0l7c8CTGYquiO7DH00\n" +
                    "wuyj3jdu3QKBgA1BdUaziKYxJEacUewMgO1gKXCrX9sGglQRFVSC2SFGCTxTUH+p\n" +
                    "sEZwzvwiRgxAb2niLkVXy8vxmP32n28FoB6zIs3mU5/EYSt/HX6xo77zHcf3VCHj\n" +
                    "+sM/9Mn89oih52Mspo1ZzksBgoV9w2StU878VWPRpLvyk+bFQP96oMP7AoGAUQKI\n" +
                    "wo0P2mqHaepVzYdYiUAhOgNy3ZVvUvIYMNYYToEB6RfGuWmOju8Yr49BsoOt8uoJ\n" +
                    "LcPmKO6TAAtoYD899zLcBkJd3k0u1gzpUWUcpizqW7AiZ1LnVUDlUX6+APp6woyD\n" +
                    "fh/1ccIeftuH8oN1RQcmoH1GS31D8++0mfuTYPECgYA6w5OO+xyE83IiKTaZW3Xk\n" +
                    "JsL9lhnbt3PSvPOfr58dOfdypTPVfIrjb8iuwJJtq2mR/A49+KYZEhnvQ/vGEY1s\n" +
                    "NB4QIeiB0Otet38M1KcwrrQNapL/G9E3mWMcyH5ng1f15mFZF6cAJplbp+1WgbAy\n" +
                    "yLcsggbJXxq480BxP2zKSw==\n" +
                    "-----END PRIVATE KEY-----");
            committeePtcConfig.setTlsClientPemCert("-----BEGIN CERTIFICATE-----\n" +
                    "MIIDnDCCAoSgAwIBAgIJANoR+ubebhQbMA0GCSqGSIb3DQEBCwUAMHwxETAPBgNV\n" +
                    "BAoMCGFudGNoYWluMQ4wDAYDVQQLDAVvZGF0czElMCMGA1UEAwwcYW50Y2hhaW4u\n" +
                    "b2RhdHNfdGxzLnNpdC5vZGF0czERMA8GA1UEBAwIdGxzLnJvb3QxHTAbBgNVBAkM\n" +
                    "FENOLlpoZWppYW5nLkhhbmd6aG91MB4XDTIzMDYwNTE0Mzc0NloXDTMzMDYwMjE0\n" +
                    "Mzc0NlowcTELMAkGA1UEBhMCQ04xETAPBgNVBAgMCFpoZWppYW5nMREwDwYDVQQH\n" +
                    "DAhIYW5nemhvdTERMA8GA1UECgwIYW50Y2hhaW4xDjAMBgNVBAsMBW9kYXRzMRkw\n" +
                    "FwYDVQQDDBBkcy50bHMuc2l0Lm9kYXRzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n" +
                    "MIIBCgKCAQEAz4nUv+ykC+xZuWgupkKQg3VIoTN4yz0o9lriqpufg+Qteh2wniAR\n" +
                    "dWjdrkHpNYBJtMMz1bEL84w8yC+kqM5IOFpDsnPHLRqQb2yktcjBssyVe71BvuGU\n" +
                    "7wT+N7DQUbIzTg7F+yzCbNij7NjUacV70EKdCkRBqIYSLEKbzMBB4r/huZYuPZad\n" +
                    "rhIXuoh6CdotVOE4iZGwUx0pqJCjQHN9DZ5lZX302dK6cqXvtr79BkRUfwUcqmNU\n" +
                    "pl18jMUKi+V/S54py92YnJFdpd//QmdaAY7WaDImtnpuafw+tRNZ0FR0ivejic/y\n" +
                    "jfqW2HTi7GOtyVlvJa2KHq3oKtGiA0jbjwIDAQABoywwKjAJBgNVHRMEAjAAMB0G\n" +
                    "A1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjANBgkqhkiG9w0BAQsFAAOCAQEA\n" +
                    "lDmUT3expsbDPiDB0L1R4JVjRck+0KMG0kUKt5GkApwvQOaTXLWpS9XoXxN2j7Hf\n" +
                    "UGVHW18KmG3zMn3ZwT5koyPcHohnq8SoDCNGf0XCT9WHaDpSnmZmrwY1zdTNcCkM\n" +
                    "kphnHdNERM8xAH1dXX+MW7oqzIxVkQU9NI8NRm+u0aRZUs+kMAoz/NNHUgR+pPQw\n" +
                    "GUAzwoASp+LiTYsXM6XBW8OpB3PM6nOEYzpmbzE2LYdHxvS4mkUl74Cyz31L0PSq\n" +
                    "Q45YA8S2qdqNCWgo+vIFIJqhZf8ymw9VRHGFpgqufZRbkgAxMWkast2AXGaOjUvB\n" +
                    "N92eu9p3hyI/j1XOLD9CRA==\n" +
                    "-----END CERTIFICATE-----");
            committeePtcConfig.setPtcCertificate(NODE_PTC_CERT);
            CommitteeNetworkInfo committeeNetworkInfo = new CommitteeNetworkInfo(COMMITTEE_ID);
            committeeNetworkInfo.addEndpoint(
                    "node1",
                    "grpcs://127.0.0.1:1234",
                    "-----BEGIN CERTIFICATE-----\n" +
                            "MIIDnDCCAoSgAwIBAgIJANoR+ubebhQbMA0GCSqGSIb3DQEBCwUAMHwxETAPBgNV\n" +
                            "BAoMCGFudGNoYWluMQ4wDAYDVQQLDAVvZGF0czElMCMGA1UEAwwcYW50Y2hhaW4u\n" +
                            "b2RhdHNfdGxzLnNpdC5vZGF0czERMA8GA1UEBAwIdGxzLnJvb3QxHTAbBgNVBAkM\n" +
                            "FENOLlpoZWppYW5nLkhhbmd6aG91MB4XDTIzMDYwNTE0Mzc0NloXDTMzMDYwMjE0\n" +
                            "Mzc0NlowcTELMAkGA1UEBhMCQ04xETAPBgNVBAgMCFpoZWppYW5nMREwDwYDVQQH\n" +
                            "DAhIYW5nemhvdTERMA8GA1UECgwIYW50Y2hhaW4xDjAMBgNVBAsMBW9kYXRzMRkw\n" +
                            "FwYDVQQDDBBkcy50bHMuc2l0Lm9kYXRzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A\n" +
                            "MIIBCgKCAQEAz4nUv+ykC+xZuWgupkKQg3VIoTN4yz0o9lriqpufg+Qteh2wniAR\n" +
                            "dWjdrkHpNYBJtMMz1bEL84w8yC+kqM5IOFpDsnPHLRqQb2yktcjBssyVe71BvuGU\n" +
                            "7wT+N7DQUbIzTg7F+yzCbNij7NjUacV70EKdCkRBqIYSLEKbzMBB4r/huZYuPZad\n" +
                            "rhIXuoh6CdotVOE4iZGwUx0pqJCjQHN9DZ5lZX302dK6cqXvtr79BkRUfwUcqmNU\n" +
                            "pl18jMUKi+V/S54py92YnJFdpd//QmdaAY7WaDImtnpuafw+tRNZ0FR0ivejic/y\n" +
                            "jfqW2HTi7GOtyVlvJa2KHq3oKtGiA0jbjwIDAQABoywwKjAJBgNVHRMEAjAAMB0G\n" +
                            "A1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjANBgkqhkiG9w0BAQsFAAOCAQEA\n" +
                            "lDmUT3expsbDPiDB0L1R4JVjRck+0KMG0kUKt5GkApwvQOaTXLWpS9XoXxN2j7Hf\n" +
                            "UGVHW18KmG3zMn3ZwT5koyPcHohnq8SoDCNGf0XCT9WHaDpSnmZmrwY1zdTNcCkM\n" +
                            "kphnHdNERM8xAH1dXX+MW7oqzIxVkQU9NI8NRm+u0aRZUs+kMAoz/NNHUgR+pPQw\n" +
                            "GUAzwoASp+LiTYsXM6XBW8OpB3PM6nOEYzpmbzE2LYdHxvS4mkUl74Cyz31L0PSq\n" +
                            "Q45YA8S2qdqNCWgo+vIFIJqhZf8ymw9VRHGFpgqufZRbkgAxMWkast2AXGaOjUvB\n" +
                            "N92eu9p3hyI/j1XOLD9CRA==\n" +
                            "-----END CERTIFICATE-----"
            );
            committeePtcConfig.setCommitteeNetworkInfo(committeeNetworkInfo);

            when(ptcServiceRepository.hasPtcServiceData(anyString())).thenReturn(false);

            ptcManager.registerPtcService(PTC_SERVICE_ID1, new CrossChainDomain(CrossChainDomain.ROOT_DOMAIN_SPACE), NODE_PTC_CERT, committeePtcConfig.encode());
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            if (ptcServiceFactoryMockedStatic != null) {
                ptcServiceFactoryMockedStatic.close();
            }
        }
    }
}
