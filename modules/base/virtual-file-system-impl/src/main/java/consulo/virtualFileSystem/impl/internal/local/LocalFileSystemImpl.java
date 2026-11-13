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
package consulo.virtualFileSystem.impl.internal.local;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ExtensionImpl;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.impl.internal.BatchingFileSystem;
import consulo.virtualFileSystem.impl.internal.watcher.FileWatchServiceImpl;
import consulo.virtualFileSystem.impl.internal.watcher.WatchedFileSystem;
import consulo.virtualFileSystem.internal.DiskQueryRelay;
import consulo.virtualFileSystem.internal.PersistentFS;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

@ExtensionImpl(profiles = ComponentProfiles.PRODUCTION)
public final class LocalFileSystemImpl extends LocalFileSystemBase
    implements RefreshableFileSystem, VirtualFilePointerCapableFileSystem, BatchingFileSystem, WatchedFileSystem {
    private final ManagingFS myManagingFS;

    private final DiskQueryRelay<Pair<VirtualFile, @Nullable Set<String>>, Map<String, FileAttributes>> myChildrenAttrGetter =
        new DiskQueryRelay<>(pair -> listWithAttributesImpl(pair.first, pair.second));

    @Inject
    public LocalFileSystemImpl(ManagingFS managingFS) {
        myManagingFS = managingFS;
    }

    // region watching

    @Override
    public boolean isWatchedPath(Path path) {
        return path.getFileSystem() == FileSystems.getDefault();
    }

    @Override
    public String toVfsPath(Path path) {
        return FileUtil.toSystemIndependentName(path.toString());
    }

    // endregion

    @Override
    public void refreshWithoutFileWatcher(boolean asynchronous) {
        Runnable heavyRefresh = () -> {
            for (VirtualFile root : myManagingFS.getRoots(this)) {
                ((NewVirtualFile) root).markDirtyRecursively();
            }
            refresh(asynchronous);
        };

        FileWatchServiceImpl watchService = FileWatchServiceImpl.getInstanceIfImpl();
        if (asynchronous && watchService != null && watchService.isOperational(FileSystems.getDefault())) {
            RefreshQueue.getInstance().refresh(true, true, heavyRefresh, myManagingFS.getRoots(this));
        }
        else {
            heavyRefresh.run();
        }
    }

    @Override
    public Map<String, FileAttributes> listWithAttributes(VirtualFile dir, @Nullable Set<String> childrenNames) {
        return dir.isDirectory() ? myChildrenAttrGetter.accessDiskWithCheckCanceled(Pair.create(dir, childrenNames)) : Map.of();
    }

    private static Map<String, FileAttributes> listWithAttributesImpl(VirtualFile dir, @Nullable Set<String> filter) {
        try {
            var path = Path.of(toIoPath(dir));
            return listWithAttributesLocal(path, filter);
        }
        catch (AccessDeniedException | NoSuchFileException e) {
            LOG.debug(e);
        }
        catch (IOException | RuntimeException e) {
            LOG.warn(e);
        }
        return Map.of();
    }

    private static Map<String, FileAttributes> listWithAttributesLocal(Path dir, @Nullable Set<String> filter) throws IOException {
        var expectedSize = filter != null ? filter.size() : 10;
        // we must return a 'normal' (=case-sensitive) map from this method (see the [BatchingFileSystem#listWithAttributes] contract)
        Map<String, FileAttributes> childrenWithAttributes = new HashMap<>(expectedSize);

        PlatformNioHelper.visitDirectory(dir, filter, (file, ioAttributesHolder) -> {
            try {
                var attributes = FileAttributes.fromNio(file, ioAttributesHolder.get());
                childrenWithAttributes.put(file.getFileName().toString(), attributes);
            }
            catch (Exception e) {
                LOG.debug(e);
            }
            return true;
        });

        return childrenWithAttributes;
    }

    @Override
    public String toString() {
        return "LocalFileSystem";
    }

    @TestOnly
    public void cleanupForNextTest() {
        PersistentFS.getInstance().clearIdCache();
        FileWatchServiceImpl watchService = FileWatchServiceImpl.getInstanceIfImpl();
        if (watchService != null) {
            watchService.cleanupForNextTest();
        }
    }
}