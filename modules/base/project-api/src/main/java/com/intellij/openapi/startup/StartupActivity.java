/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.startup;

import com.intellij.openapi.project.Project;
import consulo.annotation.DeprecationInfo;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;

/**
 * <p>Runs an activity on project open.</p>
 *
 * <p>If the activity implements {@link com.intellij.openapi.project.DumbAware} interface, e.g. {@link DumbAware}, it will be started in a pooled thread
 * under 'Loading Project' dialog, otherwise it will be started in the dispatch thread after the initialization.</p>
 *
 * @author Dmitry Avdeev
 */
@Deprecated
@DeprecationInfo("Use consulo.project.startup.StartupActivity")
public interface StartupActivity extends consulo.project.startup.StartupActivity {
  interface DumbAware extends StartupActivity, com.intellij.openapi.project.DumbAware {
  }

  interface Background extends StartupActivity, consulo.project.startup.StartupActivity.Background {
  }

  default void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
    runActivity(project);
  }

  @Override
  default void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
    runActivity(uiAccess, project);
  }

  @Deprecated
  @DeprecationInfo("Use #runActivity(@NotNull UIAccess uiAccess, @NotNull Project project)")
  default void runActivity(Project project) {
    throw new AbstractMethodError();
  }
}
