/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.antchain.bridge.relayer.server.admin;

public interface Namespace {

    /**
     * 执行命令
     *
     * <p>执行结果为一个pojo对象,最终会被框架序列化为json返回给调用方,实现该方法需要注意这一点。</p>
     *
     * @param command
     * @param args
     * @return result
     */
    Object executeCommand(String command, String... args);
}
