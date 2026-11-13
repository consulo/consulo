// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.watcher;

import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.extension.ExtensionPoint;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.VirtualFileSystemInternalHelper;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Watches roots of any file system - each root goes to the {@link PluggableFileWatcher pluggable watchers} applicable to its
 * file system, their changes are collected as dirty paths.
 *
 * @author max
 */
public class FileWatcher {
    private static final Logger LOG = Logger.getInstance(FileWatcher.class);

    static class DirtyPaths {
        final Set<Path> dirtyPaths = new HashSet<>();
        final Set<Path> dirtyPathsRecursive = new HashSet<>();
        final Set<Path> dirtyDirectories = new HashSet<>();

        static final DirtyPaths EMPTY = new DirtyPaths();

        boolean isEmpty() {
            return dirtyPaths.isEmpty() && dirtyPathsRecursive.isEmpty() && dirtyDirectories.isEmpty();
        }

        private void addDirtyPath(Path path) {
            if (!dirtyPathsRecursive.contains(path)) {
                dirtyPaths.add(path);
            }
        }

        private void addDirtyPathRecursive(Path path) {
            dirtyPaths.remove(path);
            dirtyPathsRecursive.add(path);
        }
    }

    private final ManagingFS myManagingFS;
    private final ExtensionPoint<PluggableFileWatcher> myWatchers;
    // every watcher reports to its own sink - the manual roots and the failures are of that watcher
    private final Map<PluggableFileWatcher, MyFileWatcherNotificationSink> mySinks = new ConcurrentHashMap<>();
    private final AtomicBoolean myFailureShown = new AtomicBoolean(false);
    private final ExecutorService myFileWatcherExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("File Watcher", 1);
    private final AtomicReference<Future<?>> myLastTask = new AtomicReference<>(null);

    private final Object myLock = new Object();
    private DirtyPaths myDirtyPaths = new DirtyPaths();
    private volatile CanonicalPathMap myPathMap = new CanonicalPathMap();

    FileWatcher(Application application, ManagingFS managingFS) {
        myManagingFS = managingFS;
        myWatchers = application.getExtensionPoint(PluggableFileWatcher.class);

        myFileWatcherExecutor.execute(() -> {
            for (PluggableFileWatcher watcher : myWatchers) {
                MyFileWatcherNotificationSink sink = new MyFileWatcherNotificationSink(watcher);
                mySinks.put(watcher, sink);
                watcher.initialize(myManagingFS, sink);
            }
        });
    }

    public void dispose() {
        myFileWatcherExecutor.shutdown();

        Future<?> lastTask = myLastTask.get();
        if (lastTask != null) {
            lastTask.cancel(false);
        }

        try {
            myFileWatcherExecutor.awaitTermination(1, TimeUnit.HOURS);
        }
        catch (InterruptedException e) {
            LOG.error(e);
        }

        myWatchers.forEach(PluggableFileWatcher::dispose);
    }

