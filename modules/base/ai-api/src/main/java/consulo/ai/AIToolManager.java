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
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Collects the tools contributed by every {@link AIToolProvider}, so callers do not have to know
 * which subsystem or MCP server a tool came from.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface AIToolManager {
    static AIToolManager getInstance() {
        return Application.get().getInstance(AIToolManager.class);
    }

    List<AITool> getTools(Project project);

    @Nullable AITool findTool(Project project, String name);
}
