// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
import consulo.project.RootsChangeRescanningInfo;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.util.List;

public abstract class ProjectRootManagerEx extends ProjectRootManager {
    public static ProjectRootManagerEx getInstanceEx(Project project) {
        return (ProjectRootManagerEx) getInstance(project);
    }

    /**
     * Invokes runnable surrounded by beforeRootsChange()/rootsChanged() callbacks
     * <p>
     * With {@code !fileTypes && fireEvents} indexes always make a full rescan.
     * <p>
     * @deprecated Use {@link ProjectRootManagerEx#makeRootsChange(Runnable, RootsChangeRescanningInfo)} when {@code fireEvents == true},
     * else just {@code runnable.run()}
     * <p>
     * {@link RootsChangeRescanningInfo} allows to limit the scope of rescanning. It may be configured
     * with {@link BuildableRootsChangeRescanningInfo}
     */
    @Deprecated
    public abstract void makeRootsChange(Runnable runnable, boolean fileTypes, boolean fireEvents);

    public abstract void makeRootsChange(Runnable runnable, RootsChangeRescanningInfo changes);

    public abstract AutoCloseable withRootsChange(RootsChangeRescanningInfo changes);

    public abstract List<VirtualFile> markRootsForRefresh();

    public abstract void clearScopesCachesForModules();

    public abstract void addOrderWithTracking(OrderEntryWithTracking orderEntry);

    public abstract void removeOrderWithTracking(OrderEntryWithTracking orderEntry);

    public static String extractLocalPath(String url) {
        String path = VirtualFileUtil.urlToPath(url);
        int jarSeparatorIndex = path.indexOf(ArchiveFileSystem.ARCHIVE_SEPARATOR);
        if (jarSeparatorIndex > 0) {
            return path.substring(0, jarSeparatorIndex);
        }
        return path;
    }
}
