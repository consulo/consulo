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
package consulo.virtualFileSystem.impl.internal.platform;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.platform.PlatformFileWatch;
import consulo.platform.PlatformFileWatchListener;
import consulo.platform.PlatformNioFileSystem;
import consulo.util.lang.Couple;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.internal.VfsImplUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Watches files of other machines for {@link PlatformVirtualFileSystem} - the roots come as watch requests of the
 * {@link LocalFileSystem}, the changes from the {@link PlatformNioFileSystem#watch watch} of each machine. A changed file is
 * marked dirty and refreshed, as the local file watcher does.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
final class PlatformFileWatcher {
    private static final Logger LOG = Logger.getInstance(PlatformFileWatcher.class);

    // changes of a while are refreshed together
    private static final long REFRESH_DELAY_MS = 100;

    static final class Request implements LocalFileSystem.WatchRequest {
        private final String myPath;
        private final boolean myRecursive;

        Request(String path, boolean recursive) {
            myPath = path;
            myRecursive = recursive;
        }

        @Override
        public String getRootPath() {
            return myPath;
        }

        @Override
        public boolean isToWatchRecursively() {
            return myRecursive;
        }

        @Override
        public String toString() {
            return myPath;
        }
    }

    private record Watch(FileSystem fileSystem, @Nullable PlatformFileWatch watch) {
    }

    private final PlatformVirtualFileSystem myFileSystem;
    private final Object myLock = new Object();
    private final Set<Request> myRequests = new HashSet<>();
    // platform id -> the watch of its machine; null watch - the machine does not report changes
    private final Map<String, Watch> myWatches = new HashMap<>();
    private final AtomicBoolean myRefreshScheduled = new AtomicBoolean();

    PlatformFileWatcher(PlatformVirtualFileSystem fileSystem) {
        myFileSystem = fileSystem;
    }

    Set<LocalFileSystem.WatchRequest> replaceWatchedRoots(Collection<? extends LocalFileSystem.WatchRequest> toRemove,
                                                          Collection<String> recursiveRoots,
                                                          Collection<String> flatRoots) {
        Set<LocalFileSystem.WatchRequest> added = new HashSet<>();
        synchronized (myLock) {
            for (LocalFileSystem.WatchRequest request : toRemove) {
                if (request instanceof Request platformRequest) {
                    myRequests.remove(platformRequest);
                }
            }
            for (String root : recursiveRoots) {
                Request request = new Request(root, true);
                myRequests.add(request);
                added.add(request);
            }
            for (String root : flatRoots) {
                Request request = new Request(root, false);
                myRequests.add(request);
                added.add(request);
            }
            update();
        }
        return added;
    }

    /**
     * A machine became known (or its connection was made again) - its roots can be watched now.
     */
    void fileSystemRegistered() {
        synchronized (myLock) {
            update();
        }
    }

    private void update() {
        Map<String, List<Request>> requestsByPlatform = new HashMap<>();
        for (Request request : myRequests) {
            requestsByPlatform.computeIfAbsent(myFileSystem.extractRootPath(request.getRootPath()), id -> new ArrayList<>()).add(request);
        }

        for (Iterator<Map.Entry<String, Watch>> iterator = myWatches.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Watch> entry = iterator.next();
            FileSystem fileSystem = myFileSystem.getRegisteredFileSystem(entry.getKey());
            if (!requestsByPlatform.containsKey(entry.getKey()) || fileSystem != entry.getValue().fileSystem() || !fileSystem.isOpen()) {
                PlatformFileWatch watch = entry.getValue().watch();
                if (watch != null) {
                    watch.close();
                }
                iterator.remove();
            }
        }

        for (Map.Entry<String, List<Request>> entry : requestsByPlatform.entrySet()) {
            FileSystem fileSystem = myFileSystem.getRegisteredFileSystem(entry.getKey());
            if (!(fileSystem instanceof PlatformNioFileSystem platformFileSystem) || !fileSystem.isOpen()) {
                // not connected yet - watched once it is
                continue;
            }
            Watch watch = myWatches.get(entry.getKey());
            if (watch == null) {
                PlatformFileWatch platformWatch;
                try {
                    platformWatch = platformFileSystem.watch(new Listener());
                }
                catch (UnsupportedOperationException e) {
                    LOG.info(e.getMessage());
                    platformWatch = null;
                }
                watch = new Watch(fileSystem, platformWatch);
                myWatches.put(entry.getKey(), watch);
            }
            if (watch.watch() != null) {
                setRoots(watch.watch(), entry.getValue());
            }
        }
    }

    private void setRoots(PlatformFileWatch watch, List<Request> requests) {
        List<Path> recursiveRoots = new ArrayList<>();
        List<Path> flatRoots = new ArrayList<>();
        for (Request request : requests) {
            Path path = myFileSystem.toNioPath(request.getRootPath());
            if (path != null) {
                (request.isToWatchRecursively() ? recursiveRoots : flatRoots).add(path);
            }
        }
        // a root under a recursive one is watched by it already
        recursiveRoots.removeIf(root -> isUnderOther(root, recursiveRoots));
        flatRoots.removeIf(root -> isUnderOther(root, recursiveRoots));
        watch.setRoots(new ArrayList<>(new HashSet<>(recursiveRoots)), new ArrayList<>(new HashSet<>(flatRoots)));
    }

    private static boolean isUnderOther(Path root, List<Path> recursiveRoots) {
        for (Path recursiveRoot : recursiveRoots) {
            if (!recursiveRoot.equals(root) && root.startsWith(recursiveRoot)) {
                return true;
            }
        }
        return false;
    }

    private void markDirty(Path path, boolean recursive) {
        Couple<NewVirtualFile> pair = VfsImplUtil.findCachedFileByPath(myFileSystem, myFileSystem.toVfsPath(path));
        if (pair.first != null) {
            if (recursive) {
                pair.first.markDirtyRecursively();
            }
            else {
                pair.first.markDirty();
            }
        }
        else if (pair.second != null) {
            // not known yet - its nearest known parent lists its children again
            pair.second.markDirty();
        }
    }

    private void scheduleRefresh() {
        if (myRefreshScheduled.compareAndSet(false, true)) {
            AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
                myRefreshScheduled.set(false);
                myFileSystem.refresh(true);
            }, REFRESH_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private class Listener implements PlatformFileWatchListener {
        @Override
        public void changed(Path path) {
            markDirty(path, false);
            scheduleRefresh();
        }

        @Override
        public void createdOrDeleted(Path path) {
            markDirty(path, false);
            Path parent = path.getParent();
            if (parent != null) {
                markDirty(parent, false);
            }
            scheduleRefresh();
        }

        @Override
        public void reset(Path root) {
            markDirty(root, true);
            scheduleRefresh();
        }

        @Override
        public void failed(Path root, String message) {
            LOG.info("Changes of " + root + " are not watched: " + message);
        }
    }
}
