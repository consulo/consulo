// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.platform.Platform;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.collection.Queue;
import consulo.util.collection.Sets;
import consulo.util.io.FileAttributes;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.impl.internal.entry.VirtualDirectoryImpl;
import consulo.virtualFileSystem.internal.PersistentFS;
import consulo.virtualFileSystem.util.FilePathHashingStrategy;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static consulo.util.lang.Pair.pair;
import static consulo.virtualFileSystem.impl.internal.refresh.VfsEventGenerationHelper.LOG;

public class LocalFileSystemRefreshWorker {
    private final boolean myIsRecursive;
    private final NewVirtualFile myRefreshRoot;
    private final VfsEventGenerationHelper myHelper = new VfsEventGenerationHelper();
    private volatile boolean myCancelled;

    public LocalFileSystemRefreshWorker(@Nonnull NewVirtualFile refreshRoot, boolean isRecursive) {
        myIsRecursive = isRecursive;
        myRefreshRoot = refreshRoot;
    }

    @Nonnull
    public List<VFileEvent> getEvents() {
        return myHelper.getEvents();
    }

    public void cancel() {
        myCancelled = true;
    }

    public void scan() {
        NewVirtualFile root = myRefreshRoot;
        boolean rootDirty = root.isDirty();
        if (LOG.isDebugEnabled()) {
            LOG.debug("root=" + root + " dirty=" + rootDirty);
        }
        if (!rootDirty) {
            return;
        }

        NewVirtualFileSystem fs = root.getFileSystem();
        FileAttributes rootAttributes = fs.getAttributes(root);
        if (rootAttributes == null) {
            myHelper.scheduleDeletion(root);
            root.markClean();
            return;
        }

        RefreshContext context = createRefreshContext(fs, PersistentFS.getInstance(), FilePathHashingStrategy.create(fs.isCaseSensitive()));
        context.submitRefreshRequest(() -> processFile(root, context));
        context.waitForRefreshToFinish();
    }

    @Nonnull
    private RefreshContext createRefreshContext(@Nonnull NewVirtualFileSystem fs, @Nonnull PersistentFS persistentFS, @Nonnull HashingStrategy<String> strategy) {
        int parallelism = Registry.intValue("vfs.use.nio-based.local.refresh.worker.parallelism", Runtime.getRuntime().availableProcessors() - 1);

        if (myIsRecursive && parallelism > 0 && !Application.get().isDispatchThread()) {
            return new ConcurrentRefreshContext(fs, persistentFS, strategy, parallelism);
        }
        return new SequentialRefreshContext(fs, persistentFS, strategy);
    }

    private void processFile(@Nonnull NewVirtualFile file, @Nonnull RefreshContext refreshContext) {
        if (!VfsEventGenerationHelper.checkDirty(file) || isCancelled(file, refreshContext)) {
            return;
        }

        if (file.isDirectory()) {
            boolean fullSync = ((VirtualDirectoryImpl) file).allChildrenLoaded();
            if (fullSync) {
                fullDirRefresh((VirtualDirectoryImpl) file, refreshContext);
            }
            else {
                partialDirRefresh((VirtualDirectoryImpl) file, refreshContext);
            }
        }
        else {
            refreshFile(file, refreshContext);
        }

        if (isCancelled(file, refreshContext)) {
            return;
        }

        if (myIsRecursive || !file.isDirectory()) {
            file.markClean();
        }
    }

    private abstract static class RefreshContext {
        final NewVirtualFileSystem fs;
        final PersistentFS persistence;
        final HashingStrategy<String> strategy;
        final BlockingQueue<NewVirtualFile> filesToBecomeDirty = new LinkedBlockingQueue<>();

        RefreshContext(@Nonnull NewVirtualFileSystem fs, @Nonnull PersistentFS persistence, @Nonnull HashingStrategy<String> strategy) {
            this.fs = fs;
            this.persistence = persistence;
            this.strategy = strategy;
        }

        abstract void submitRefreshRequest(@Nonnull Runnable action);

        abstract void doWaitForRefreshToFinish();

