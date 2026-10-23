/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.externalTool.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfiguration;
import consulo.externalTool.impl.internal.localize.ExternalToolLocalize;
import consulo.localize.LocalizeValue;

import java.util.List;

@ExtensionImpl
public class ToolBeforeRunTaskProvider extends AbstractToolBeforeRunTaskProvider<ToolBeforeRunTask> {
    @Override
    public String getId() {
        return "ToolBeforeRunTask";
    }

    @Override
    public LocalizeValue getName() {
        return ExternalToolLocalize.toolsBeforeRunProviderName();
    }

    @Override
    public Class<ToolBeforeRunTask> getStateClass() {
        return ToolBeforeRunTask.class;
    }

    @Override
    public ToolBeforeRunTask createTask(RunConfiguration runConfiguration) {
        return new ToolBeforeRunTask();
    }

    @Override
    protected List<Tool> getTools() {
        return ToolManager.getInstance().getTools();
    }
}
