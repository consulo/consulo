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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.vcs.impl.projectlevelman.NewMappings;
import consulo.project.ProjectCoreUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.PathUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class DefaultVcsRootPolicy {
  public static DefaultVcsRootPolicy getInstance(Project project) {
    return ServiceManager.getService(project, DefaultVcsRootPolicy.class);
  }

  @Nonnull
  public abstract Collection<VirtualFile> getDefaultVcsRoots(@Nonnull NewMappings mappingList, @Nonnull String vcsName);

  public abstract boolean matchesDefaultMapping(@Nonnull VirtualFile file, final Object matchContext);

  @Nullable
  public abstract Object getMatchContext(final VirtualFile file);

  @jakarta.annotation.Nullable
  public abstract VirtualFile getVcsRootFor(@Nonnull VirtualFile file);

  @Nonnull
  public abstract Collection<VirtualFile> getDirtyRoots();

  public String getProjectConfigurationMessage(@Nonnull Project project) {
    boolean isDirectoryBased = ProjectCoreUtil.isDirectoryBased(project);
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
      sb.append(PathUtil.getFileName(ProjectCoreUtil.getDirectoryStoreFile(project).getPresentableUrl())).append(" directory contents");
    }
    return sb.toString();
  }
}
