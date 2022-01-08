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
package com.intellij.openapi.vcs.impl;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.impl.projectlevelman.NewMappings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.project.ProjectKt;
import com.intellij.util.PathUtilRt;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * @author yole
 */
public abstract class DefaultVcsRootPolicy {
  public static DefaultVcsRootPolicy getInstance(Project project) {
    return ServiceManager.getService(project, DefaultVcsRootPolicy.class);
  }

  @Nonnull
  public abstract Collection<VirtualFile> getDefaultVcsRoots(@Nonnull NewMappings mappingList, @Nonnull String vcsName);

  public abstract boolean matchesDefaultMapping(@Nonnull VirtualFile file, final Object matchContext);

  @Nullable
  public abstract Object getMatchContext(final VirtualFile file);

  @javax.annotation.Nullable
  public abstract VirtualFile getVcsRootFor(@Nonnull VirtualFile file);

  @Nonnull
  public abstract Collection<VirtualFile> getDirtyRoots();

  public String getProjectConfigurationMessage(@Nonnull Project project) {
    boolean isDirectoryBased = ProjectKt.isDirectoryBased(project);
    final StringBuilder sb = new StringBuilder("Content roots of all modules");
    if (isDirectoryBased) {
      sb.append(", ");
    }
    else {
      sb.append(", and ");
    }
    sb.append("all immediate descendants of project base directory");
    if (isDirectoryBased) {
      sb.append(", and ");
      sb.append(PathUtilRt.getFileName(ProjectKt.getDirectoryStoreFile(project).getPresentableUrl())).append(" directory contents");
    }
    return sb.toString();
  }
}
