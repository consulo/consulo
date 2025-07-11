// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.application.ReadAction;
import consulo.logging.Logger;
import consulo.module.content.ProjectFileIndex;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.internal.ProjectManagerEx;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.collection.Stack;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.*;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.impl.internal.entry.ChildInfoImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class VfsEventGenerationHelper {
    static final Logger LOG = Logger.getInstance(VfsEventGenerationHelper.class);

    private final List<VFileEvent> myEvents = new ArrayList<>();
    private int myMarkedStart = -1;

    @Nonnull
    public List<VFileEvent> getEvents() {
        return myEvents;
    }

    public static boolean checkDirty(@Nonnull NewVirtualFile file) {
        boolean fileDirty = file.isDirty();
        if (LOG.isTraceEnabled()) {
            LOG.trace("file=" + file + " dirty=" + fileDirty);
        }
        return fileDirty;
    }

    public void checkContentChanged(@Nonnull VirtualFile file, long oldTimestamp, long newTimestamp, long oldLength, long newLength) {
        if (oldTimestamp != newTimestamp || oldLength != newLength) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("update file=" + file + (oldTimestamp != newTimestamp ? " TS=" + oldTimestamp + "->" + newTimestamp : "") + (oldLength != newLength ? " len=" + oldLength + "->" + newLength : ""));
            }
            myEvents.add(new VFileContentChangeEvent(null, file, file.getModificationStamp(), -1, oldTimestamp, newTimestamp, oldLength, newLength, true));
        }
    }

    public void scheduleCreation(
        @Nonnull VirtualFile parent,
        @Nonnull String childName,
        @Nonnull FileAttributes attributes,
        @Nullable String symlinkTarget,
        @Nonnull ThrowableRunnable<RefreshWorker.RefreshCancelledException> checkCanceled
    ) throws RefreshWorker.RefreshCancelledException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("create parent=" + parent + " name=" + childName + " attr=" + attributes);
        }
        ChildInfo[] children = null;
        if (attributes.isDirectory() && parent.getFileSystem() instanceof LocalFileSystem && !attributes.isSymLink()) {
            try {
                Path child = Paths.get(parent.getPath(), childName);
                if (shouldScanDirectory(parent, child, childName)) {
                    Path[] relevantExcluded = ContainerUtil.mapNotNull(ProjectManagerEx.getInstanceEx().getAllExcludedUrls(), url -> {
                        Path path = Paths.get(VirtualFileManager.extractPath(url));
                        return path.startsWith(child) ? path : null;
                    }, new Path[0]);
                    children = scanChildren(child, relevantExcluded, checkCanceled);
                }
            }
            catch (InvalidPathException ignored) {
                // Paths.get() throws sometimes
            }
        }
        VFileCreateEvent event = new VFileCreateEvent(null, parent, childName, attributes.isDirectory(), attributes, symlinkTarget, true, children);
        myEvents.add(event);
    }

    private static boolean shouldScanDirectory(@Nonnull VirtualFile parent, @Nonnull Path child, @Nonnull String childName) {
        if (FileTypeRegistry.getInstance().isFileIgnored(childName)) {
            return false;
        }
        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            if (ReadAction.compute(() -> ProjectFileIndex.getInstance(openProject).isUnderIgnored(parent))) {
                return false;
            }
            String projectRootPath = openProject.getBasePath();
            if (projectRootPath != null) {
                Path path = Paths.get(projectRootPath);
                if (child.startsWith(path)) {
                    return true;
                }
            }
        }
        return false;
    }


    void beginTransaction() {
        myMarkedStart = myEvents.size();
    }

    void endTransaction(boolean success) {
        if (!success) {
            myEvents.subList(myMarkedStart, myEvents.size()).clear();
        }
        myMarkedStart = -1;
    }

    // scan all children of "root" (except excluded dirs) recursively and return them in the ChildInfo[] array
    @Nullable // null means error during scan
    private static ChildInfo[] scanChildren(@Nonnull Path root, @Nonnull Path[] excluded, @Nonnull ThrowableRunnable<RefreshWorker.RefreshCancelledException> checkCanceled) throws RefreshWorker.RefreshCancelledException {
        // top of the stack contains list of children found so far in the current directory
        Stack<List<ChildInfo>> stack = new Stack<>();
        ChildInfo fakeRoot = new ChildInfoImpl(ChildInfoImpl.UNKNOWN_ID_YET, "", null, null, null);
        stack.push(new SmartList<>(fakeRoot));
        FileVisitor<Path> visitor = new SimpleFileVisitor<>() {
            int checkCanceledCount;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(root)) {
                    visitFile(dir, attrs);
                }
                // on average, this "excluded" array is very small for any particular root, so linear search it is.
                if (ArrayUtil.contains(dir, excluded)) {
                    // do not drill inside excluded root (just record its attributes nevertheless), even if we have content roots beneath
                    // stop optimization right here - it's too much pain to track all these nested content/excluded/content otherwise
                    return FileVisitResult.SKIP_SUBTREE;
                }
                stack.push(new ArrayList<>());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if ((++checkCanceledCount & 0xf) == 0) {
                    checkCanceled.run();
                }
                String name = file.getFileName().toString();
                boolean isSymLink = false;
                if (attrs.isSymbolicLink()) {
                    // under windows the isDirectory attribute for symlink is incorrect - reread it again
                    isSymLink = true;
                    attrs = Files.readAttributes(file, BasicFileAttributes.class);

                }
                FileAttributes attributes = LocalFileSystemRefreshWorker.toFileAttributes(file, attrs, isSymLink);
                String symLinkTarget = attributes.isSymLink() ? file.toRealPath().toString() : null;
                ChildInfo info = new ChildInfoImpl(ChildInfoImpl.UNKNOWN_ID_YET, name, attributes, null, symLinkTarget);
                stack.peek().add(info);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                List<ChildInfo> childInfos = stack.pop();
                List<ChildInfo> parentInfos = stack.peek();
                // store children back
                ChildInfo parentInfo = ContainerUtil.getLastItem(parentInfos);
                ChildInfo[] children = childInfos.toArray(ChildInfo.EMPTY_ARRAY);
                ChildInfo newInfo = new ChildInfoImpl(parentInfo.getId(),
                    parentInfo.getNameId(),
                    parentInfo.getFileAttributes(),
                    children,
                    parentInfo.getSymLinkTarget()
                );
                parentInfos.set(parentInfos.size() - 1, newInfo);
                return FileVisitResult.CONTINUE;
            }
        };
        try {
            Files.walkFileTree(root, visitor);
        }
        catch (IOException e) {
            LOG.warn(e);
            // tell client we didn't find any children, abandon the optimization altogether
            return null;
        }
        return stack.pop().get(0).getChildren();
    }

    void scheduleDeletion(@Nonnull VirtualFile file) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("delete file=" + file);
        }
        myEvents.add(new VFileDeleteEvent(null, file, true));
    }

    void checkSymbolicLinkChange(@Nonnull VirtualFile child, String oldTarget, String currentTarget) {
        String currentVfsTarget = currentTarget != null ? FileUtil.toSystemIndependentName(currentTarget) : null;
        if (!Comparing.equal(oldTarget, currentVfsTarget)) {
            scheduleAttributeChange(child, VirtualFile.PROP_SYMLINK_TARGET, oldTarget, currentVfsTarget);
        }
    }

    void checkHiddenAttributeChange(@Nonnull VirtualFile child, boolean oldHidden, boolean newHidden) {
        if (oldHidden != newHidden) {
            scheduleAttributeChange(child, VirtualFile.PROP_HIDDEN, oldHidden, newHidden);
        }
    }

    void checkWritableAttributeChange(@Nonnull VirtualFile file, boolean oldWritable, boolean newWritable) {
        if (oldWritable != newWritable) {
            scheduleAttributeChange(file, VirtualFile.PROP_WRITABLE, oldWritable, newWritable);
        }
    }

    void scheduleAttributeChange(@Nonnull VirtualFile file, @VirtualFile.PropName @Nonnull String property, Object current, Object upToDate) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("update file=" + file + ' ' + property + '=' + current + "->" + upToDate);
        }
        myEvents.add(new VFilePropertyChangeEvent(null, file, property, current, upToDate, true));
    }

    void addAllEventsFrom(@Nonnull VfsEventGenerationHelper otherHelper) {
        myEvents.addAll(otherHelper.myEvents);
    }
}