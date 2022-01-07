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
package com.intellij.openapi.project.ex;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  @RequiredUIAccess
  public boolean openProject(Project project) {
    return openProject(project, UIAccess.current());
  }

  public abstract boolean openProject(@Nonnull Project project, @Nonnull UIAccess uiAccess);

  public abstract boolean isProjectOpened(Project project);

  public abstract void saveChangedProjectFile(@Nonnull VirtualFile file, @Nonnull Project project);

  public abstract void blockReloadingProjectOnExternalChanges();

  public abstract void unblockReloadingProjectOnExternalChanges();

  @TestOnly
  @RequiredUIAccess
  public abstract void openTestProject(@Nonnull Project project);

  @TestOnly
  // returns remaining open test projects
  @RequiredUIAccess
  public abstract Collection<Project> closeTestProject(@Nonnull Project project);

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
