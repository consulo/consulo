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
package consulo.ide.impl.idea.tools;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfiguration;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class ToolBeforeRunTaskProvider extends AbstractToolBeforeRunTaskProvider<ToolBeforeRunTask> {
  static final Key<ToolBeforeRunTask> ID = Key.create("ToolBeforeRunTask");

  @Nonnull
  @Override
  public Key<ToolBeforeRunTask> getId() {
    return ID;
  }

  @Nonnull
  @Override
  public String getName() {
    return ToolsBundle.message("tools.before.run.provider.name");
  }

  @Override
  public ToolBeforeRunTask createTask(RunConfiguration runConfiguration) {
    return new ToolBeforeRunTask();
  }

  @Override
  protected ToolsPanel createToolsPanel() {
    return new ToolsPanel();
  }
}
