// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.module.content.internal.TraverseState;
import consulo.module.content.internal.WorkspaceFileIndexEx;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.InvalidVirtualFileAccessException;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.ChildInfo;
import consulo.virtualFileSystem.impl.internal.TransientVirtualFileVfsRefreshUtils;
import consulo.virtualFileSystem.impl.internal.entry.ChildInfoImpl;
import consulo.virtualFileSystem.impl.internal.url.TreeNodeProcessingResult;
import consulo.virtualFileSystem.impl.internal.url.VirtualFileUrlManagerImpl;
import consulo.virtualFileSystem.internal.VirtualFileWithAttributes;
import consulo.virtualFileSystem.url.VirtualFileUrl;
import consulo.virtualFileSystem.url.VirtualFileUrlManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.io.IOError;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TransientChildScanner implements ChildScanner {
    private static final Logger LOG = Logger.getInstance(TransientChildScanner.class);
    private static final String ITERATE_INCLUDED_FILES_UNDER_EXCLUDE = "vfs.refresh.iterate.included.files.under.exclude";

    private static volatile @Nullable Runnable scanStartedCallbackForTests;

    private final Consumer<VirtualFile> checkCancelled;

    TransientChildScanner(Consumer<VirtualFile> checkCancelled) {
        this.checkCancelled = checkCancelled;
    }

    /**
     * {@code callback} is invoked each time the scanner starts loading children of a new directory
     */
    @TestOnly
    public static void setScanStartedCallbackForTests(Disposable disposable, Runnable callback) {
        scanStartedCallbackForTests = callback;
        Disposer.register(disposable, () -> scanStartedCallbackForTests = null);
    }

    /**
     * Traverses children of {@code childName} recursively, so they are loaded into VFS during event processing
     *
     * @return {@code null} if error occurred or children must not be loaded proactively. Children to load into VFS otherwise
     */
    @Override
    public @Nullable ScannedChildren scanChildrenRecursively(NewVirtualFile parent, String childName) {
        try {
            NewVirtualFileSystem fs = parent.getFileSystem();
            var child = TransientVirtualFileVfsRefreshUtils.createTransientChild(parent, childName, fs);
            if (!child.exists()) {
                return null;
            }

            return scanChildren(child, initialStates(parent, () -> checkCancelled.accept(parent)), parent);
        }
        catch (InvalidPathException e) {
            LOG.warn("Invalid child name: '" + childName + "'", e);
            return null;
        }
        catch (IOError e) {
            LOG.warn(e);
            return null;
        }
    }

    private static void notifyScanStarted() {
        Runnable callback = scanStartedCallbackForTests;
        if (callback != null) {
            callback.run();
        }
    }

    /**
     * Using project in the application scope, may lead to {@link CancellationException} because project is disposed
     * However, the vfs refresh should continue in this case if the application is not disposed
     */
    private static <T> @Nullable T useProject(@Nullable Project project,
                                              Runnable checkCanceled,
                                              Function<? super Project, ? extends @Nullable T> action) {
        if (project == null || project.isDisposed()) {
            return null;
        }
        try {
            return action.apply(project);
        }
        catch (CancellationException e) {
            checkCanceled.run();
            ProgressManager.checkCanceled();
            return null;
        }
    }

    /**
     * {@link TraverseState} of a file in {@code project}
     * <p>
     * Project is stored in a reference, so if it is disposed, it can be released, avoiding temporary project leak
     */
    private record ProjectState(SimpleReference<Project> projectRef, TraverseState traverseState, @Nullable VirtualFileUrl vfu) {
        private <T> @Nullable T useProject(Runnable checkCanceled, Function<? super Project, ? extends @Nullable T> action) {
            var project = projectRef.get();
            if (project != null && project.isDisposed()) {
                projectRef.set(null);
                return null;
            }
            return TransientChildScanner.useProject(project, checkCanceled, action);
        }

        /**
         * @return {@code null} if the project is disposed or excludes the subtree of {@code file}
         */
        @Nullable ProjectState goDown(VirtualFile file, Runnable checkCanceled) {
            return useProject(checkCanceled, project -> {
                VirtualFileUrl vfu = this.vfu == null ? null : this.vfu.findChild(file.getName());
                TraverseState state = this.traverseState.goDown(file, vfu, WorkspaceFileIndexEx.getInstance(project));
                if (state == null) {
                    return null;
                }
                return new ProjectState(projectRef, state, vfu);
            });
        }
    }

    private static VirtualFileUrlManagerImpl getVirtualFileUrlManager() {
        return (VirtualFileUrlManagerImpl) Application.get().getInstance(VirtualFileUrlManager.class);
    }

    /**
     * Whether an indexable recursive file set includes the file in any project, so the children of the file are indexable too
     */
    private static boolean isIndexableRecursive(List<ProjectState> states) {
        return ContainerUtil.exists(states, state -> state.traverseState().isIndexableRecursive());
    }

    /**
     * Computes states of {@code parent} which is loaded into VFS for all open projects.
     * Skips the projects which exclude the subtree of {@code parent}
     */
    private static List<ProjectState> initialStates(VirtualFile parent, Runnable checkCanceled) {
        String url = parent.getUrl();
        List<ProjectState> states = new ArrayList<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            var projectState = useProject(project, checkCanceled, p -> {
                TraverseState state = TraverseState.getTraverseState(parent, p);
                if (state == null) {
                    return null;
                }
                VirtualFileUrl vfu = getVirtualFileUrlManager().findNode(url);
                return new ProjectState(new SimpleReference<>(p), state, vfu);
            });
            if (projectState != null) {
                states.add(projectState);
            }
        }
        return states;
    }

    /**
     * Computes states of {@code file} from {@code parentStates} of its parent.
     * Skips the projects which exclude the subtree of {@code file}
     */
    private static List<ProjectState> goDown(List<ProjectState> parentStates,
                                             VirtualFile file,
                                             Runnable checkCanceled) {
        if (parentStates.isEmpty()) {
            return parentStates;
        }
        return ContainerUtil.mapNotNull(parentStates, state -> state.goDown(file, checkCanceled));
    }

    /**
     * Finds roots of indexable file sets under {@code directory} which aren't excluded in their projects
     *
     * @param states states of {@code directory}
     */
    private static List<VirtualFile> indexableRootsUnder(VirtualFile directory,
                                                         List<ProjectState> states,
                                                         Runnable checkCanceled) {
        if (!Registry.is(ITERATE_INCLUDED_FILES_UNDER_EXCLUDE)) {
            return Collections.emptyList();
        }

        var roots = new LinkedHashMap<String, VirtualFile>();
        String url = directory.getUrl();
        for (ProjectState directoryState : states) {
            directoryState.useProject(checkCanceled, project -> {
                var workspaceFileIndex = WorkspaceFileIndexEx.getInstance(project);
                var vfuManager = getVirtualFileUrlManager();
                vfuManager.processChildrenRecursively(url, child -> {
                    if (workspaceFileIndex.getFileSetsAt(child).mask() == 0) {
                        return TreeNodeProcessingResult.CONTINUE;
                    }
                    var root = directory.findFileByRelativePath(child.getUrl().substring(url.length()));
                    if (root == null) {
                        return TreeNodeProcessingResult.SKIP_CHILDREN; // file does not exist, skip
                    }

                    TraverseState state = stateUnder(directory, directoryState.traverseState(), root, child, workspaceFileIndex);
                    if (state == null) {
                        return TreeNodeProcessingResult.SKIP_CHILDREN; // file is excluded, skip
                    }

                    if (state.isIndexable()) {
                        roots.putIfAbsent(root.getPath(), root);
                        return TreeNodeProcessingResult.SKIP_CHILDREN; // find indexable file
                    }
                    return TreeNodeProcessingResult.CONTINUE; // file can be indexable underneath, continue
                });
                return null;
            });
        }
        return ContainerUtil.filter(roots.values(), VirtualFile::exists);
    }

    /**
     * Computes the traverseState of {@code file} under {@code directory} from {@code directoryState}
     *
     * @param vfu the node of {@code file} in the {@link VirtualFileUrl} trie
     */
    private static @Nullable TraverseState stateUnder(VirtualFile directory,
                                                      TraverseState directoryState,
                                                      VirtualFile file,
                                                      VirtualFileUrl vfu,
                                                      WorkspaceFileIndexEx workspaceFileIndex) {
        List<VirtualFile> path = new ArrayList<>();
        List<@Nullable VirtualFileUrl> vfus = new ArrayList<>();
        VirtualFileUrl currentVfu = vfu;
        for (VirtualFile current = file; current != null && !current.equals(directory); current = current.getParent()) {
            path.add(current);
            vfus.add(currentVfu);
            currentVfu = currentVfu == null ? null : currentVfu.getParent();
        }
        TraverseState state = directoryState;
        for (int i = path.size() - 1; i >= 0 && state != null; i--) {
            state = state.goDown(path.get(i), vfus.get(i), workspaceFileIndex);
        }
        return state;
    }

    /**
     * Scans the indexable children of {@code root} recursively
     *
     * @param parentStates states of the parent of {@code root}
     * @return {@code null} if an error occurs or no children need to be loaded
     */
    private @Nullable ScannedChildren scanChildren(VirtualFile root, List<ProjectState> parentStates, NewVirtualFile currentDir) {
        if (!root.exists() || TransientVirtualFileVfsRefreshUtils.isCached(root)) {
            return null;
        }
        List<ChildInfo> rootInfos = new SmartList<>();

        record ScanFrame(List<ChildInfo> parentChildren, List<ChildInfo> children, List<ProjectState> states) {
        }

        VirtualFileUtil.visitChildrenRecursively(root, new VirtualFileVisitor<ScanFrame>(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
            private int checkCanceledCount;

            {
                // The visitor restores its value stack in a finally block if traversal is interrupted.
                setValueForChildren(new ScanFrame(Collections.emptyList(), rootInfos, parentStates));
            }

            @Override
            public Iterable<VirtualFile> getChildrenIterable(VirtualFile file) {
                notifyScanStarted();
                VirtualFile[] children = file.getChildren();
                return Arrays.asList(children);
            }

            @Override
            public Result visitFileEx(VirtualFile file) {
                try {
                    if ((++checkCanceledCount & 0xf) == 0) {
                        checkCancelled.accept(currentDir);
                    }

                    ChildInfo info = createChildInfo(file);
                    if (info == null) {
                        return SKIP_CHILDREN;
                    }
                    ScanFrame frame = getCurrentValue();
                    List<ChildInfo> siblings = frame.children();
                    siblings.add(info);

                    FileAttributes attributes = info.getFileAttributes();
                    if (attributes == null || !isTraversableDirectory(attributes)) {
                        return SKIP_CHILDREN;
                    }

                    List<ProjectState> states = goDown(frame.states(), file, () -> checkCancelled.accept(currentDir));
                    if (!isIndexableRecursive(states)) {
                        siblings.set(siblings.size() - 1, withIndexableRootsUnder(file, info, states, currentDir));
                        return SKIP_CHILDREN;
                    }
                    setValueForChildren(new ScanFrame(siblings, new ArrayList<>(), states));
                    return CONTINUE;
                }
                catch (IOError | InvalidPathException | InvalidVirtualFileAccessException e) {
                    LOG.warn(e);
                    return SKIP_CHILDREN;  // ignoring exceptions from short-living temp files
                }
            }

            @Override
            public void afterChildrenVisited(VirtualFile file) {
                ScanFrame frame = getCurrentValue();
                List<ChildInfo> parentInfos = frame.parentChildren();
                ChildInfo parentInfo = ContainerUtil.getLastItem(parentInfos);
                ChildInfo[] children = frame.children().toArray(ChildInfo.EMPTY_ARRAY);
                ChildInfo newInfo = ((ChildInfoImpl) parentInfo).withChildren(children, true);
                parentInfos.set(parentInfos.size() - 1, newInfo);
            }
        });

        ChildInfo rootInfo = ContainerUtil.getFirstItem(rootInfos);
        if (rootInfo == null) {
            return null;
        }
        ChildInfo[] children = rootInfo.getChildren();
        return children == null ? null : new ScannedChildren(children, rootInfo.isAllChildren());
    }

    /**
     * Whether the scanner visits children of a file with {@code attributes}. It skips symlinks and NTFS reparse points
     */
    private static boolean isTraversableDirectory(FileAttributes attributes) {
        return attributes.isDirectory() && !attributes.isSymLink() && !(Platform.current().os().isWindows() && attributes.isSpecial());
    }

    private @Nullable ChildInfo createChildInfo(VirtualFile file) {
        NewVirtualFileSystem fs = (NewVirtualFileSystem) file.getFileSystem();
        FileAttributes attributes = computeAttributesForFile(fs, file);
        if (attributes == null) {
            return null;
        }
        String symlinkTarget = attributes.isSymLink() ? fs.resolveSymLink(file) : null;
        if (symlinkTarget != null) {
            symlinkTarget = FileUtil.toSystemIndependentName(symlinkTarget);
        }
        return new ChildInfoImpl(ChildInfoImpl.UNKNOWN_ID_YET, file.getName(), attributes, null, symlinkTarget, false);
    }

    private static @Nullable FileAttributes computeAttributesForFile(NewVirtualFileSystem fs, VirtualFile file) {
        if (file instanceof VirtualFileWithAttributes attributesCachingVirtualFile) {
            return attributesCachingVirtualFile.getAttributes();
        }
        return fs.getAttributes(file);
    }

    /**
     * @param states states of {@code node.file}
     */
    private ChildInfo withChildrenUnderExcluded(VirtualFileTreeNode node,
                                                ChildInfo info,
                                                List<ProjectState> states,
                                                NewVirtualFile currentDir) {
        List<ChildInfo> children = new ArrayList<>(node.children.size());
        for (VirtualFileTreeNode childNode : node.children.values()) {
            ChildInfo childInfo = createChildInfo(childNode.file);
            if (childInfo == null) {
                continue;
            }
            if (childNode.isContentRoot) {
                ScannedChildren scannedChildren = scanChildren(childNode.file, states, currentDir);
                children.add(scannedChildren == null
                    ? childInfo
                    : ((ChildInfoImpl) childInfo).withChildren(scannedChildren.children(), scannedChildren.childrenComplete()));
            }
            else {
                children.add(withChildrenUnderExcluded(childNode, childInfo,
                    goDown(states, childNode.file, () -> checkCancelled.accept(currentDir)), currentDir));
            }
        }
        return ((ChildInfoImpl) info).withChildren(children.toArray(ChildInfo.EMPTY_ARRAY), false);
    }

    /**
     * Adds to {@code info} the children of excluded {@code directory} down to the indexable roots under it
     *
     * @param states states of {@code directory}
     * @return {@code info} if there are no indexable roots under {@code directory}
     */
    private ChildInfo withIndexableRootsUnder(VirtualFile directory,
                                              ChildInfo info,
                                              List<ProjectState> states,
                                              NewVirtualFile currentDir) {
        List<VirtualFile> indexableRoots = indexableRootsUnder(directory, states, () -> checkCancelled.accept(currentDir));
        if (indexableRoots.isEmpty()) {
            return info;
        }
        notifyScanStarted();
        return withChildrenUnderExcluded(VirtualFileTreeNode.create(directory, indexableRoots), info, states, currentDir);
    }

    private static final class VirtualFileTreeNode {
        private final VirtualFile file;
        private final Map<String, VirtualFileTreeNode> children = new LinkedHashMap<>();
        private boolean isContentRoot;

        private VirtualFileTreeNode(VirtualFile file) {
            this.file = file;
        }

        private static VirtualFileTreeNode create(VirtualFile directory, List<VirtualFile> contentRootsUnder) {
            VirtualFileTreeNode result = new VirtualFileTreeNode(directory);
            for (var contentRoot : contentRootsUnder) {
                result.addContentRoot(contentRoot);
            }
            return result;
        }

        /**
         * Go up until reach {@link #file}, then create nodes for the passed path
         */
        private void addContentRoot(VirtualFile contentRoot) {
            List<VirtualFile> path = new ArrayList<>();
            VirtualFile currentFile = contentRoot;
            while (!currentFile.equals(file)) {
                path.add(currentFile);
                currentFile = currentFile.getParent();
                if (currentFile == null) {
                    return;
                }
            }

            if (isContentRoot) {
                return;
            }
            VirtualFileTreeNode node = this;
            for (var child : path.reversed()) {
                node = node.children.computeIfAbsent(child.getName(), ignored -> new VirtualFileTreeNode(child));
                if (node.isContentRoot) {
                    return;
                }
            }
            node.isContentRoot = true;
            node.children.clear();
        }
    }
}
