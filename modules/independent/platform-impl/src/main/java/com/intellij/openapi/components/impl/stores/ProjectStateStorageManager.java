/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.components.impl.stores;

import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.tracker.VirtualFileTracker;
import consulo.application.options.PathMacrosService;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ProjectStateStorageManager extends StateStorageManagerImpl {
  @NonNls
  protected static final String ROOT_TAG_NAME = "project";

  protected final ProjectImpl myProject;

  public ProjectStateStorageManager(@Nullable PathMacroManager pathMacroManager,
                                    @Nonnull VirtualFileTracker virtualFileTracker,
                                    @Nonnull LocalFileSystem localFileSystem,
                                    @Nonnull ProjectImpl project,
                                    @Nonnull PathMacrosService pathMacrosService) {
    super(pathMacroManager, ROOT_TAG_NAME, project.getMessageBus(), localFileSystem, virtualFileTracker, pathMacrosService);
    myProject = project;
  }

  @Nonnull
  @Override
  protected String getConfigurationMacro(boolean directorySpec) {
    return StoragePathMacros.PROJECT_CONFIG_DIR;
  }

  @Override
  protected StorageData createStorageData(@Nonnull String fileSpec, @Nonnull String filePath) {
    return new StorageData(ROOT_TAG_NAME, myPathMacrosService);
  }

  @Nonnull
  @Override
  protected StateStorage.Listener createStorageTopicListener() {
    return myProject.getMessageBus().syncPublisher(StateStorage.PROJECT_STORAGE_TOPIC);
  }
}
