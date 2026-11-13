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
package consulo.virtualFileSystem.impl.internal.watcher;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.util.lang.Couple;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.FileWatchService;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.internal.VfsImplUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Watching roots of any file system - the {@link FileWatcher} passes each root to the watchers of its file system, the changes are
 * marked dirty in the {@link WatchedFileSystem virtual file system} of their paths.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.PRODUCTION)
public class FileWatchServiceImpl implements FileWatchService, Disposable {
    private static final Logger LOG = Logger.getInstance(FileWatchServiceImpl.class);

    private static final int STATUS_UPDATE_PERIOD = 1000;

    private static class WatchRequestImpl implements WatchRequest {
        private final Path myRoot;
        private final boolean myWatchRecursively;
        private boolean myDominated;

        WatchRequestImpl(Path root, boolean watchRecursively) {
            myRoot = root;
            myWatchRecursively = watchRecursively;
        }

        @Override
        public Path getRoot() {
            return myRoot;
        }

        @Override
        public boolean isToWatchRecursively() {
            return myWatchRecursively;
        }

        @Override
        public String toString() {
            return myRoot.toString();
        }
    }

    private static class TreeNode {
        private @Nullable WatchRequestImpl watchRequest;
        // a root, then the names of the path - paths compare as their file system does
        private final Map<Path, TreeNode> nodes = new HashMap<>(1);
    }

    private final Application myApplication;
    private final FileWatcher myWatcher;

    private final Object myLock = new Object();
    private final Set<WatchRequestImpl> myRootsToWatch = new HashSet<>();
    private @Nullable TreeNode myNormalizedTree;

    private Future<?> myWatcherFuture;

    @Inject
    public FileWatchServiceImpl(Application application, ApplicationConcurrency applicationConcurrency, ManagingFS managingFS) {
        myApplication = application;
        myWatcher = new FileWatcher(application, managingFS);

        myWatcherFuture = applicationConcurrency.getScheduledExecutorService().scheduleWithFixedDelay(() -> {
            if (!application.isDisposed()) {
                storeRefreshStatusToFiles();
            }
        }, STATUS_UPDATE_PERIOD, STATUS_UPDATE_PERIOD, TimeUnit.MILLISECONDS);
    }

    public static @Nullable FileWatchServiceImpl getInstanceIfImpl() {
        return FileWatchService.getInstance() instanceof FileWatchServiceImpl impl ? impl : null;
    }

    @Override
    public void dispose() {
        myWatcher.dispose();

        myWatcherFuture.cancel(false);
        myWatcherFuture = CompletableFuture.completedFuture(null);
    }

    public FileWatcher getFileWatcher() {
        return myWatcher;
    }

    /**
     * True when the changes of the file system are watched.
     */
    public boolean isOperational(FileSystem fileSystem) {
        return myWatcher.isOperational(fileSystem);
    }

    // region watch requests

    @Override
    public Set<WatchRequest> replaceWatchedRoots(Collection<WatchRequest> watchRequests,
                                                 @Nullable Collection<Path> recursiveRoots,
                                                 @Nullable Collection<Path> flatRoots) {
        Collection<Path> recursive = ObjectUtil.notNull(recursiveRoots, Collections.emptyList());
        Collection<Path> flat = ObjectUtil.notNull(flatRoots, Collections.emptyList());

        Set<WatchRequest> result = new HashSet<>();
        synchronized (myLock) {
            boolean update = doAddRootsToWatch(recursive, flat, result) | doRemoveWatchedRoots(watchRequests);
            if (update) {
                myNormalizedTree = null;
                setUpFileWatcher();
            }
        }
        return result;
    }

    private boolean doAddRootsToWatch(Collection<Path> recursiveRoots, Collection<Path> flatRoots, Set<WatchRequest> results) {
        boolean update = false;
        for (Path root : recursiveRoots) {
            update |= addRootToWatch(root, true, results);
        }
        for (Path root : flatRoots) {
            update |= addRootToWatch(root, false, results);
        }
        return update;
    }

    private boolean addRootToWatch(Path root, boolean recursively, Set<WatchRequest> results) {
        if (!root.isAbsolute()) {
            LOG.warn(new IllegalArgumentException("Invalid path: " + root));
            return false;
        }
        WatchRequestImpl request = new WatchRequestImpl(root.normalize(), recursively);
        boolean alreadyWatched = isAlreadyWatched(request);

        request.myDominated = alreadyWatched;
        myRootsToWatch.add(request);
        results.add(request);

        return !alreadyWatched;
    }

