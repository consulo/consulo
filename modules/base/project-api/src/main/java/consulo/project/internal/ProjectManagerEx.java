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
package consulo.project.internal;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.function.Predicate;

public abstract class ProjectManagerEx extends ProjectManager {
  public static ProjectManagerEx getInstanceEx() {
    return (ProjectManagerEx)ProjectManager.getInstance();
  }

  /**
   * @param dirPath path to directory where .consulo directory is located
   */
  @Nullable
  public abstract Project newProject(final String projectName, @Nonnull String dirPath, boolean useDefaultProjectSettings, boolean isDummy);

  public abstract void blockReloadingProjectOnExternalChanges();

  public abstract void unblockReloadingProjectOnExternalChanges();

  // returns true on success
  @RequiredUIAccess
  public abstract boolean closeAndDispose(@Nonnull Project project);

  @Nullable
  @Override
  public Project createProject(String name, String path) {
    return newProject(name, path, true, false);
  }

  public abstract boolean canClose(Project project);

  @Nonnull
  public abstract Disposable registerCloseProjectVeto(@Nonnull Predicate<Project> projectVeto);

  @Nonnull
  //@ApiStatus.Internal
  public abstract String[] getAllExcludedUrls();
}
