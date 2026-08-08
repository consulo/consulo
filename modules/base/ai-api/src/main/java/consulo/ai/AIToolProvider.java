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
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;

import java.util.List;

/**
 * Contributes tools a model may call. MCP supplies its own implementations, so the dependency runs
 * MCP to AI and never the other way round.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AIToolProvider {
    List<AITool> getTools(Project project);
}
