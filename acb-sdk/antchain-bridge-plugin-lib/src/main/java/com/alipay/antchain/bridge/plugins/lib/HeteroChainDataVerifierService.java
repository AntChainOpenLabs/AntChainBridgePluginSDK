package com.alipay.antchain.bridge.plugins.lib;


import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotation to mark PTC plugin implementation.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
@Documented
public @interface HeteroChainDataVerifierService {

    /**
     * the blockchain product name like {@code Ethereum} etc.
     *
     * @return {@link String[]}
     */
    String[] products() default {};

    /**
     * the unique identity of the plugin
     *
     * @return {@link String[]}
     */
    String[] pluginId() default {};
}
