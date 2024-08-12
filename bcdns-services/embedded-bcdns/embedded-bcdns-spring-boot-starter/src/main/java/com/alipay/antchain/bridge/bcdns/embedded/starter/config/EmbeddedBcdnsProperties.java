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

package com.alipay.antchain.bridge.bcdns.embedded.starter.config;

import com.alipay.antchain.bridge.commons.utils.crypto.HashAlgoEnum;
import com.alipay.antchain.bridge.commons.utils.crypto.SignAlgoEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("acb.bcdns.embedded")
@Data
public class EmbeddedBcdnsProperties {

    private boolean serverOn = false;

    private String serverHost = "0.0.0.0";

    private int serverPort = 8090;

    private HashAlgoEnum signCertHashAlgo = HashAlgoEnum.KECCAK_256;

    private SignAlgoEnum signAlgo = SignAlgoEnum.KECCAK256_WITH_SECP256K1;

    private Resource rootPrivateKeyFile;

    private Resource rootCertFile;
}
