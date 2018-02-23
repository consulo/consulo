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

import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.project.impl.ProjectImpl;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

class ProjectStateStorageManager extends StateStorageManagerImpl {
  protected final ProjectImpl myProject;
  @NonNls protected static final String ROOT_TAG_NAME = "project";

  public ProjectStateStorageManager(final TrackingPathMacroSubstitutor macroSubstitutor, ProjectImpl project) {
    super(macroSubstitutor, ROOT_TAG_NAME, project, project.getPicoContainer());
    myProject = project;
  }

  @Nonnull
  @Override
  protected String getConfigurationMacro(boolean directorySpec) {
    return StoragePathMacros.PROJECT_CONFIG_DIR;
  }

  @Override
  protected StorageData createStorageData(@Nonnull String fileSpec, @Nonnull String filePath) {
    return new StorageData(ROOT_TAG_NAME);
  }

  @Nonnull
  @Override
  protected StateStorage.Listener createStorageTopicListener() {
    return myProject.getMessageBus().syncPublisher(StateStorage.PROJECT_STORAGE_TOPIC);
  }
}
