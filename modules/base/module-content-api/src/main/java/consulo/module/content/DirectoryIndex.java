/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.module.content;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.application.util.query.Query;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Service(ComponentScope.PROJECT)
public abstract class DirectoryIndex {
  @Nonnull
  public static DirectoryIndex getInstance(Project project) {
    assert !project.isDefault() : "Must not call DirectoryIndex for default project";
    return project.getInstance(DirectoryIndex.class);
  }

  /**
   * The same as {@link #getInfoForFile} but works only for directories or file roots and returns {@code null} for directories
   * which aren't included in project content or libraries
   *
   * @deprecated use {@link #getInfoForFile(VirtualFile)} instead
   */
  @Deprecated
  public abstract DirectoryInfo getInfoForDirectory(@Nonnull VirtualFile dir);

  @Nonnull
  public abstract DirectoryInfo getInfoForFile(@Nonnull VirtualFile file);

  @Nullable
  public abstract ContentFolderTypeProvider getContentFolderType(@Nonnull DirectoryInfo info);

  @Nonnull
  public abstract Query<VirtualFile> getDirectoriesByPackageName(@Nonnull String packageName, boolean includeLibrarySources);

  @Nullable
  public abstract String getPackageName(@Nonnull VirtualFile dir);

  @Nonnull
  public abstract OrderEntry[] getOrderEntries(@Nonnull DirectoryInfo info);
}
