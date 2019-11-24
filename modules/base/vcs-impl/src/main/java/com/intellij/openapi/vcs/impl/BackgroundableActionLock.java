/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import javax.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

public class BackgroundableActionLock {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final Object[] myKeys;

  BackgroundableActionLock(@Nonnull Project project, @Nonnull final Object[] keys) {
    myProject = project;
    myKeys = keys;
  }

  @RequiredUIAccess
  public boolean isLocked() {
    return isLocked(myProject, myKeys);
  }

  @RequiredUIAccess
  public void lock() {
    lock(myProject, myKeys);
  }

  @RequiredUIAccess
  public void unlock() {
    unlock(myProject, myKeys);
  }


  @Nonnull
  public static BackgroundableActionLock getLock(@Nonnull Project project, @Nonnull Object... keys) {
    return new BackgroundableActionLock(project, keys);
  }

  @RequiredUIAccess
  public static boolean isLocked(@Nonnull Project project, @Nonnull Object... keys) {
    return getManager(project).isBackgroundTaskRunning(keys);
  }

  @RequiredUIAccess
  public static void lock(@Nonnull Project project, @Nonnull Object... keys) {
    getManager(project).startBackgroundTask(keys);
  }

  @RequiredUIAccess
  public static void unlock(@Nonnull Project project, @Nonnull Object... keys) {
    getManager(project).stopBackgroundTask(keys);
  }

  @Nonnull
  private static ProjectLevelVcsManagerImpl getManager(@Nonnull Project project) {
    return (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(project);
  }
}
