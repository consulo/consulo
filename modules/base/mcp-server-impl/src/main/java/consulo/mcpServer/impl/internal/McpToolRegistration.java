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

import consulo.mcp.tool.McpToolDescriptor;
import consulo.mcpServer.McpToolHandler;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpToolRegistration {
    private final McpToolDescriptor myDescriptor;
    private final McpToolHandler myHandler;
    private final boolean myRequiresProject;

    McpToolRegistration(McpToolDescriptor descriptor, McpToolHandler handler, boolean requiresProject) {
        myDescriptor = descriptor;
        myHandler = handler;
        myRequiresProject = requiresProject;
    }

    public McpToolDescriptor getDescriptor() {
        return myDescriptor;
    }

    public McpToolHandler getHandler() {
        return myHandler;
    }

    public boolean isRequiresProject() {
        return myRequiresProject;
    }
}
