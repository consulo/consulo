/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@State(name = "RecentProjectsManager", storages = {@Storage(value = "recentProjects.xml", roamingType = RoamingType.DISABLED)})
public class RecentDirectoryProjectsManager extends RecentProjectsManagerBase {
  @Inject
  public RecentDirectoryProjectsManager(Application application) {
    super(application);
  }

  @Override
  @Nullable
  protected String getProjectPath(@Nonnull Project project) {
    final VirtualFile baseDirVFile = project.getBaseDir();
    return baseDirVFile != null ? FileUtil.toSystemDependentName(baseDirVFile.getPath()) : null;
  }
}
