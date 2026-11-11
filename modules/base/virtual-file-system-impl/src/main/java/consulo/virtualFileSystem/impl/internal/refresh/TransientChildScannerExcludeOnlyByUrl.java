// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.registry.Registry;
import consulo.logging.Logger;
import consulo.module.content.internal.TraverseState;
import consulo.module.content.internal.WorkspaceFileIndexEx;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectManagerEx;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.InvalidVirtualFileAccessException;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.ChildInfo;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.impl.internal.TransientVirtualFileVfsRefreshUtils;
import consulo.virtualFileSystem.impl.internal.entry.ChildInfoImpl;
import consulo.virtualFileSystem.impl.internal.url.TreeNodeProcessingResult;
import consulo.virtualFileSystem.impl.internal.url.VirtualFileUrlManagerImpl;
import consulo.virtualFileSystem.internal.VirtualFileWithAttributes;
import consulo.virtualFileSystem.url.VirtualFileUrlManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import org.jspecify.annotations.Nullable;

import java.io.IOError;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class TransientChildScannerExcludeOnlyByUrl implements ChildScanner {
    private static final Logger LOG = Logger.getInstance(TransientChildScannerExcludeOnlyByUrl.class);

    private final Consumer<VirtualFile> checkCancelled;

    TransientChildScannerExcludeOnlyByUrl(Consumer<VirtualFile> checkCancelled) {
        this.checkCancelled = checkCancelled;
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
            var exists = child.exists();
            if (!exists || !shouldScanDirectory(parent, child)) {
                return null;
            }

            String childUrl = child.getUrl();
            var relevantExcluded = ContainerUtil.mapNotNull(ProjectManagerEx.getInstanceEx().getAllExcludedUrls(), url -> {
                if (!VirtualFileUtil.isEqualOrAncestor(childUrl, url)) {
                    return null;
                }
                return child.findFileByRelativePath(url.substring(childUrl.length()));
            });
            return scanChildren(child, relevantExcluded, parent);
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

    private static boolean shouldScanDirectory(VirtualFile parent, VirtualFile child) {
        if (FileTypeRegistry.getInstance().isFileIgnored(child.getName())) {
            return false;
        }
        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            if (ReadAction.compute(() -> {
                TraverseState state = openProject.isDisposed() ? null : TraverseState.getTraverseState(parent, openProject);
                return state != null && state.isIndexableRecursive();
            })) {
                return true;
            }
        }
        return isIndexableFilesetRecursiveRoot(child) || !indexableRootsUnder(child).isEmpty();
    }

    private static boolean isIndexableFilesetRecursiveRoot(VirtualFile file) {
        if (!Registry.is("vfs.refresh.iterate.included.files.under.exclude")) {
            return false;
        }

        String url = file.getUrl();
        var openProjects = ProjectManager.getInstance().getOpenProjects();
        return ContainerUtil.exists(openProjects, project -> ReadAction.compute(
            () -> !project.isDisposed() && WorkspaceFileIndexEx.getInstance(project).isUrlIndexableRecursiveFileSetRoot(url)));
    }

    private static List<VirtualFile> indexableRootsUnder(VirtualFile directory) {
        if (!Registry.is("vfs.refresh.iterate.included.files.under.exclude")) {
            return Collections.emptyList();
        }

        var roots = new LinkedHashMap<String, VirtualFile>();
        String url = directory.getUrl();
        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            ReadAction.run(() -> {
                if (openProject.isDisposed()) {
                    return;
                }
                var workspaceFileIndex = WorkspaceFileIndexEx.getInstance(openProject);
                var vfuManager = (VirtualFileUrlManagerImpl) Application.get().getInstance(VirtualFileUrlManager.class);
                vfuManager.processChildrenRecursively(url, child -> {
                    if (workspaceFileIndex.isUrlIndexableRecursiveFileSetRoot(child.getUrl())) {
                        var root = directory.findFileByRelativePath(child.getUrl().substring(url.length()));
                        if (root != null) {
                            roots.putIfAbsent(root.getPath(), root);
                        }
                        return TreeNodeProcessingResult.SKIP_CHILDREN;
                    }
                    return TreeNodeProcessingResult.CONTINUE;
                });
            });
        }
        return ContainerUtil.filter(roots.values(), VirtualFile::exists);
    }

    // scan all children of "root" (except excluded dirs) recursively and return them in a ScannedChildren record
    // `null` means error during scan
    private @Nullable ScannedChildren scanChildren(VirtualFile root, List<VirtualFile> excluded, NewVirtualFile currentDir) {
        if (!root.exists() || TransientVirtualFileVfsRefreshUtils.isCached(root)) {
            return null;
        }
        ChildInfo fakeRoot = new ChildInfoImpl(ChildInfoImpl.UNKNOWN_ID_YET, "", null, null, null, false);
        List<ChildInfo> rootInfos = new SmartList<>(fakeRoot);

        record ScanFrame(List<ChildInfo> parentChildren, List<ChildInfo> children) {
        }

        VirtualFileUtil.visitChildrenRecursively(root, new VirtualFileVisitor<ScanFrame>(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
            private int checkCanceledCount;

            {
                // The visitor restores its value stack in a finally block if traversal is interrupted.
                setValueForChildren(new ScanFrame(Collections.emptyList(), rootInfos));
            }

            @Override
            public Iterable<VirtualFile> getChildrenIterable(VirtualFile file) {
                VirtualFile[] children = file.getChildren();
                return Arrays.asList(children);
            }

            @Override
            public Result visitFileEx(VirtualFile file) {
                try {
                    if (!file.equals(root)) {
                        if ((++checkCanceledCount & 0xf) == 0) {
                            checkCancelled.accept(currentDir);
                        }

                        ChildInfo info = createChildInfo(file);
                        if (info == null) {
                            return SKIP_CHILDREN;  // ignoring files deleted during the scan
                        }
                        getCurrentValue().children().add(info);

                        FileAttributes attributes = info.getFileAttributes();
                        if (attributes == null || !attributes.isDirectory() || attributes.isSymLink()) {
                            return SKIP_CHILDREN;
                        }
                        if (Platform.current().os().isWindows() && attributes.isSpecial()) {
                            return SKIP_CHILDREN;  // bypassing NTFS reparse points
                        }
                    }

                    // on average, this "excluded" array is small for any particular root, so linear search it is.
                    if (isExcluded(file, excluded) && !isIndexableFilesetRecursiveRoot(file)) {
                        List<VirtualFile> contentUnderExcluded = indexableRootsUnder(file);
                        if (contentUnderExcluded.isEmpty()) {
                            return SKIP_CHILDREN;
                        }
                        // `file` is excluded but has registered content roots beneath. Replace its ChildInfo with one whose children
                        // contain shared intermediate dirs down to the scanned content roots. Do not create a frame for the file because
                        // `afterChildrenVisited` will not be called after skipping the children.
                        List<ChildInfo> parentChildren = getCurrentValue().children();
                        ChildInfo dirInfo = ContainerUtil.getLastItem(parentChildren);
                        VirtualFileTreeNode rootNode = VirtualFileTreeNode.create(file, contentUnderExcluded);
                        parentChildren.set(parentChildren.size() - 1, withChildrenUnderExcluded(rootNode, dirInfo, excluded, currentDir));
                        return SKIP_CHILDREN;
                    }

                    setValueForChildren(new ScanFrame(getCurrentValue().children(), new ArrayList<>()));
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

        ChildInfo rootInfo = rootInfos.getFirst();
        ChildInfo[] children = rootInfo.getChildren();
        return children == null ? null : new ScannedChildren(children, rootInfo.isAllChildren());
    }

    private static boolean isExcluded(VirtualFile file, List<VirtualFile> excluded) {
        return excluded.contains(file);
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

    private ChildInfo withChildrenUnderExcluded(VirtualFileTreeNode node,
                                                ChildInfo info,
                                                List<VirtualFile> nestedExcluded,
                                                NewVirtualFile currentDir) {
        if (node.isContentRoot) {
            ScannedChildren children = scanChildren(node.file, nestedExcluded, currentDir);
            return children == null ? info : ((ChildInfoImpl) info).withChildren(children.children(), children.childrenComplete());
        }

        List<ChildInfo> children = new ArrayList<>(node.children.size());
        for (VirtualFileTreeNode childNode : node.children.values()) {
            ChildInfo childInfo = createChildInfo(childNode.file);
            if (childInfo == null) {
                continue;
            }
            ChildInfo childInfoWithChildren = withChildrenUnderExcluded(childNode, childInfo, nestedExcluded, currentDir);
            children.add(childInfoWithChildren);
        }
        return ((ChildInfoImpl) info).withChildren(children.toArray(ChildInfo.EMPTY_ARRAY), false);
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

            VirtualFileTreeNode node = this;
            if (node.isContentRoot) {
                return;
            }
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
