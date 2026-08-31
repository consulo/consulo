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

import consulo.localize.LocalizeValue;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolDescriptor;
import consulo.mcpServer.McpToolBuilder;
import consulo.mcpServer.McpToolHandler;
import consulo.mcpServer.McpToolRegistrar;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
final class McpToolCollector implements McpToolRegistrar {
    private final Map<String, McpToolRegistration> myTools = new LinkedHashMap<>();

    @Override
    public McpToolBuilder tool(String name) {
        return new BuilderImpl(name);
    }

    Map<String, McpToolRegistration> getTools() {
        return myTools;
    }

    private final class BuilderImpl implements McpToolBuilder {
        private final String myName;
        private final List<McpParameter> myParameters = new ArrayList<>();

        private LocalizeValue myTitle = LocalizeValue.empty();
        private String myDescription = "";
        private boolean myRequiresProject;
        private @Nullable Boolean myReadOnlyHint;
        private @Nullable Boolean myDestructiveHint;
        private @Nullable Boolean myIdempotentHint;
        private @Nullable Boolean myOpenWorldHint;

        private BuilderImpl(String name) {
            myName = name;
        }

        @Override
        public McpToolBuilder description(String description) {
            myDescription = description;
            return this;
        }

        @Override
        public McpToolBuilder title(LocalizeValue title) {
            myTitle = title;
            return this;
        }

        @Override
        public McpToolBuilder readOnly() {
            myReadOnlyHint = Boolean.TRUE;
            return this;
        }

        @Override
        public McpToolBuilder destructive() {
            myDestructiveHint = Boolean.TRUE;
            return this;
        }

        @Override
        public McpToolBuilder idempotent() {
            myIdempotentHint = Boolean.TRUE;
            return this;
        }

        @Override
        public McpToolBuilder openWorld() {
            myOpenWorldHint = Boolean.TRUE;
            return this;
        }

        @Override
        public McpToolBuilder requiresProject() {
            myRequiresProject = true;
            return this;
        }

        @Override
        public McpToolBuilder param(McpParameter parameter) {
            myParameters.add(parameter);
            return this;
        }

        @Override
        public void handler(McpToolHandler handler) {
            McpToolDescriptor descriptor = new McpToolDescriptor(myName,
                                                                 myTitle,
                                                                 myDescription,
                                                                 myParameters,
                                                                 myReadOnlyHint,
                                                                 myDestructiveHint,
                                                                 myIdempotentHint,
                                                                 myOpenWorldHint);
            myTools.put(myName, new McpToolRegistration(descriptor, handler, myRequiresProject));
        }
    }
}
