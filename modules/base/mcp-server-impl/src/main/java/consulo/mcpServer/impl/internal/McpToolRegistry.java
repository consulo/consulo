/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.mcpServer.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.logging.Logger;
import consulo.mcpServer.McpToolset;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpToolRegistry {
    private static final Logger LOG = Logger.getInstance(McpToolRegistry.class);

    private final Application myApplication;

    private volatile @Nullable Map<String, McpToolRegistration> myTools;

    @Inject
    public McpToolRegistry(Application application) {
        myApplication = application;
    }

    public Collection<McpToolRegistration> getTools() {
        return tools().values();
    }

    public @Nullable McpToolRegistration findTool(String name) {
        return tools().get(name);
    }

    private Map<String, McpToolRegistration> tools() {
        Map<String, McpToolRegistration> tools = myTools;
        if (tools == null) {
            McpToolCollector collector = new McpToolCollector();
            myApplication.getExtensionPoint(McpToolset.class).forEach(toolset -> {
                try {
                    toolset.registerTools(collector);
                }
                catch (Throwable e) {
                    LOG.error("Failed to register MCP tools of " + toolset.getClass().getName(), e);
                }
            });
            tools = Collections.unmodifiableMap(new LinkedHashMap<>(collector.getTools()));
            myTools = tools;
        }
        return tools;
    }
}
