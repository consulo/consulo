/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.execution.ui.layout.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.execution.ui.layout.RunnerLayoutUiFactory;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

@Singleton
@ServiceImpl
public class RunnerLayoutUiFactoryImpl implements RunnerLayoutUiFactory {
  private final Project myProject;

  @Inject
  public RunnerLayoutUiFactoryImpl(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public RunnerLayoutUi create(@Nonnull final String runnerId, @Nonnull final String runnerTitle, @Nonnull final String sessionName, @Nonnull final Disposable parent) {
    return new RunnerLayoutUiImpl(myProject, parent, runnerId, runnerTitle, sessionName);
  }
}