    public boolean isOperational() {
        for (PluggableFileWatcher watcher : myWatchers) {
            if (watcher.isOperational()) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the changes of the file system are watched.
     */
    public boolean isOperational(FileSystem fileSystem) {
        for (PluggableFileWatcher watcher : myWatchers) {
            if (watcher.isApplicable(fileSystem) && watcher.isOperational()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSettingRoots() {
        Future<?> lastTask = myLastTask.get();  // a new task may come after the read, but this seem to be an acceptable race
        if (lastTask != null && !lastTask.isDone()) {
            return true;
        }
        for (PluggableFileWatcher watcher : myWatchers) {
            if (watcher.isSettingRoots()) {
                return true;
            }
        }
        return false;
    }

    DirtyPaths getDirtyPaths() {
        DirtyPaths dirtyPaths = DirtyPaths.EMPTY;

        synchronized (myLock) {
            if (!myDirtyPaths.isEmpty()) {
                dirtyPaths = myDirtyPaths;
                myDirtyPaths = new DirtyPaths();
            }
        }

        myWatchers.forEach(PluggableFileWatcher::resetChangedPaths);

        return dirtyPaths;
    }

    /**
     * The roots no watcher can watch - they are refreshed by scanning. Every watcher reports the roots of its file systems.
     */
    public Collection<Path> getManualWatchRoots() {
        Set<Path> result = new HashSet<>();
        for (MyFileWatcherNotificationSink sink : mySinks.values()) {
            result.addAll(sink.getManualWatchRoots());
        }
        return result;
    }

    /**
     * Clients should take care of not calling this method in parallel.
     */
    void setWatchRoots(List<Path> recursive, List<Path> flat) {
        Future<?> prevTask = myLastTask.getAndSet(myFileWatcherExecutor.submit(() -> {
            try {
                CanonicalPathMap pathMap = new CanonicalPathMap(recursive, flat);

                myPathMap = pathMap;
                for (MyFileWatcherNotificationSink sink : mySinks.values()) {
                    sink.resetManualWatchRoots();
                }

                for (PluggableFileWatcher watcher : myWatchers) {
                    watcher.setWatchRoots(
                        applicableRoots(watcher, pathMap.getCanonicalRecursiveWatchRoots()),
                        applicableRoots(watcher, pathMap.getCanonicalFlatWatchRoots())
                    );
                }
            }
            catch (RuntimeException | Error e) {
                LOG.error(e);
            }
        }));
        if (prevTask != null) {
            prevTask.cancel(false);
        }
    }

    private static List<Path> applicableRoots(PluggableFileWatcher watcher, List<Path> roots) {
        List<Path> result = new ArrayList<>(roots.size());
        for (Path root : roots) {
            if (watcher.isApplicable(root.getFileSystem())) {
                result.add(root);
            }
        }
        return result;
    }

    public void notifyOnFailure(LocalizeValue cause) {
        LOG.warn(cause.get());

        if (myFailureShown.compareAndSet(false, true)) {
            VirtualFileSystemInternalHelper.getInstance().notifyAboutSlowFileWatcher(cause);
        }
    }

    private class MyFileWatcherNotificationSink implements FileWatcherNotificationSink {
        private final PluggableFileWatcher myWatcher;
        private volatile List<Collection<Path>> myManualWatchRoots = Collections.emptyList();

        private MyFileWatcherNotificationSink(PluggableFileWatcher watcher) {
            myWatcher = watcher;
        }

        void resetManualWatchRoots() {
            myManualWatchRoots = Lists.newLockFreeCopyOnWriteList();
        }

        Collection<Path> getManualWatchRoots() {
            Set<Path> result = null;
            for (Collection<Path> roots : myManualWatchRoots) {
                if (result == null) {
                    result = new HashSet<>(roots);
                }
                else {
                    result.retainAll(roots);
                }
            }
            return result != null ? result : Collections.emptyList();
        }

        @Override
        public void notifyManualWatchRoots(Collection<Path> roots) {
            myManualWatchRoots.add(roots.isEmpty() ? Collections.emptySet() : new HashSet<>(roots));
            notifyOnEvent(OTHER);
        }

        @Override
        public void notifyMapping(Collection<? extends Pair<Path, Path>> mapping) {
            if (!mapping.isEmpty()) {
                myPathMap.addMapping(mapping);
            }
            notifyOnEvent(OTHER);
        }

        @Override
        public void notifyDirtyPath(Path path) {
            Collection<Path> paths = myPathMap.getWatchedPaths(path, true);
            if (!paths.isEmpty()) {
                synchronized (myLock) {
                    for (Path eachPath : paths) {
                        myDirtyPaths.addDirtyPath(eachPath);
                    }
                }
            }
            notifyOnEvent(path.toString());
        }

        @Override
        public void notifyPathCreatedOrDeleted(Path path) {
            Collection<Path> paths = myPathMap.getWatchedPaths(path, true);
            if (!paths.isEmpty()) {
                synchronized (myLock) {
                    for (Path p : paths) {
                        myDirtyPaths.addDirtyPathRecursive(p);
                        Path parentPath = p.getParent();
                        if (parentPath != null) {
                            myDirtyPaths.addDirtyPath(parentPath);
                        }
                    }
                }
            }
            notifyOnEvent(path.toString());
        }

        @Override
        public void notifyDirtyDirectory(Path path) {
            Collection<Path> paths = myPathMap.getWatchedPaths(path, false);
            if (!paths.isEmpty()) {
                synchronized (myLock) {
                    myDirtyPaths.dirtyDirectories.addAll(paths);
                }
            }
            notifyOnEvent(path.toString());
        }

        @Override
        public void notifyDirtyPathRecursive(Path path) {
            Collection<Path> paths = myPathMap.getWatchedPaths(path, false);
            if (!paths.isEmpty()) {
                synchronized (myLock) {
                    for (Path each : paths) {
                        myDirtyPaths.addDirtyPathRecursive(each);
                    }
                }
            }
            notifyOnEvent(path.toString());
        }

        @Override
        public void notifyReset(@Nullable Path path) {
            if (path != null) {
                synchronized (myLock) {
                    myDirtyPaths.addDirtyPathRecursive(path);
                }
            }
            else {
                List<Path> roots = new ArrayList<>();
                if (myWatcher.isApplicable(FileSystems.getDefault())) {
                    // every local root - as the native watcher may have missed anything
                    for (VirtualFile root : myManagingFS.getLocalRoots()) {
                        try {
                            roots.add(Path.of(root.getPresentableUrl()));
                        }
                        catch (InvalidPathException e) {
                            LOG.debug(e);
                        }
                    }
                }
                for (Path root : myPathMap.getRecursiveWatchRoots()) {
                    if (myWatcher.isApplicable(root.getFileSystem()) && root.getFileSystem() != FileSystems.getDefault()) {
                        roots.add(root);
                    }
                }
                synchronized (myLock) {
                    for (Path root : roots) {
                        myDirtyPaths.addDirtyPathRecursive(root);
                    }
                }
            }
            notifyOnEvent(RESET);
        }

        @Override
        public void notifyUserOnFailure(LocalizeValue cause) {
            notifyOnFailure(cause);
        }
    }

    //<editor-fold desc="Test stuff.">
    public static final String RESET = "(reset)";
    public static final String OTHER = "(other)";

    private volatile Consumer<? super String> myTestNotifier;

    private void notifyOnEvent(String path) {
        Consumer<? super String> notifier = myTestNotifier;
        if (notifier != null) {
            notifier.accept(path);
        }
    }

    @TestOnly
    public void startup(@Nullable Consumer<? super String> notifier) throws Exception {
        myTestNotifier = notifier;
        myFileWatcherExecutor.submit(() -> {
            for (PluggableFileWatcher watcher : myWatchers) {
                watcher.startup();
            }
            return null;
        }).get();
    }

    @TestOnly
    public void shutdown() throws Exception {
        myFileWatcherExecutor.submit(() -> {
            for (PluggableFileWatcher watcher : myWatchers) {
                watcher.shutdown();
            }
            myTestNotifier = null;
            return null;
        }).get();
    }
    //</editor-fold>
}