    private boolean doRemoveWatchedRoots(Collection<WatchRequest> watchRequests) {
        boolean update = false;
        for (WatchRequest watchRequest : watchRequests) {
            WatchRequestImpl impl = (WatchRequestImpl) watchRequest;
            boolean wasWatched = myRootsToWatch.remove(impl) && !impl.myDominated;
            update |= wasWatched;
        }
        return update;
    }

    private void setUpFileWatcher() {
        if (!myApplication.isDisposeInProgress() && myWatcher.isOperational()) {
            List<Path> recursiveRoots = new ArrayList<>();
            List<Path> flatRoots = new ArrayList<>();

            for (WatchRequestImpl request : normalizeRootsForRefresh()) {
                (request.isToWatchRecursively() ? recursiveRoots : flatRoots).add(request.getRoot());
            }

            myWatcher.setWatchRoots(recursiveRoots, flatRoots);
        }
    }

    private List<WatchRequestImpl> normalizeRootsForRefresh() {
        List<WatchRequestImpl> result = new ArrayList<>();

        synchronized (myLock) {
            TreeNode rootNode = new TreeNode();
            for (WatchRequestImpl request : myRootsToWatch) {
                request.myDominated = false;

                TreeNode currentNode = rootNode;
                MainLoop:
                for (Path subPath : splitPath(request.getRoot())) {
                    TreeNode nextNode = currentNode.nodes.get(subPath);
                    if (nextNode != null) {
                        currentNode = nextNode;
                        if (currentNode.watchRequest != null && currentNode.watchRequest.isToWatchRecursively()) {
                            // a parent path of this request is already being watched recursively - do not need to add this one
                            request.myDominated = true;
                            break MainLoop;
                        }
                    }
                    else {
                        TreeNode newNode = new TreeNode();
                        currentNode.nodes.put(subPath, newNode);
                        currentNode = newNode;
                    }
                }
                if (currentNode.watchRequest == null) {
                    currentNode.watchRequest = request;
                }
                else {
                    // we already have a watchRequest configured - select the better of the two
                    if (!currentNode.watchRequest.isToWatchRecursively()) {
                        currentNode.watchRequest.myDominated = true;
                        currentNode.watchRequest = request;
                    }
                    else {
                        request.myDominated = true;
                    }
                }

                if (currentNode.watchRequest.isToWatchRecursively() && !currentNode.nodes.isEmpty()) {
                    // since we are watching this node recursively, we can remove it's children
                    visitTree(currentNode, node -> {
                        if (node.watchRequest != null) {
                            node.watchRequest.myDominated = true;
                        }
                    });
                    currentNode.nodes.clear();
                }
            }

            visitTree(rootNode, node -> {
                if (node.watchRequest != null) {
                    result.add(node.watchRequest);
                }
            });
            myNormalizedTree = rootNode;
        }

        return result;
    }

    /**
     * The root of the path (of its file system - roots of different file systems never equal), then its names.
     */
    private static List<Path> splitPath(Path path) {
        List<Path> parts = new ArrayList<>(path.getNameCount() + 1);
        Path root = path.getRoot();
        if (root != null) {
            parts.add(root);
        }
        for (Path name : path) {
            parts.add(name);
        }
        return parts;
    }

    private static void visitTree(TreeNode rootNode, Consumer<TreeNode> consumer) {
        for (TreeNode node : rootNode.nodes.values()) {
            consumer.accept(node);
            visitTree(node, consumer);
        }
    }

    private boolean isAlreadyWatched(WatchRequestImpl request) {
        if (myNormalizedTree == null) {
            normalizeRootsForRefresh();
        }

        TreeNode currentNode = Objects.requireNonNull(myNormalizedTree);
        for (Path subPath : splitPath(request.getRoot())) {
            TreeNode nextNode = currentNode.nodes.get(subPath);
            if (nextNode == null) {
                return false;
            }
            currentNode = nextNode;
            if (currentNode.watchRequest != null && currentNode.watchRequest.isToWatchRecursively()) {
                return true;
            }
        }
        // if we reach here it means that the exact path is already present in the graph -
        // then this request is assumed to be present only if it is not being watched recursively
        return !request.isToWatchRecursively() && currentNode.watchRequest != null;
    }

