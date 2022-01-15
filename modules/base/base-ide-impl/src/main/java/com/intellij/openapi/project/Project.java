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
package com.intellij.openapi.project;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Project interface class.
 */
public interface Project extends ComponentManager {
  String DIRECTORY_STORE_FOLDER = ".consulo";

  /**
   * @return project annotation
   */
  @Nonnull
  Application getApplication();

  /**
   * Returns a name ot the project. For a directory-based project it's an arbitrary string specified by user at project creation
   * or later in a project settings. For a file-based project it's a name of a project file without extension.
   *
   * @return project name
   */
  @Nonnull
  String getName();

  /**
   * Returns a project base directory - a parent directory of a <code>.ipr</code> file or <code>.consulo</code> directory.<br/>
   * Returns a project base directory - a parent directory of a <code>.ipr</code> file or <code>.consulo</code> directory.<br/>
   * Returns <code>null</code> for default project.
   *
   * @return project base directory, or <code>null</code> for default project
   */
  @Nullable
  VirtualFile getBaseDir();

  /**
   * Returns a system-dependent path to a project base directory (see {@linkplain #getBaseDir()}).<br/>
   * Returns <code>null</code> for default project.
   *
   * @return a path to a project base directory, or <code>null</code> for default project
   */
  String getBasePath();

  /**
   * Returns project descriptor file:
   * <ul>
   * <li><code>path/to/project/project.ipr</code> - for file-based projects</li>
   * <li><code>path/to/project/.consulo/misc.xml</code> - for directory-based projects</li>
   * </ul>
   * Returns <code>null</code> for default project.
   *
   * @return project descriptor file, or null for default project
   */
  @Nullable
  VirtualFile getProjectFile();

  /**
   * Returns a system-dependent path to project descriptor file (see {@linkplain #getProjectFile()}).<br/>
   * Returns empty string (<code>""</code>) for default project.
   *
   * @return project descriptor file, or empty string for default project
   */
  @Nonnull
  String getProjectFilePath();

  /**
   * Returns presentable project path:
   * {@linkplain #getProjectFilePath()} for file-based projects, {@linkplain #getBasePath()} for directory-based ones.<br/>
   * * Returns <code>null</code> for default project.
   * <b>Note:</b> the word "presentable" here implies file system presentation, not a UI one.
   *
   * @return presentable project path
   */
  @Nullable
  String getPresentableUrl();

  /**
   * <p>Returns a workspace file:
   * <ul>
   * <li><code>path/to/project/project.iws</code> - for file-based projects</li>
   * <li><code>path/to/project/.consulo/workspace.xml</code> - for directory-based ones</li>
   * </ul>
   * Returns <code>null</code> for default project.
   *
   * @return workspace file, or null for default project
   */
  @Nullable
  VirtualFile getWorkspaceFile();

  @Nonnull
  String getLocationHash();

  void save();

  @Nonnull
  AsyncResult<Void> saveAsync(@Nonnull UIAccess uiAccess);

  boolean isOpen();

  boolean isInitialized();

  default boolean isModulesReady() {
    return true;
  }

  default boolean isDefault() {
    return false;
  }
}
