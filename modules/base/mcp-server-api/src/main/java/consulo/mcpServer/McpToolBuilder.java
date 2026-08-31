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
package consulo.mcpServer;

import consulo.localize.LocalizeValue;
import consulo.mcp.tool.McpParameter;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public interface McpToolBuilder {
    /**
     * Agent-facing description. Stays a plain string — it is prompt input, not UI text.
     */
    McpToolBuilder description(String description);

    /**
     * User-facing name shown in the MCP settings UI.
     */
    McpToolBuilder title(LocalizeValue title);

    McpToolBuilder readOnly();

    McpToolBuilder destructive();

    McpToolBuilder idempotent();

    McpToolBuilder openWorld();

    /**
     * Fails the call with a descriptive error when no project can be resolved for the session.
     */
    McpToolBuilder requiresProject();

    McpToolBuilder param(McpParameter parameter);

    /**
     * Terminal call — registers the tool.
     */
    void handler(McpToolHandler handler);
}
