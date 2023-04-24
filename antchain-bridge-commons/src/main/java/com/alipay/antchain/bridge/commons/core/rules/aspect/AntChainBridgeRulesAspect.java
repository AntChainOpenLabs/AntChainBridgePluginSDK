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

package com.alipay.antchain.bridge.commons.core.rules.aspect;

import java.util.Arrays;

import com.alipay.antchain.bridge.commons.core.rules.AntChainBridgeRule;
import com.alipay.antchain.bridge.commons.exception.CommonsErrorCodeEnum;
import com.alipay.antchain.bridge.commons.exception.AntChainBridgeCommonsException;
import com.alipay.antchain.bridge.commons.exception.base.AntChainBridgeBaseException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class AntChainBridgeRulesAspect {

    @Pointcut("execution(@AntChainBridgeRules *.new(..)) && @annotation(antchainBridgeRules)")
    public void antchainBridgeRulesMethodConstructor(AntChainBridgeRules antchainBridgeRules) {}

    @After("antchainBridgeRulesMethodConstructor(antchainBridgeRules)")
    public void afterAntChainBridgeRulesMethodConstructor(JoinPoint joinPoint, AntChainBridgeRules antchainBridgeRules) {
        try {
            Arrays.stream(antchainBridgeRules.value()).forEach(
                    clz -> {
                        try {
                            if (!AntChainBridgeRule.class.isAssignableFrom(clz)) {
                                return;
                            }
                            AntChainBridgeRule rule = (AntChainBridgeRule) clz.newInstance();
                            if (!rule.check(joinPoint.getTarget())) {
                                throw new RuntimeException(clz.getName());
                            }
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new AntChainBridgeCommonsException(
                                    CommonsErrorCodeEnum.RULES_CHECK_ERROR,
                                    "rules check failed: ",
                                    e
                            );
                        }
                    }
            );
        } catch (AntChainBridgeBaseException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AntChainBridgeCommonsException(
                    CommonsErrorCodeEnum.RULES_CHECK_ERROR,
                    "check rules not pass: " + e.getMessage()
            );
        }
    }
}
