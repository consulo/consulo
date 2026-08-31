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

import consulo.mcp.tool.McpToolException;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Accessors throw {@link McpToolException} when a required argument is missing or has the wrong
 * shape, so tools can read arguments without defensive checks.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
public interface McpToolCallContext {
    /**
     * @throws McpToolException when no project could be resolved for this call
     */
    Project getProject();

    String getString(String name);

    @Nullable String findString(String name);

    int getInt(String name);

    boolean getBoolean(String name);

    List<String> getStringList(String name);
}
