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

package consulo.module.content.internal;

import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.OrderEntryWithTracking;
import consulo.project.Project;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

public abstract class ProjectRootManagerEx extends ProjectRootManager {
  public static ProjectRootManagerEx getInstanceEx(Project project) {
    return (ProjectRootManagerEx)getInstance(project);
  }

  // invokes runnable surrounded by beforeRootsChage()/rootsChanged() callbacks
  public abstract void makeRootsChange(@Nonnull Runnable runnable, boolean filetypes, boolean fireEvents);

  public abstract void markRootsForRefresh();

  public abstract void clearScopesCachesForModules();

  public abstract void addOrderWithTracking(@Nonnull OrderEntryWithTracking orderEntry);

  public abstract void removeOrderWithTracking(@Nonnull OrderEntryWithTracking orderEntry);

  public static String extractLocalPath(String url) {
    String path = VirtualFileUtil.urlToPath(url);
    int jarSeparatorIndex = path.indexOf(ArchiveFileSystem.ARCHIVE_SEPARATOR);
    if (jarSeparatorIndex > 0) {
      return path.substring(0, jarSeparatorIndex);
    }
    return path;
  }
}
