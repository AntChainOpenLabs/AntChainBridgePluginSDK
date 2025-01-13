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

package com.alipay.antchain.bridge.pluginserver.cli.command;

import com.alipay.antchain.bridge.pluginserver.cli.shell.GroovyScriptCommandNamespace;

public class ManagementCommandNamespace extends GroovyScriptCommandNamespace {
    @Override
    public String name() {
        return "manage";
    }

    Object loadPlugins() {
        return queryAPI("loadPlugins");
    }

    Object startPlugins() {
        return queryAPI("startPlugins");
    }

    Object loadPlugin(
            @ArgsConstraint(name = "path") String path
    ) {
        return queryAPI("loadPlugin", path);
    }

    Object startPlugin(
            @ArgsConstraint(name = "path") String path
    ) {
        return queryAPI("startPlugin", path);
    }

    Object stopPlugin(
            @ArgsConstraint(name = "product") String product
    ) {
        return queryAPI("stopPlugin", product);
    }

    Object startPluginFromStop(
            @ArgsConstraint(name = "product") String product
    ) {
        return queryAPI("startPluginFromStop", product);
    }

    Object reloadPlugin(
            @ArgsConstraint(name = "product") String product
    ) {
        return queryAPI("reloadPlugin", product);
    }

    Object reloadPluginInNewPath(
            @ArgsConstraint(name = "product") String product,
            @ArgsConstraint(name = "path") String path
    ) {
        return queryAPI("reloadPluginInNewPath", product, path);
    }

    Object hasPlugins(
            @ArgsConstraint(name = "products...") String... products
    ) {
        return queryAPI("hasPlugins", products);
    }

    Object allPlugins() {
        return queryAPI("allPlugins");
    }

    Object hasDomains(
            @ArgsConstraint(name = "domains...") String... domains
    ) {
        return queryAPI("hasDomains", domains);
    }

    Object allDomains() {
        return queryAPI("allDomains");
    }

    Object restartBBC(
            @ArgsConstraint(name = "product") String product,
            @ArgsConstraint(name = "domain") String domain
    ) {
        return queryAPI("restartBBC", product, domain);
    }
}
