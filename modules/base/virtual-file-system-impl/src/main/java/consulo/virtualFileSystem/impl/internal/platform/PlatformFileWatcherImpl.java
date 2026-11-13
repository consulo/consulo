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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ExtensionImpl;
import consulo.logging.Logger;
import consulo.platform.PlatformFileWatch;
import consulo.platform.PlatformFileWatchListener;
import consulo.platform.PlatformNioFileSystem;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.impl.internal.watcher.FileWatcherNotificationSink;
import consulo.virtualFileSystem.impl.internal.watcher.PluggableFileWatcher;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Watches files of other machines - through the {@link PlatformNioFileSystem#watch watch} of each machine (its agent). The roots of
 * a machine which does not report changes are watched manually (rescanned on refresh).
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@ExtensionImpl(profiles = ComponentProfiles.PRODUCTION)
public class PlatformFileWatcherImpl extends PluggableFileWatcher {
    private static final Logger LOG = Logger.getInstance(PlatformFileWatcherImpl.class);

    private record Watch(@Nullable PlatformFileWatch watch) {
    }

    private final Object myLock = new Object();
    // the file system of a machine -> its watch; null watch - the machine does not report changes
    private final Map<FileSystem, Watch> myWatches = new HashMap<>();
    private volatile @Nullable FileWatcherNotificationSink mySink;

    @Override
    public boolean isApplicable(FileSystem fileSystem) {
        return fileSystem instanceof PlatformNioFileSystem;
    }

    @Override
    public void initialize(ManagingFS managingFS, FileWatcherNotificationSink notificationSink) {
        mySink = notificationSink;
    }

    @Override
    public void dispose() {
        synchronized (myLock) {
            for (Watch watch : myWatches.values()) {
                if (watch.watch() != null) {
                    watch.watch().close();
                }
            }
            myWatches.clear();
        }
        mySink = null;
    }

    @Override
    public boolean isOperational() {
        return mySink != null;
    }

    @Override
    public boolean isSettingRoots() {
        return false;
    }

    @Override
    public void setWatchRoots(List<Path> recursive, List<Path> flat) {
        FileWatcherNotificationSink sink = mySink;
        if (sink == null) {
            return;
        }

        Map<FileSystem, List<Path>> recursiveByFileSystem = groupByFileSystem(recursive);
        Map<FileSystem, List<Path>> flatByFileSystem = groupByFileSystem(flat);
        List<Path> manualRoots = new ArrayList<>();
        synchronized (myLock) {
            for (Iterator<Map.Entry<FileSystem, Watch>> iterator = myWatches.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<FileSystem, Watch> entry = iterator.next();
                FileSystem fileSystem = entry.getKey();
                if (!fileSystem.isOpen() || (!recursiveByFileSystem.containsKey(fileSystem) && !flatByFileSystem.containsKey(fileSystem))) {
                    PlatformFileWatch watch = entry.getValue().watch();
                    if (watch != null) {
                        watch.close();
                    }
                    iterator.remove();
                }
            }

            Map<FileSystem, Boolean> fileSystems = new LinkedHashMap<>();
            recursiveByFileSystem.keySet().forEach(fileSystem -> fileSystems.put(fileSystem, true));
            flatByFileSystem.keySet().forEach(fileSystem -> fileSystems.put(fileSystem, true));
            for (FileSystem fileSystem : fileSystems.keySet()) {
                List<Path> recursiveRoots = recursiveByFileSystem.getOrDefault(fileSystem, List.of());
                List<Path> flatRoots = flatByFileSystem.getOrDefault(fileSystem, List.of());
                Watch watch = myWatches.computeIfAbsent(fileSystem, this::startWatch);
                if (watch.watch() != null) {
                    watch.watch().setRoots(recursiveRoots, flatRoots);
                }
                else {
                    manualRoots.addAll(recursiveRoots);
                    manualRoots.addAll(flatRoots);
                }
            }
        }
        sink.notifyManualWatchRoots(manualRoots);
    }

    private Watch startWatch(FileSystem fileSystem) {
        try {
            return new Watch(((PlatformNioFileSystem) fileSystem).watch(new Listener()));
        }
        catch (UnsupportedOperationException e) {
            LOG.info(e.getMessage());
            return new Watch(null);
        }
    }

    private static Map<FileSystem, List<Path>> groupByFileSystem(List<Path> roots) {
        Map<FileSystem, List<Path>> result = new LinkedHashMap<>();
        for (Path root : roots) {
            result.computeIfAbsent(root.getFileSystem(), fileSystem -> new ArrayList<>()).add(root);
        }
        return result;
    }

    @Override
    public void startup() {
    }

    @Override
    public void shutdown() {
    }

    private class Listener implements PlatformFileWatchListener {
        @Override
        public void changed(Path path) {
            FileWatcherNotificationSink sink = mySink;
            if (sink != null) {
                sink.notifyDirtyPath(path);
            }
        }

        @Override
        public void createdOrDeleted(Path path) {
            FileWatcherNotificationSink sink = mySink;
            if (sink != null) {
                sink.notifyPathCreatedOrDeleted(path);
            }
        }

        @Override
        public void reset(Path root) {
            FileWatcherNotificationSink sink = mySink;
            if (sink != null) {
                sink.notifyReset(root);
            }
        }

        @Override
        public void failed(Path root, String message) {
            LOG.info("Changes of " + root + " are not watched: " + message);
        }
    }
}
