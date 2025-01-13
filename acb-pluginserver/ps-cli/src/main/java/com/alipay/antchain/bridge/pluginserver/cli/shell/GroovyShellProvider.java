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

package com.alipay.antchain.bridge.pluginserver.cli.shell;

import java.beans.Introspector;

import com.alipay.antchain.bridge.pluginserver.cli.command.NamespaceManager;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.reflection.ClassInfo;
import org.codehaus.groovy.runtime.InvokerHelper;

public class GroovyShellProvider extends GroovyShell implements ShellProvider {

    private NamespaceManager namespaceManager;

    public GroovyShellProvider(NamespaceManager namespaceManager) {
        this.namespaceManager = namespaceManager;

        // init GroovyShell
        this.namespaceManager.getCommandNamespaces().forEach(namespace -> {

            // only load GroovyScriptCommandNamespace
            if (namespace instanceof GroovyScriptCommandNamespace) {
                this.setVariable(namespace.name(), namespace);
            }
        });
    }

    private int cleanCount = 0;

    private static int CLEAN_PERIOD = 20;

    @Override
    public String execute(String cmd) {
        Script shell = this.parse(cmd);
        Object scriptObject = InvokerHelper.createScript(shell.getClass(), this.getContext()).run();

        // 周期清除缓存，防止OOM
        if((++cleanCount) % CLEAN_PERIOD == 0) {
            getClassLoader().clearCache();
            ClassInfo.clearModifiedExpandos();
            Introspector.flushCaches();
        }

        // execute by groovy script
        return scriptObject.toString();
    }

    @Override
    public void shutdown() {
        // nothing
    }
}
