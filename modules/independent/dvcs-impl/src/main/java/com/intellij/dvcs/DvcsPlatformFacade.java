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
package com.intellij.dvcs;

import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ChangeListManagerEx;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

/**
 * IntelliJ code provides a lot of statical bindings to the interested pieces of data. For example we need to execute code
 * like below to get list of modules for the target project:
 * <pre>
 *   ModuleManager.getInstance(project).getModules()
 * </pre>
 * That means that it's not possible to test target classes in isolation if corresponding infrastructure is not set up.
 * However, we don't want to set it up if we execute a simple standalone test.
 * <p/>
 * This interface is intended to encapsulate access to the underlying IntelliJ functionality.
 * <p/>
 * Implementations of this interface are expected to be thread-safe.
 * 
 * @author Kirill Likhodedov
 */
public interface DvcsPlatformFacade {

  @Nonnull
  AbstractVcs getVcs(@Nonnull Project project);

  @Nonnull
  ProjectLevelVcsManager getVcsManager(@Nonnull Project project);

  void showDialog(@Nonnull DialogWrapper dialog);

  @Nonnull
  ProjectRootManager getProjectRootManager(@Nonnull Project project);

  /**
   * Invokes {@link com.intellij.openapi.application.Application#runReadAction(Computable)}.
   */
  <T> T runReadAction(@Nonnull Computable<T> computable);

  void runReadAction(@Nonnull Runnable runnable);

  void runWriteAction(@Nonnull Runnable runnable);

  void invokeAndWait(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState);

  void executeOnPooledThread(@Nonnull Runnable runnable);

  ChangeListManagerEx getChangeListManager(@Nonnull Project project);

  LocalFileSystem getLocalFileSystem();

  @Nonnull
  AbstractVcsHelper getVcsHelper(@Nonnull Project project);

  @javax.annotation.Nullable
  IdeaPluginDescriptor getPluginByClassName(@Nonnull String name);

  /**
   * Gets line separator of the given virtual file.
   * If {@code detect} is set {@code true}, and the information about line separator wasn't retrieved yet, loads the file and detects.
   */
  @javax.annotation.Nullable
  String getLineSeparator(@Nonnull VirtualFile file, boolean detect);

  void saveAllDocuments();

  @Nonnull
  ProjectManagerEx getProjectManager();

  @Nonnull
  SaveAndSyncHandler getSaveAndSyncHandler();

  void hardRefresh(@Nonnull VirtualFile root);
}
