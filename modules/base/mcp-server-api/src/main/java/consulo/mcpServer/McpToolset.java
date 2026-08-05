/*
 * Copyright 2000-2026 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * Contributes MCP tools exposed to external agents. Implementations belong in the module that owns
 * the domain they act on, so that the MCP modules stay free of subsystem dependencies.
 *
 * <pre>
 * &#64;ExtensionImpl
 * public class FileToolset implements McpToolset {
 *     &#64;Override
 *     public void registerTools(McpToolRegistrar registrar) {
 *         registrar.tool("read_file")
 *             .description("Reads a project file by project-relative path.")
 *             .readOnly()
 *             .idempotent()
 *             .param(McpParameter.string("path", "Project-relative file path."))
 *             .handler(this::readFile);
 *     }
 * }
 * </pre>
 *
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface McpToolset {
    void registerTools(McpToolRegistrar registrar);
}
