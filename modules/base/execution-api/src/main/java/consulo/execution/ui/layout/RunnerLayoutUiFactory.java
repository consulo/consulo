/*
 * Copyright 2013-2022 consulo.io
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
package consulo.execution.ui.layout;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.project.Project;

import javax.annotation.Nonnull;

@ServiceAPI(ComponentScope.PROJECT)
public interface RunnerLayoutUiFactory {
  public static RunnerLayoutUiFactory getInstance(Project project) {
    return project.getInstance(RunnerLayoutUiFactory.class);
  }

  @Nonnull
  public abstract RunnerLayoutUi create(@Nonnull String runnerId, @Nonnull String runnerTitle, @Nonnull String sessionName, @Nonnull Disposable parent);
}