    // endregion

    // region dirty files

    private void storeRefreshStatusToFiles() {
        if (myWatcher.isOperational()) {
            markDirtyPaths(myWatcher.getDirtyPaths());
        }
    }

    private void markDirtyPaths(FileWatcher.DirtyPaths dirtyPaths) {
        if (dirtyPaths.isEmpty()) {
            return;
        }
        //TODO RC: this method is sometimes called without RA => it makes some VFS intermediate states visible -- e.g.
        //         the state there file is already marked as removed, but is not yet removed from it's parent.children
        //         list => causes FileDeletedException during path resolution.
        Set<WatchedFileSystem> marked = new LinkedHashSet<>();
        markPathsDirty(dirtyPaths.dirtyPaths, marked);
        markFlatDirsDirty(dirtyPaths.dirtyDirectories, marked);
        markRecursiveDirsDirty(dirtyPaths.dirtyPathsRecursive, marked);
        for (WatchedFileSystem fileSystem : marked) {
            fileSystem.dirtyFilesMarked();
        }
    }

    private @Nullable WatchedFileSystem findFileSystem(Path path) {
        for (VirtualFileSystem fileSystem : myApplication.getExtensionPoint(VirtualFileSystem.class)) {
            if (fileSystem instanceof WatchedFileSystem watched && watched.isWatchedPath(path)) {
                return watched;
            }
        }
        return null;
    }

    private Couple<@Nullable NewVirtualFile> findCachedFile(Path path, Set<WatchedFileSystem> marked) {
        WatchedFileSystem fileSystem = findFileSystem(path);
        if (fileSystem == null) {
            return Couple.of(null, null);
        }
        marked.add(fileSystem);
        return VfsImplUtil.findCachedFileByPath(fileSystem.asFileSystem(), fileSystem.toVfsPath(path));
    }

    private void markPathsDirty(Iterable<Path> dirtyPaths, Set<WatchedFileSystem> marked) {
        for (Path dirtyPath : dirtyPaths) {
            ProgressManager.checkCanceled();

            NewVirtualFile file = findCachedFile(dirtyPath, marked).first;
            if (file != null) {
                file.markDirty();
            }
        }
    }

    private void markFlatDirsDirty(Iterable<Path> dirtyPaths, Set<WatchedFileSystem> marked) {
        for (Path dirtyPath : dirtyPaths) {
            ProgressManager.checkCanceled();

            Couple<NewVirtualFile> pair = findCachedFile(dirtyPath, marked);
            if (pair.first != null) {
                pair.first.markDirty();

                for (VirtualFile child : pair.first.getCachedChildren()) {
                    ((NewVirtualFile) child).markDirty();
                }
            }
            else if (pair.second != null) {
                pair.second.markDirty();
            }
        }
    }

    private void markRecursiveDirsDirty(Iterable<Path> dirtyPaths, Set<WatchedFileSystem> marked) {
        for (Path dirtyPath : dirtyPaths) {
            ProgressManager.checkCanceled();

            Couple<NewVirtualFile> pair = findCachedFile(dirtyPath, marked);
            if (pair.first != null) {
                pair.first.markDirtyRecursively();
            }
            else if (pair.second != null) {
                pair.second.markDirty();
            }
        }
    }

    /**
     * Before a refresh of the files - what the watchers reported is marked dirty; the roots they cannot watch, and the files of
     * file systems not watched at all, are scanned.
     */
    public void markSuspiciousFilesDirty(List<VirtualFile> files) {
        storeRefreshStatusToFiles();

        Set<WatchedFileSystem> marked = new HashSet<>();
        for (Path root : myWatcher.getManualWatchRoots()) {
            NewVirtualFile suspiciousRoot = findCachedFile(root, marked).first;
            if (suspiciousRoot != null) {
                suspiciousRoot.markDirtyRecursively();
            }
        }

        for (VirtualFile file : files) {
            if (!(file.getFileSystem() instanceof WatchedFileSystem)) {
                continue;
            }
            Path path = file.getFileSystem().getNioPath(file);
            if (path == null || !myWatcher.isOperational(path.getFileSystem())) {
                ((NewVirtualFile) file).markDirtyRecursively();
            }
        }
    }

    // endregion

    @TestOnly
    public void cleanupForNextTest() {
        synchronized (myLock) {
            myRootsToWatch.clear();
            myNormalizedTree = null;
        }
    }
}
