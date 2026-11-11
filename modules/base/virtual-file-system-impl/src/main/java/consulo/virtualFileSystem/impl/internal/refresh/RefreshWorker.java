// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.refresh;

import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.ReadAction;
import consulo.application.util.Semaphore;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.collection.Sets;
import consulo.util.interner.Interner;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.util.lang.MathUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.TimeoutUtil;
import consulo.virtualFileSystem.InvalidVirtualFileAccessException;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.event.ChildInfo;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileCopyEvent;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.event.VFilePropertyChangeEvent;
import consulo.virtualFileSystem.impl.internal.BatchingFileSystem;
import consulo.virtualFileSystem.impl.internal.FileSystemUnavailableException;
import consulo.virtualFileSystem.impl.internal.UnavailabilityAwareFileSystem;
import consulo.virtualFileSystem.impl.internal.entry.ChildInfoImpl;
import consulo.virtualFileSystem.impl.internal.entry.VirtualDirectoryImpl;
import consulo.virtualFileSystem.impl.internal.entry.VirtualFileSystemEntry;
import consulo.virtualFileSystem.internal.DiskQueryRelay;
import consulo.virtualFileSystem.internal.FakeVirtualFile;
import consulo.virtualFileSystem.internal.PersistentFS;
import consulo.virtualFileSystem.util.FilePathHashingStrategy;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class RefreshWorker {
    private static final Logger LOG = Logger.getInstance(RefreshWorker.class);

    private static final int PARALLELISM = MathUtil.clamp(
        Registry.intValue("vfs.refresh.worker.parallelism", 6),
        1, Runtime.getRuntime().availableProcessors()
    );

    private static final Executor executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("RefreshWorker", PARALLELISM);

    private static final @Nullable Object REQUESTOR = null;

    private final boolean isRecursive;
    private final boolean parallel;

    private final Set<NewVirtualFile> roots;
    private final Queue<NewVirtualFile> refreshQueue;
    private final Semaphore semaphore;

    private final PersistentFS persistentFS = PersistentFS.getInstance();
    private final ChildScanner childScanner;

    private volatile boolean cancelled;

    /**
     * The files whose file system could not answer in this scan. Their state is unknown, not absent: the worker generates
     * no events for them and keeps them dirty, so a later refresh asks again once the file system can answer. See {@link #scan()}.
     */
    private final Set<NewVirtualFile> skipped = ConcurrentHashMap.newKeySet();

    // =========================== monitoring =========================================================================== //
    private final AtomicInteger fullScans = new AtomicInteger();
    private final AtomicInteger partialScans = new AtomicInteger();
    private final AtomicInteger queryItemsProcessed = new AtomicInteger();
    /**
     * Total time (ns) since instance creation, spent on VFS (i.e. potentially cached) requests
     */
    private final AtomicLong vfsTime = new AtomicLong();
    /**
     * Total time (ns) since instance creation, spent on IO -- usually, via {@link VirtualFileSystem}
     */
    private final AtomicLong ioTime = new AtomicLong();

    public RefreshWorker(Collection<NewVirtualFile> refreshRoots, boolean isRecursive) {
        this.isRecursive = isRecursive;
        Application application = Application.get();
        parallel = isRecursive
            && (PARALLELISM > 1 && !((ApplicationEx) application).isWriteThread());
        roots = new HashSet<>(refreshRoots);
        refreshQueue = new LinkedBlockingQueue<>(refreshRoots);
        semaphore = new Semaphore(refreshRoots.size());
        childScanner = getChildScanner();
    }

    @SuppressWarnings("deprecation")
    private ChildScanner getChildScanner() {
        if (!Registry.is("vfs.refresh.use.transient.files.for.children.preloading", true)) {
            return new NioChildScanner(file -> checkCancelled((NewVirtualFile) file));
        }
        if (!Registry.is("vfs.refresh.use.accurate.wfi.data.for.children.preloading", true)) {
            return new TransientChildScannerExcludeOnlyByUrl(file -> checkCancelled((NewVirtualFile) file));
        }
        return new TransientChildScanner(file -> checkCancelled((NewVirtualFile) file));
    }

    public void cancel() {
        cancelled = true;
    }

    public List<VFileEvent> scan() {
        var events = new ArrayList<VFileEvent>();
        if (!parallel) {
            singleThreadScan(events);
        }
        else {
            parallelScan(events);
        }
        // the parents are already clean, and `markDirty` of a still-dirty file does not reach them:
        // re-dirty the skipped files after the join, so the chain from the root to each of them is dirty again
        skipped.forEach(RefreshWorker::forceMarkDirty);
        return events;
    }

    private void singleThreadScan(List<VFileEvent> events) {
        try {
            processQueue(events);
        }
        catch (RefreshCancelledException e) {
            LOG.trace("refresh cancelled [1T]");
        }
    }

    private void parallelScan(List<VFileEvent> events) {
        var futures = new ArrayList<CompletableFuture<List<VFileEvent>>>(PARALLELISM);

        for (var i = 0; i < PARALLELISM; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                var threadEvents = new ArrayList<VFileEvent>();
                try {
                    processQueue(threadEvents);
                }
                catch (RefreshCancelledException ignored) {
                }
                catch (CancellationException e) {
                    cancelled = true;
                }
                catch (Throwable t) {
                    LOG.error(t);
                    cancelled = true;
                }
                return threadEvents;
            }, executor));
        }

        for (var future : futures) {
            try {
                events.addAll(future.get());
            }
            catch (InterruptedException ignored) {
            }
            catch (ExecutionException e) {
                LOG.error(e);
            }
        }

        if (cancelled) {
            LOG.trace("refresh cancelled [MT]");
        }
    }

    private void processQueue(/*OutParam*/ List<VFileEvent> events) throws RefreshCancelledException {
        nextDir:
        while (!semaphore.isUp()) {
            var file = refreshQueue.poll();
            if (file == null) {
                TimeoutUtil.sleep(1);
                continue;
            }

            var fs = file.getFileSystem();

            try {
                if (roots.contains(file)) {
                    var attributes = computeAttributesForFile(fs, file);
                    if (attributes == null) {
                        scheduleDeletion(events, file);
                        file.markClean();
                        continue;
                    }

                    checkAndScheduleChildRefresh(events, fs, file.getParent(), file, attributes, false);

                    if (!file.isDirty() || !file.isDirectory()) {
                        continue;
                    }
                }

                var dir = (VirtualDirectoryImpl) file;
                var mark = events.size();

                while (true) {
                    checkCancelled(dir);
                    boolean fullSync = dir.allChildrenLoaded();
                    try {
                        boolean success;
                        if (fullSync) {
                            fullScans.incrementAndGet();
                            success = fullDirRefresh(events, fs, dir);
                        }
                        else {
                            partialScans.incrementAndGet();
                            success = partialDirRefresh(events, fs, dir);
                        }
                        if (success) {
                            break;
                        }

                        events.subList(mark, events.size()).clear();
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("retry: " + dir);
                        }
                    }
                    catch (InvalidVirtualFileAccessException e) {
                        events.subList(mark, events.size()).clear();
                        continue nextDir;
                    }
                }
                queryItemsProcessed.incrementAndGet();

                if (isRecursive) {
                    dir.markClean();
                }
            }
            catch (FileSystemUnavailableException e) {
                LOG.info("refresh skipped, the file system is unavailable: " + file);
                skipped.add(file);
            }
            finally {
                semaphore.up();
            }
        }
    }

    private boolean fullDirRefresh(List<VFileEvent> events, NewVirtualFileSystem fs, VirtualDirectoryImpl dir)
        throws FileSystemUnavailableException {
        var t = System.nanoTime();
        Pair<VirtualFile[], List<String>> snapshot = ReadAction.compute(() -> {
            VirtualFile[] children = dir.getChildren();
            return new Pair<>(children, getNames(children));
        });
        vfsTime.addAndGet(System.nanoTime() - t);
        VirtualFile[] vfsChildren = snapshot.first;
        List<String> vfsNames = snapshot.second;

        boolean dirIsCaseSensitive = dir.getFileSystem().isCaseSensitive();

        Map<String, @Nullable FileAttributes> childrenWithAttributes;
        t = System.nanoTime();
        if (fs instanceof BatchingFileSystem) {
            childrenWithAttributes = adjustCaseSensitivity(
                computeAllChildrenAttributes((BatchingFileSystem) fs, dir, /*filter: */null),
                dirIsCaseSensitive
            );
        }
        else {
            String[] childrenNames = fs.list(dir);
            childrenWithAttributes = createFilePathMap(childrenNames.length, dirIsCaseSensitive);
            for (String name : childrenNames) {
                childrenWithAttributes.put(name, null);
            }
            if (childrenWithAttributes.size() != childrenNames.length) {
                //TODO RC: seems like dir.isCaseSensitive() is wrong/outdated (i.e. actual dir case-sensitivity is different from
                //         FS-default, and it wasn't yet determined).
                //         We should re-query dir.case-sensitivity
            }
        }
        ioTime.addAndGet(System.nanoTime() - t);

        Set<String> newNames = createFilePathSet(childrenWithAttributes.keySet(), dirIsCaseSensitive);
        vfsNames.forEach(newNames::remove);

        Set<String> deletedNames = createFilePathSet(vfsNames, dirIsCaseSensitive);
        childrenWithAttributes.keySet().forEach(deletedNames::remove);

        Interner<String> actualNames = dirIsCaseSensitive ? null : createCaseInsensitiveNames(childrenWithAttributes.keySet());
        if (LOG.isTraceEnabled()) {
            LOG.trace("current=" + vfsNames + " +" + newNames + " -" + deletedNames);
        }

        List<ChildInfo> newKids = newNames.isEmpty() && deletedNames.isEmpty() ?
            List.of() :
            new ArrayList<>(newNames.size());
        for (String newName : newNames) {
            if (VirtualFileUtil.isBadName(newName)) {
                continue;
            }
            FakeVirtualFile child = new FakeVirtualFile(dir, newName);
            FileAttributes attributes = getAttributes(fs, childrenWithAttributes, child);
            if (attributes != null) {
                newKids.add(childRecord(fs, child, attributes, false));
            }
        }

        List<Pair<VirtualFile, @Nullable FileAttributes>> existingMap = new ArrayList<>(vfsChildren.length - deletedNames.size());
        for (VirtualFile child : vfsChildren) {
            if (!deletedNames.contains(child.getName())) {
                existingMap.add(new Pair<>(child, getAttributes(fs, childrenWithAttributes, child)));
            }
        }

        checkCancelled(dir);
        if (isDirectoryChanged(dir, vfsChildren, vfsNames)) {
            return false;
        }

        generateDeleteEvents(events, dir, deletedNames, actualNames, newKids);

        generateCreateEvents(events, dir, newKids);

        generateUpdateEvents(events, fs, dir, actualNames, existingMap);

        checkCancelled(dir);
        return !isDirectoryChanged(dir, vfsChildren, vfsNames);
    }

    private static List<String> getNames(VirtualFile[] children) {
        return ContainerUtil.map(children, VirtualFile::getName);
    }

    private boolean isDirectoryChanged(VirtualDirectoryImpl dir, VirtualFile[] children, List<String> names) {
        var t = System.nanoTime();
        var changed = ReadAction.compute(() -> {
            VirtualFile[] currentChildren = dir.getChildren();
            return !Arrays.equals(children, currentChildren) || !names.equals(getNames(currentChildren));
        });
        vfsTime.addAndGet(System.nanoTime() - t);
        return changed;
    }

    private boolean partialDirRefresh(List<VFileEvent> events, NewVirtualFileSystem fs, VirtualDirectoryImpl dir)
        throws FileSystemUnavailableException {
        var t = System.nanoTime();
        Pair<List<VirtualFile>, List<String>> snapshot = ReadAction.compute(
            () -> new Pair<>(dir.getCachedChildren(), dir.getSuspiciousNames())
        );
        vfsTime.addAndGet(System.nanoTime() - t);
        List<VirtualFile> cached = snapshot.first;
        List<String> wanted = snapshot.second;

        boolean dirIsCaseSensitive = dir.getFileSystem().isCaseSensitive();
        Set<String> namesToRefresh = createFilePathSet(wanted, dirIsCaseSensitive);
        for (VirtualFile file : cached) {
            namesToRefresh.add(file.getName());
        }

        Map<String, @Nullable FileAttributes> childrenWithAttributes = null;
        if (fs instanceof BatchingFileSystem batchingFileSystem) {
            t = System.nanoTime();
            childrenWithAttributes = adjustCaseSensitivity(
                computeAllChildrenAttributes(batchingFileSystem, dir, namesToRefresh),
                dirIsCaseSensitive
            );
            ioTime.addAndGet(System.nanoTime() - t);
        }

        Interner<String> actualNames;
        if (dirIsCaseSensitive || cached.isEmpty()) {
            actualNames = null;
        }
        else if (childrenWithAttributes != null) {
            actualNames = createCaseInsensitiveNames(childrenWithAttributes.keySet());
        }
        else {
            t = System.nanoTime();
            String[] childrenNames = fs.list(dir);
            actualNames = createCaseInsensitiveNames(Arrays.asList(childrenNames));
            ioTime.addAndGet(System.nanoTime() - t);
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("cached=" + cached + " actual=" + (actualNames == null ? null : actualNames.getValues()) + " suspicious=" + wanted);
        }

        List<ChildInfo> newKids = wanted.isEmpty() ? List.of() : new ArrayList<>(wanted.size());
        for (String newName : wanted) {
            if (VirtualFileUtil.isBadName(newName)) {
                continue;
            }
            FakeVirtualFile child = new FakeVirtualFile(dir, newName);
            FileAttributes attributes = getAttributes(fs, childrenWithAttributes, child);
            if (attributes != null) {
                newKids.add(childRecord(fs, child, attributes, /*canonicalize: */ true));
            }
        }

        List<Pair<VirtualFile, @Nullable FileAttributes>> existingMap = cached.isEmpty() ? List.of() : new ArrayList<>(cached.size());
        for (VirtualFile child : cached) {
            existingMap.add(new Pair<>(child, getAttributes(fs, childrenWithAttributes, child)));
        }

        checkCancelled(dir);
        if (isDirectoryChanged(dir, cached, wanted)) {
            return false;
        }

        generateCreateEvents(events, dir, newKids);

        generateUpdateEvents(events, fs, dir, actualNames, existingMap);

        checkCancelled(dir);
        return !isDirectoryChanged(dir, cached, wanted);
    }

    private boolean isDirectoryChanged(VirtualDirectoryImpl dir, List<VirtualFile> cached, List<String> wanted) {
        var t = System.nanoTime();
        var changed = ReadAction.compute(() -> !cached.equals(dir.getCachedChildren()) || !wanted.equals(dir.getSuspiciousNames()));
        vfsTime.addAndGet(System.nanoTime() - t);
        return changed;
    }

    /**
     * Converts a case-sensitive childrenWithAttributes map into case-insensitive, if toCaseSensitive=false,
     * leaves the map as-is otherwise
     */
    private static Map<String, @Nullable FileAttributes> adjustCaseSensitivity(Map<String, FileAttributes> childrenWithAttributes,
                                                                               boolean toCaseSensitive) {
        if (toCaseSensitive) {
            return childrenWithAttributes;
        }
        else {
            Map<String, @Nullable FileAttributes> childrenWithAttributesCaseInsensitive = createFilePathMap(
                childrenWithAttributes.size(),
                /*caseSensitive: */ false
            );
            childrenWithAttributesCaseInsensitive.putAll(childrenWithAttributes);
            if (childrenWithAttributesCaseInsensitive.size() != childrenWithAttributes.size()) {
                //TODO RC: seems like a conflict if dir.isCaseSensitive() is wrong/outdated (i.e. actual dir case-sensitivity
                //         is different from FS-default, and it wasn't yet determined).
                //         We should re-query dir.case-sensitivity
            }
            return childrenWithAttributesCaseInsensitive;
        }
    }

    private @Nullable FileAttributes getAttributes(NewVirtualFileSystem fs,
                                                   @Nullable Map<String, @Nullable FileAttributes> dirList,
                                                   VirtualFile child) throws FileSystemUnavailableException {
        FileAttributes attributes = null;
        if (dirList != null) {
            attributes = dirList.get(child.getName());
        }
        if (attributes == null && !(fs instanceof BatchingFileSystem)) {
            var t = System.nanoTime();
            attributes = computeAttributesForFile(fs, child);
            ioTime.addAndGet(System.nanoTime() - t);
        }
        return attributes;
    }

    /**
     * If attributes are computed in a cancellable context, then single-thread refresh gets a performance degradation.
     * The reason is {@link DiskQueryRelay#accessDiskWithCheckCanceled(Object)},
     * which starts constant exchanging messages with an IO thread.
     * The non-cancellable section here is merely a reification of the existing implicit assumption on cancellability,
     * so it does not make anything worse.
     * In the future, it should be removed in favor of non-blocking or suspending IO.
     */
    private static @Nullable FileAttributes computeAttributesForFile(NewVirtualFileSystem fs, VirtualFile file)
        throws FileSystemUnavailableException {
        return fs instanceof UnavailabilityAwareFileSystem uafs
            ? uafs.getAttributesOrThrowIfUnavailable(file)
            : fs.getAttributes(file);
    }

    /**
     * See {@link RefreshWorker#computeAttributesForFile(NewVirtualFileSystem, VirtualFile)} docs about cancellability
     */
    private static Map<String, FileAttributes> computeAllChildrenAttributes(BatchingFileSystem fs,
                                                                            VirtualFile dir,
                                                                            @Nullable Set<String> filter)
        throws FileSystemUnavailableException {
        return fs instanceof UnavailabilityAwareFileSystem uafs
            ? uafs.listWithAttributesOrThrowIfUnavailable(dir, filter)
            : fs.listWithAttributes(dir, filter);
    }

    private ChildInfo childRecord(NewVirtualFileSystem fs, FakeVirtualFile child, FileAttributes attributes, boolean canonicalize) {
        var t = System.nanoTime();
        String name = canonicalize ? fs.getCanonicallyCasedName(child) : child.getName();
        boolean isEmptyDir = attributes.isDirectory() && !fs.hasChildren(child);
        String symlinkTarget = attributes.isSymLink() ? fs.resolveSymLink(child) : null;
        ioTime.addAndGet(System.nanoTime() - t);
        return new ChildInfoImpl(ChildInfoImpl.UNKNOWN_ID_YET, name, attributes, isEmptyDir ? ChildInfo.EMPTY_ARRAY : null, symlinkTarget);
    }

    private void generateDeleteEvents(List<VFileEvent> events,
                                      VirtualDirectoryImpl dir,
                                      Set<String> deletedNames,
                                      @Nullable Interner<String> actualNames,
                                      List<ChildInfo> newKids) {
        for (String name : deletedNames) {
            VirtualFileSystemEntry child = dir.findChild(name);
            if (child != null) {
                if (checkAndScheduleFileNameChange(events, actualNames, child)) {
                    newKids.removeIf(newKidCandidate -> StringUtil.equal(newKidCandidate.getName(), child.getName(), true));
                }
                else {
                    scheduleDeletion(events, child);
                }
            }
        }
    }

    private void generateCreateEvents(List<VFileEvent> events, VirtualDirectoryImpl dir, List<ChildInfo> newKids) {
        for (ChildInfo record : newKids) {
            scheduleCreationOrCopy(
                events, dir, record.getName().toString(), record.getFileAttributes(), record.getSymLinkTarget(), null, REQUESTOR
            );
        }
    }

    private void generateUpdateEvents(List<VFileEvent> events,
                                      NewVirtualFileSystem fs,
                                      VirtualDirectoryImpl dir,
                                      @Nullable Interner<String> actualNames,
                                      List<Pair<VirtualFile, @Nullable FileAttributes>> existingMap) {
        for (Pair<VirtualFile, @Nullable FileAttributes> pair : existingMap) {
            NewVirtualFile child = (NewVirtualFile) pair.first;
            FileAttributes childAttributes = pair.second;
            if (childAttributes != null) {
                checkAndScheduleChildRefresh(events, fs, dir, child, childAttributes, true);
                checkAndScheduleFileNameChange(events, actualNames, child);
            }
            else {
                scheduleDeletion(events, child);
            }
        }
    }

    private static final class RefreshCancelledException extends RuntimeException {
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private void checkCancelled(NewVirtualFile stopAt) throws RefreshCancelledException {
        Consumer<? super VirtualFile> testListener = ourTestListener;
        if (testListener != null) {
            testListener.accept(stopAt);
        }
        if (cancelled) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("cancelled at: " + stopAt);
            }
            forceMarkDirty(stopAt);
            synchronized (this) {
                NewVirtualFile file;
                while ((file = refreshQueue.poll()) != null) {
                    forceMarkDirty(file);
                    semaphore.up();
                }
            }
            throw new RefreshCancelledException();
        }
    }

    private static void forceMarkDirty(NewVirtualFile file) {
        file.markClean();  // otherwise, consequent markDirty() won't have any effect
        file.markDirty();
    }

    private void scheduleDeletion(List<VFileEvent> events, VirtualFile file) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("delete file=" + file);
        }
        events.add(new VFileDeleteEvent(REQUESTOR, file, true));
    }

    private void scheduleCreationOrCopy(List<VFileEvent> events,
                                        NewVirtualFile parent,
                                        String childName,
                                        FileAttributes attributes,
                                        @Nullable String symlinkTarget,
                                        @Nullable VirtualFile file,
                                        @Nullable Object requestor) {
        if (LOG.isTraceEnabled()) {
            LOG.trace((file == null ? "create" : "copy") + " parent=" + parent + " name=" + childName + " attr=" + attributes);
        }

        ChildScanner.ScannedChildren scannedChildren = null;
        if (attributes.isDirectory() && !attributes.isSymLink()) {
            var t = System.nanoTime();
            scannedChildren = childScanner.scanChildrenRecursively(parent, childName);
            ioTime.addAndGet(System.nanoTime() - t);
        }

        ChildInfo[] children = scannedChildren == null ? null : scannedChildren.children();
        boolean childrenComplete = scannedChildren != null && scannedChildren.childrenComplete();
        if (file == null) {
            events.add(new VFileCreateEvent(
                requestor, parent, childName, attributes.isDirectory(), attributes, symlinkTarget, true, children, childrenComplete
            ));
        }
        else {
            events.add(new VFileCopyEvent(requestor, file, parent, childName));
        }
    }

    private void checkAndScheduleChildRefresh(List<VFileEvent> events,
                                              NewVirtualFileSystem fs,
                                              @Nullable NewVirtualFile parent,
                                              NewVirtualFile child,
                                              FileAttributes childAttributes,
                                              boolean enqueue) {
        boolean fileDirty = child.isDirty();
        if (LOG.isTraceEnabled()) {
            LOG.trace("file=" + child + " dirty=" + fileDirty);
        }
        if (!fileDirty) {
            return;
        }

        if (checkAndScheduleFileTypeChange(events, fs, parent, child, childAttributes)) {
            child.markClean();
            return;
        }

        checkWritableAttributeChange(events, child, persistentFS.isWritable(child), childAttributes.isWritable());

        if (Platform.current().os().isWindows()) {
            checkHiddenAttributeChange(events, child, child.is(VFileProperty.HIDDEN), childAttributes.isHidden());
        }

        if (childAttributes.isSymLink()) {
            var t = System.nanoTime();
            var target = fs.resolveSymLink(child);
            ioTime.addAndGet(System.nanoTime() - t);
            checkSymbolicLinkChange(events, child, child.getCanonicalPath(), target);
        }

        if (!childAttributes.isDirectory()) {
            long oldTimestamp = persistentFS.getTimeStamp(child), newTimestamp = childAttributes.lastModified;
            long oldLength = persistentFS.getLastRecordedLength(child), newLength = childAttributes.length;
            if (oldTimestamp != newTimestamp || oldLength != newLength) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("update file=" + child +
                        (oldTimestamp != newTimestamp ? " TS=" + oldTimestamp + "->" + newTimestamp : "") +
                        (oldLength != newLength ? " len=" + oldLength + "->" + newLength : ""));
                }

                events.add(new VFileContentChangeEvent(REQUESTOR, child, child.getModificationStamp(),
                    -1, oldTimestamp, newTimestamp,
                    oldLength, newLength, true));
            }
            child.markClean();
        }
        else if (enqueue && isRecursive) {
            if (child instanceof VirtualDirectoryImpl) {
                semaphore.down();
                refreshQueue.add(child);
            }
            else {
                LOG.error("not a directory: " + child + " (" + child.getClass() + ')');
            }
        }
    }

    private boolean checkAndScheduleFileTypeChange(List<VFileEvent> events,
                                                   NewVirtualFileSystem fs,
                                                   @Nullable NewVirtualFile parent,
                                                   NewVirtualFile child,
                                                   FileAttributes childAttributes) {
        boolean currentIsDirectory = child.isDirectory(), upToDateIsDirectory = childAttributes.isDirectory();
        boolean currentIsSymlink = child.is(VFileProperty.SYMLINK), upToDateIsSymlink = childAttributes.isSymLink();
        boolean currentIsSpecial = child.is(VFileProperty.SPECIAL), upToDateIsSpecial = childAttributes.isSpecial();

        boolean isFileTypeChanged = currentIsSymlink != upToDateIsSymlink || currentIsSpecial != upToDateIsSpecial;
        if (currentIsDirectory != upToDateIsDirectory ||
            (isFileTypeChanged && !Boolean.getBoolean("refresh.ignore.file.type.changes"))) {
            scheduleDeletion(events, child);
            if (parent != null) {
                var t = System.nanoTime();
                String symlinkTarget = upToDateIsSymlink ? fs.resolveSymLink(child) : null;
                ioTime.addAndGet(System.nanoTime() - t);
                scheduleCreationOrCopy(events, parent, child.getName(), childAttributes, symlinkTarget, null, REQUESTOR);
            }
            else {
                LOG.error("transgender orphan: " + child + ' ' + childAttributes);
            }
            return true;
        }

        return false;
    }

    private boolean checkAndScheduleFileNameChange(List<VFileEvent> events,
                                                   @Nullable Interner<String> actualNames,
                                                   VirtualFile child) {
        if (actualNames != null) {
            String currentName = child.getName();
            String actualName = actualNames.get(currentName);
            if (actualName != null && !currentName.equals(actualName)) {
                scheduleAttributeChange(events, child, VirtualFile.PROP_NAME, currentName, actualName);
                return true;
            }
        }
        return false;
    }

    private void checkWritableAttributeChange(List<VFileEvent> events, VirtualFile file, boolean oldWritable, boolean newWritable) {
        if (oldWritable != newWritable) {
            scheduleAttributeChange(events, file, VirtualFile.PROP_WRITABLE, oldWritable, newWritable);
        }
    }

    private void checkHiddenAttributeChange(List<VFileEvent> events, VirtualFile child, boolean oldHidden, boolean newHidden) {
        if (oldHidden != newHidden) {
            scheduleAttributeChange(events, child, VirtualFile.PROP_HIDDEN, oldHidden, newHidden);
        }
    }

    private void checkSymbolicLinkChange(List<VFileEvent> events, VirtualFile child, @Nullable String oldTarget, @Nullable String currentTarget) {
        String currentVfsTarget = currentTarget != null ? FileUtil.toSystemIndependentName(currentTarget) : null;
        if (!Objects.equals(oldTarget, currentVfsTarget)) {
            scheduleAttributeChange(events, child, VirtualFile.PROP_SYMLINK_TARGET, oldTarget, currentVfsTarget);
        }
    }

    private void scheduleAttributeChange(List<VFileEvent> events,
                                         VirtualFile file,
                                         @VirtualFile.PropName String property,
                                         @Nullable Object current,
                                         @Nullable Object upToDate) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("update file=" + file + ' ' + property + '=' + current + "->" + upToDate);
        }
        events.add(new VFilePropertyChangeEvent(REQUESTOR, file, property, current, upToDate, true));
    }

    private static <V> Map<String, V> createFilePathMap(int expectedSize, boolean caseSensitive) {
        return Maps.newHashMap(expectedSize, FilePathHashingStrategy.create(caseSensitive));
    }

    private static Set<String> createFilePathSet(Collection<String> paths, boolean caseSensitive) {
        return Sets.newHashSet(paths, FilePathHashingStrategy.create(caseSensitive));
    }

    private static Interner<String> createCaseInsensitiveNames(Collection<String> names) {
        Interner<String> interner = Interner.createHashInterner(FilePathHashingStrategy.create(false));
        interner.internAll(names);
        return interner;
    }

    static @Nullable Consumer<? super VirtualFile> ourTestListener;

    @TestOnly
    public static void setTestListener(@Nullable Consumer<? super VirtualFile> testListener) {
        ourTestListener = testListener;
    }
}