        final void waitForRefreshToFinish() {
            doWaitForRefreshToFinish();

            for (NewVirtualFile file : filesToBecomeDirty) {
                forceMarkDirty(file);
            }
        }
    }

    private void refreshFile(@Nonnull NewVirtualFile file, @Nonnull RefreshContext refreshContext) {
        RefreshingFileVisitor refreshingFileVisitor = new RefreshingFileVisitor(file, refreshContext, null, Collections.singletonList(file));
        refreshingFileVisitor.visit(file);
        addAllEventsFrom(refreshingFileVisitor);
    }

    private void addAllEventsFrom(@Nonnull RefreshingFileVisitor refreshingFileVisitor) {
        synchronized (myHelper) {
            myHelper.addAllEventsFrom(refreshingFileVisitor.getHelper());
        }
    }

    private void fullDirRefresh(@Nonnull VirtualDirectoryImpl dir, @Nonnull RefreshContext refreshContext) {
        while (true) {
            // obtaining directory snapshot
            Pair<String[], VirtualFile[]> result = getDirectorySnapshot(refreshContext.persistence, dir);
            if (result == null) {
                return;
            }
            String[] persistedNames = result.getFirst();
            VirtualFile[] children = result.getSecond();

            RefreshingFileVisitor refreshingFileVisitor = new RefreshingFileVisitor(dir, refreshContext, null, Arrays.asList(children));
            refreshingFileVisitor.visit(dir);
            if (myCancelled) {
                addAllEventsFrom(refreshingFileVisitor);
                break;
            }

            // generating events unless a directory was changed in between
            boolean hasEvents = ReadAction.compute(() -> {
                if (Application.get().isDisposed()) {
                    return true;
                }
                if (!Arrays.equals(persistedNames, refreshContext.persistence.list(dir)) || !Arrays.equals(children, dir.getChildren())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("retry: " + dir);
                    }
                    return false;
                }

                addAllEventsFrom(refreshingFileVisitor);
                return true;
            });
            if (hasEvents) {
                break;
            }
        }
    }

    static Pair<String[], VirtualFile[]> getDirectorySnapshot(@Nonnull PersistentFS persistence, @Nonnull VirtualDirectoryImpl dir) {
        return ReadAction.compute(() -> Application.get().isDisposed() ? null : pair(persistence.list(dir), dir.getChildren()));
    }

    private void partialDirRefresh(@Nonnull VirtualDirectoryImpl dir, @Nonnull RefreshContext refreshContext) {
        while (true) {
            // obtaining directory snapshot
            Pair<List<VirtualFile>, List<String>> result = ReadAction.compute(() -> pair(dir.getCachedChildren(), dir.getSuspiciousNames()));

            List<VirtualFile> cached = result.getFirst();
            List<String> wanted = result.getSecond();

            if (cached.isEmpty() && wanted.isEmpty()) {
                return;
            }
            RefreshingFileVisitor refreshingFileVisitor = new RefreshingFileVisitor(dir, refreshContext, wanted, cached);
            refreshingFileVisitor.visit(dir);
            if (myCancelled) {
                addAllEventsFrom(refreshingFileVisitor);
                break;
            }

            // generating events unless a directory was changed in between
            boolean hasEvents = ReadAction.compute(() -> {
                if (!cached.equals(dir.getCachedChildren()) || !wanted.equals(dir.getSuspiciousNames())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("retry: " + dir);
                    }
                    return false;
                }

                addAllEventsFrom(refreshingFileVisitor);

                return true;
            });
            if (hasEvents) {
                break;
            }
        }
    }

    private boolean isCancelled(@Nonnull NewVirtualFile stopAt, @Nonnull RefreshContext refreshContext) {
        if (ourTestListener != null) {
            ourTestListener.accept(stopAt);
        }
        if (myCancelled) {
            refreshContext.filesToBecomeDirty.offer(stopAt);
            return true;
        }
        return false;
    }

    private void checkCancelled(@Nonnull NewVirtualFile stopAt, @Nonnull RefreshContext refreshContext) throws RefreshWorker.RefreshCancelledException {
        if (isCancelled(stopAt, refreshContext)) {
            throw new RefreshWorker.RefreshCancelledException();
        }
    }

    private static void forceMarkDirty(@Nonnull NewVirtualFile file) {
        file.markClean();  // otherwise consequent markDirty() won't have any effect
        file.markDirty();
    }

    private static Consumer<? super VirtualFile> ourTestListener;

    @TestOnly
    static void setTestListener(@Nullable Consumer<? super VirtualFile> testListener) {
        ourTestListener = testListener;
    }

    private static class SequentialRefreshContext extends RefreshContext {
        private final Queue<Runnable> myRefreshRequests = new Queue<>(100);

        SequentialRefreshContext(@Nonnull NewVirtualFileSystem fs, @Nonnull PersistentFS persistentFS, @Nonnull HashingStrategy<String> strategy) {
            super(fs, persistentFS, strategy);
        }

        @Override
        void submitRefreshRequest(@Nonnull Runnable request) {
            myRefreshRequests.addLast(request);
        }

        @Override
        void doWaitForRefreshToFinish() {
            while (!myRefreshRequests.isEmpty()) {
                myRefreshRequests.pullFirst().run();
            }
        }
    }

    private static class ConcurrentRefreshContext extends RefreshContext {
        private final ExecutorService service;
        private final AtomicInteger tasksScheduled = new AtomicInteger();
        private final CountDownLatch refreshFinishedLatch = new CountDownLatch(1);

        ConcurrentRefreshContext(@Nonnull NewVirtualFileSystem fs, @Nonnull PersistentFS persistentFS, @Nonnull HashingStrategy<String> strategy, int parallelism) {
            super(fs, persistentFS, strategy);
            service = AppExecutorUtil.createBoundedApplicationPoolExecutor("Refresh Worker", parallelism);
        }

        @Override
        void submitRefreshRequest(@Nonnull Runnable action) {
            tasksScheduled.incrementAndGet();

            service.execute(() -> {
                try {
                    action.run();
                }
                finally {
                    if (tasksScheduled.decrementAndGet() == 0) {
                        refreshFinishedLatch.countDown();
                    }
                }
            });
        }

        @Override
        void doWaitForRefreshToFinish() {
            try {
                refreshFinishedLatch.await(1, TimeUnit.DAYS);
                service.shutdown();
            }
            catch (InterruptedException ignore) {
            }
        }
    }

    private class RefreshingFileVisitor extends SimpleFileVisitor<Path> {
        private final VfsEventGenerationHelper myHelper = new VfsEventGenerationHelper();
        private final Map<String, VirtualFile> myPersistentChildren;
        private final Set<String> myChildrenWeAreInterested; // null - no limit

        private final NewVirtualFile myFileOrDir;
        private final RefreshContext myRefreshContext;

        /**
         * @param fileOrDir
         * @param refreshContext
         * @param childrenToRefresh          null means all
         * @param existingPersistentChildren
         */
        RefreshingFileVisitor(@Nonnull NewVirtualFile fileOrDir,
                              @Nonnull RefreshContext refreshContext,
                              @Nullable Collection<String> childrenToRefresh,
                              @Nonnull Collection<? extends VirtualFile> existingPersistentChildren) {
            myFileOrDir = fileOrDir;
            myRefreshContext = refreshContext;
            myPersistentChildren = Maps.newHashMap(existingPersistentChildren.size(), refreshContext.strategy);
            myChildrenWeAreInterested = childrenToRefresh == null ? null : Sets.newHashSet(childrenToRefresh, refreshContext.strategy);

            for (VirtualFile child : existingPersistentChildren) {
                String name = child.getName();
                myPersistentChildren.put(name, child);
                if (myChildrenWeAreInterested != null) {
                    myChildrenWeAreInterested.add(name);
                }
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            String name = file.getName(file.getNameCount() - 1).toString();

            if (!acceptsFileName(name)) {
                return FileVisitResult.CONTINUE;
            }

            NewVirtualFile child = (NewVirtualFile) myPersistentChildren.remove(name);
            boolean isDirectory = attributes.isDirectory();
            boolean isSpecial = attributes.isOther();
            boolean isLink = attributes.isSymbolicLink();

            if (isSpecial && isDirectory && Platform.current().os().isWindows()) {
                // Windows junction is a special directory, handle it as symlink
                isSpecial = false;
                isLink = true;
            }

            if (isLink) {
                try {
                    attributes = Files.readAttributes(file, BasicFileAttributes.class);
                }
                catch (FileSystemException ignore) {
                    attributes = BROKEN_SYMLINK_ATTRIBUTES;
                }
                isDirectory = attributes.isDirectory();
            }
            else if (myFileOrDir.is(VFileProperty.SYMLINK)) {
                try {
                    attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                }
                catch (NoSuchFileException | AccessDeniedException ignore) {
                    attributes = BROKEN_SYMLINK_ATTRIBUTES;
                }
                isLink = attributes.isSymbolicLink();
            }

            if (child == null) { // new file is created
                VirtualFile parent = myFileOrDir.isDirectory() ? myFileOrDir : myFileOrDir.getParent();

                String symlinkTarget = isLink ? file.toRealPath().toString() : null;
                try {
                    FileAttributes fa = toFileAttributes(file, attributes, isLink);
                    myHelper.scheduleCreation(parent, name, fa, symlinkTarget, () -> checkCancelled(myFileOrDir, myRefreshContext));
                }
                catch (RefreshWorker.RefreshCancelledException e) {
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

            if (isCancelled(child, myRefreshContext)) {
                return FileVisitResult.TERMINATE;
            }

            if (!child.isDirty()) {
                return FileVisitResult.CONTINUE;
            }

            boolean oldIsDirectory = child.isDirectory();
            boolean oldIsSymlink = child.is(VFileProperty.SYMLINK);
            boolean oldIsSpecial = child.is(VFileProperty.SPECIAL);

            if (oldIsDirectory != isDirectory || oldIsSymlink != isLink || oldIsSpecial != isSpecial) { // symlink or directory or special changed
                myHelper.scheduleDeletion(child);
                VirtualFile parent = myFileOrDir.isDirectory() ? myFileOrDir : myFileOrDir.getParent();
                String symlinkTarget = isLink ? file.toRealPath().toString() : null;
                try {
                    FileAttributes fa = toFileAttributes(file, attributes, isLink);
                    myHelper.scheduleCreation(parent, child.getName(), fa, symlinkTarget, () -> checkCancelled(myFileOrDir, myRefreshContext));
                }
                catch (RefreshWorker.RefreshCancelledException e) {
                    return FileVisitResult.TERMINATE;
                }
                // ignore everything else
                child.markClean();
                return FileVisitResult.CONTINUE;
            }

            String currentName = child.getName();
            if (!currentName.equals(name)) {
                myHelper.scheduleAttributeChange(child, VirtualFile.PROP_NAME, currentName, name);
            }

            if (!isDirectory) {
                myHelper.checkContentChanged(child, myRefreshContext.persistence.getTimeStamp(child), attributes.lastModifiedTime().toMillis(), myRefreshContext.persistence.getLastRecordedLength(child),
                    attributes.size());
            }

            myHelper.checkWritableAttributeChange(child, myRefreshContext.persistence.isWritable(child), isWritable(file, attributes, isDirectory));

            if (attributes instanceof DosFileAttributes dosFileAttributes) {
                myHelper.checkHiddenAttributeChange(child, child.is(VFileProperty.HIDDEN), dosFileAttributes.isHidden());
            }

            if (isLink) {
                myHelper.checkSymbolicLinkChange(child, child.getCanonicalPath(), myRefreshContext.fs.resolveSymLink(child));
            }

            if (!child.isDirectory()) {
                child.markClean();
            }
            else if (myIsRecursive) {
                myRefreshContext.submitRefreshRequest(() -> processFile(child, myRefreshContext));
            }
            return FileVisitResult.CONTINUE;
        }

        boolean acceptsFileName(@Nonnull String name) {
            return !VirtualFileUtil.isBadName(name);
        }

        void visit(@Nonnull VirtualFile fileOrDir) {
            try {
                Path path = Paths.get(fileOrDir.getPath());
                if (fileOrDir.isDirectory()) {
                    if (myChildrenWeAreInterested == null) {
                        // Files.walkFileTree is more efficient than File.openDirectoryStream / readAttributes because former provides access to cached
                        // file attributes of visited children, see usages of BasicFileAttributesHolder in FileTreeWalker.getAttributes
                        EnumSet<FileVisitOption> options = fileOrDir.is(VFileProperty.SYMLINK) ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : EnumSet.noneOf(FileVisitOption.class);
                        Files.walkFileTree(path, options, 1, this);
                    }
                    else {
                        for (String child : myChildrenWeAreInterested) {
                            try {
                                Path subPath = fixCaseIfNeeded(path.resolve(child), fileOrDir);
                                BasicFileAttributes attrs = Files.readAttributes(subPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                                FileVisitResult result = visitFile(subPath, attrs);
                                if (result == FileVisitResult.TERMINATE) {
                                    break;
                                }
                            }
                            catch (IOException ignore) {
                            }
                        }
                    }
                }
                else {
                    Path file = fixCaseIfNeeded(path, fileOrDir);
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    visitFile(file, attrs);
                }
            }
            catch (AccessDeniedException | NoSuchFileException ignore) {
            }
            catch (IOException ex) {
                LOG.error(ex);
            }
        }

        @Nonnull
        VfsEventGenerationHelper getHelper() {
            if (!myPersistentChildren.isEmpty()) {
                if (!myCancelled) {
                    for (VirtualFile child : myPersistentChildren.values()) {
                        myHelper.scheduleDeletion(child);
                    }
                }
                myPersistentChildren.clear();
            }

            return myHelper;
        }
    }

    @Nonnull
    private static Path fixCaseIfNeeded(@Nonnull Path path, @Nonnull VirtualFile file) throws IOException {
        if (Platform.current().fs().isCaseSensitive()) {
            return path;
        }
        // Mac: toRealPath() will return the current file's name w.r.t. case
        // Win: toRealPath(LinkOption.NOFOLLOW_LINKS) will return the current file's name w.r.t. case
        return file.is(VFileProperty.SYMLINK) ? path.toRealPath(LinkOption.NOFOLLOW_LINKS) : path.toRealPath();
    }

    private static boolean isWritable(@Nonnull Path file, @Nonnull BasicFileAttributes a, boolean directory) {
        boolean isWritable;

        if (a instanceof DosFileAttributes dosFileAttributes) {
            isWritable = directory || !dosFileAttributes.isReadOnly();
        }
        else if (a instanceof PosixFileAttributes posixFileAttributes) {
            isWritable = posixFileAttributes.permissions().contains(PosixFilePermission.OWNER_WRITE);
        }
        else {
            isWritable = file.toFile().canWrite();
        }
        return isWritable;
    }

    @Nonnull
    static FileAttributes toFileAttributes(@Nonnull Path path, @Nonnull BasicFileAttributes a, boolean isSymlink) {
        if (isSymlink && a == BROKEN_SYMLINK_ATTRIBUTES) {
            return FileAttributes.BROKEN_SYMLINK;
        }

        long lastModified = a.lastModifiedTime().toMillis();
        boolean writable = isWritable(path, a, a.isDirectory());
        if (Platform.current().os().isWindows()) {
            boolean hidden = path.getParent() != null && ((DosFileAttributes) a).isHidden();
            return new FileAttributes(a.isDirectory(), a.isOther(), isSymlink, hidden, a.size(), lastModified, writable);
        }
        else {
            return new FileAttributes(a.isDirectory(), a.isOther(), isSymlink, false, a.size(), lastModified, writable);
        }
    }

    private static final BasicFileAttributes BROKEN_SYMLINK_ATTRIBUTES = new BasicFileAttributes() {
        private final FileTime myFileTime = FileTime.fromMillis(0);

        @Override
        public FileTime lastModifiedTime() {
            return myFileTime;
        }

        @Override
        public FileTime lastAccessTime() {
            return myFileTime;
        }

        @Override
        public FileTime creationTime() {
            return myFileTime;
        }

        @Override
        public boolean isRegularFile() {
            return false;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isSymbolicLink() {
            return true;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public long size() {
            return 0;
        }

        @Override
        public Object fileKey() {
            return this;
        }
    };
}