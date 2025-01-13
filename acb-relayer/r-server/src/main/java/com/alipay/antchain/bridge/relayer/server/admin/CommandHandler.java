/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.antchain.bridge.relayer.server.admin;

/**
 *  @author honglin.qhl
 *  @version $Id: CommandHandler.java, v 0.1 2017-06-17 下午1:31 honglin.qhl Exp $$
 */
@FunctionalInterface
public interface CommandHandler {
    /**
     * 执行结果为一个pojo对象,最终会被框架序列化为json返回给调用方,实现该方法需要注意这一点。
     *
     * @param args
     * @return
     */
    Object execute(String... args);
}
