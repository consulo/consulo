// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.newvfs.persistent;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.LowMemoryWatcher;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.concurrency.ConcurrentCollectionFactory;
import consulo.ide.impl.idea.concurrency.JobSchedulerImpl;
import consulo.ide.impl.idea.openapi.progress.util.PingProgress;
import consulo.ide.impl.idea.openapi.vfs.PersistentFSConstants;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.openapi.vfs.newvfs.ArchiveFileSystem;
import consulo.ide.impl.idea.openapi.vfs.newvfs.AsyncEventSupport;
import consulo.ide.impl.idea.openapi.vfs.newvfs.ChildInfoImpl;
import consulo.ide.impl.idea.openapi.vfs.newvfs.VfsImplUtil;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.*;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.ArrayUtilRt;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.util.collection.*;
import consulo.util.collection.primitive.ints.*;
import consulo.util.io.*;
import consulo.util.lang.*;
import consulo.util.lang.function.PairFunction;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.encoding.Utf8BomOptionProvider;
import consulo.virtualFileSystem.event.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author max
 */
@ServiceImpl
public final class PersistentFSImpl extends PersistentFS implements Disposable {
    private static final Logger LOG = Logger.getInstance(PersistentFS.class);

    private final Map<String, VirtualFileSystemEntry> myRoots =
        ConcurrentCollectionFactory.createMap(10, 0.4f, JobSchedulerImpl.getCPUCoresCount(), FileUtil.PATH_HASHING_STRATEGY);

    // FS roots must be in this map too. findFileById() relies on this.
    private final ConcurrentIntObjectMap<VirtualFileSystemEntry> myIdToDirCache = ContainerUtil.createConcurrentIntObjectSoftValueMap();
    private final Object myInputLock = new Object();

    private final AtomicBoolean myShutDown = new AtomicBoolean(false);
    private final AtomicInteger myStructureModificationCount = new AtomicInteger();
    private BulkFileListener myPublisher;
    private final VfsData myVfsData = new VfsData();

    public PersistentFSImpl() {
        ShutDownTracker.getInstance().registerShutdownTask(this::performShutdown);
        LowMemoryWatcher.register(this::clearIdCache, this);

        AsyncEventSupport.startListening();

        //Activity activity = StartUpMeasurer.startActivity("connect FSRecords");
        FSRecords.connect();
        //activity.end();
    }

    @Nonnull
    private BulkFileListener getPublisher() {
        BulkFileListener publisher = myPublisher;
        if (publisher == null) {
            // cannot be in constructor, to ensure that lazy listeners will be not created too early
            publisher = Application.get().getMessageBus().syncPublisher(BulkFileListener.class);
            myPublisher = publisher;
        }
        return publisher;
    }

    @Override
    public void dispose() {
        performShutdown();
    }

    private void performShutdown() {
        if (myShutDown.compareAndSet(false, true)) {
            LOG.info("VFS dispose started");
            FSRecords.dispose();
            LOG.info("VFS dispose completed");
        }
    }

    @Override
    public boolean areChildrenLoaded(@Nonnull VirtualFile dir) {
        return areChildrenLoaded(getFileId(dir));
    }

    @Override
    public long getCreationTimestamp() {
        return FSRecords.getCreationTimestamp();
    }

    @Nonnull
    public VirtualFileSystemEntry getOrCacheDir(int id, @Nonnull VirtualDirectoryImpl newDir) {
        VirtualFileSystemEntry dir = myIdToDirCache.get(id);
        if (dir != null) {
            return dir;
        }
        return myIdToDirCache.cacheOrGet(id, newDir);
    }

    public VirtualFileSystemEntry getCachedDir(int id) {
        return myIdToDirCache.get(id);
    }

    @Nonnull
    private static NewVirtualFileSystem getDelegate(@Nonnull VirtualFile file) {
        return (NewVirtualFileSystem)file.getFileSystem();
    }

    @Override
    public boolean wereChildrenAccessed(@Nonnull VirtualFile dir) {
        return FSRecords.wereChildrenAccessed(getFileId(dir));
    }

    @Override
    @Nonnull
    public String[] list(@Nonnull VirtualFile file) {
        FSRecords.NameId[] nameIds = listAll(file);
        return ContainerUtil.map2Array(nameIds, String.class, id -> id.name.toString());
    }

    @Override
    @Nonnull
    public String[] listPersisted(@Nonnull VirtualFile parent) {
        return listPersisted(FSRecords.list(getFileId(parent)));
    }

    @Nonnull
    private static String[] listPersisted(@Nonnull int[] childrenIds) {
        String[] names = ArrayUtil.newStringArray(childrenIds.length);
        for (int i = 0; i < childrenIds.length; i++) {
            names[i] = FSRecords.getName(childrenIds[i]);
        }
        return names;
    }

    @Nonnull
    private static FSRecords.NameId[] persistAllChildren(@Nonnull VirtualFile file, int id, @Nonnull FSRecords.NameId[] current) {
        final NewVirtualFileSystem fs = getDelegate(file);

        String[] delegateNames = VfsUtil.filterNames(fs.list(file));
        if (delegateNames.length == 0 && current.length > 0) {
            return current;
        }

        Set<String> toAdd = Sets.newHashSet(List.of(delegateNames), FileUtil.PATH_HASHING_STRATEGY);
        for (FSRecords.NameId nameId : current) {
            toAdd.remove(nameId.name.toString());
        }

        final IntList childrenIds = IntLists.newArrayList(current.length + toAdd.size());
        final List<FSRecords.NameId> nameIds = new ArrayList<>(current.length + toAdd.size());
        for (FSRecords.NameId nameId : current) {
            childrenIds.add(nameId.id);
            nameIds.add(nameId);
        }
        for (String newName : toAdd) {
            Pair<FileAttributes, String> childData = getChildData(fs, file, newName, null, null);
            if (childData != null) {
                int childId = makeChildRecord(id, newName, childData, fs);
                childrenIds.add(childId);
                nameIds.add(new FSRecords.NameId(childId, FileNameCache.storeName(newName), newName));
            }
        }

        // some clients (e.g. RefreshWorker) expect subsequent list() calls to return equal arrays
        nameIds.sort(Comparator.comparingInt(o -> o.id));
        FSRecords.updateList(id, childrenIds.toArray());
        setChildrenCached(id);

        return nameIds.toArray(FSRecords.NameId.EMPTY_ARRAY);
    }

    private static void setChildrenCached(int id) {
        int flags = FSRecords.getFlags(id);
        FSRecords.setFlags(id, flags | CHILDREN_CACHED_FLAG, true);
    }

    @Override
    @Nonnull
    public FSRecords.NameId[] listAll(@Nonnull VirtualFile file) {
        int id = getFileId(file);

        FSRecords.NameId[] nameIds = FSRecords.listAll(id);
        if (!areChildrenLoaded(id)) {
            nameIds = persistAllChildren(file, id, nameIds);
        }

        return nameIds;
    }

    private static boolean areChildrenLoaded(int parentId) {
        return BitUtil.isSet(FSRecords.getFlags(parentId), CHILDREN_CACHED_FLAG);
    }

    @Override
    @Nullable
    public DataInputStream readAttribute(@Nonnull VirtualFile file, @Nonnull FileAttribute att) {
        return FSRecords.readAttributeWithLock(getFileId(file), att);
    }

    @Override
    @Nonnull
    public DataOutputStream writeAttribute(@Nonnull VirtualFile file, @Nonnull FileAttribute att) {
        return FSRecords.writeAttribute(getFileId(file), att);
    }

    @Nullable
    private static DataInputStream readContent(@Nonnull VirtualFile file) {
        return FSRecords.readContent(getFileId(file));
    }

    @Nonnull
    private static DataInputStream readContentById(int contentId) {
        return FSRecords.readContentById(contentId);
    }

    @Nonnull
    private static DataOutputStream writeContent(@Nonnull VirtualFile file, boolean readOnly) {
        return FSRecords.writeContent(getFileId(file), readOnly);
    }

    private static void writeContent(@Nonnull VirtualFile file, consulo.util.io.ByteArraySequence content, boolean readOnly) {
        FSRecords.writeContent(getFileId(file), content, readOnly);
    }

    @Override
    public int storeUnlinkedContent(@Nonnull byte[] bytes) {
        return FSRecords.storeUnlinkedContent(bytes);
    }

    @Override
    public int getModificationCount(@Nonnull VirtualFile file) {
        return FSRecords.getModCount(getFileId(file));
    }

    @Override
    public int getModificationCount() {
        return FSRecords.getLocalModCount();
    }

    @Override
    public int getStructureModificationCount() {
        return myStructureModificationCount.get();
    }

    public void incStructuralModificationCount() {
        myStructureModificationCount.incrementAndGet();
    }

    @Override
    public int getFilesystemModificationCount() {
        return FSRecords.getModCount();
    }

    private static boolean writeAttributesToRecord(
        int id,
        int parentId,
        @Nonnull CharSequence name,
        @Nonnull NewVirtualFileSystem fs,
        @Nonnull FileAttributes attributes,
        @Nullable String symlinkTarget
    ) {
        assert id > 0 : id;
        assert parentId >= 0 : parentId; // 0 means there's no parent
        if (name.length() != 0) {
            if (namesEqual(fs, name, FSRecords.getNameSequence(id))) {
                return false; // TODO: Handle root attributes change.
            }
        }
        else if (areChildrenLoaded(id)) {
            return false; // TODO: hack
        }

        FSRecords.writeAttributesToRecord(id, parentId, attributes, name.toString());
        if (attributes.isSymLink()) {
            FSRecords.storeSymlinkTarget(id, symlinkTarget);
        }

        return true;
    }

    @Override
    public int getFileAttributes(int id) {
        assert id > 0;
        //noinspection MagicConstant
        return FSRecords.getFlags(id);
    }

    @Override
    public boolean isDirectory(@Nonnull VirtualFile file) {
        return isDirectory(getFileAttributes(getFileId(file)));
    }

    private static boolean namesEqual(@Nonnull VirtualFileSystem fs, @Nonnull CharSequence n1, @Nonnull CharSequence n2) {
        return Comparing.equal(n1, n2, fs.isCaseSensitive());
    }

    @Override
    public boolean exists(@Nonnull VirtualFile fileOrDirectory) {
        return fileOrDirectory.exists();
    }

    @Override
    public long getTimeStamp(@Nonnull VirtualFile file) {
        return FSRecords.getTimestamp(getFileId(file));
    }

    @Override
    public void setTimeStamp(@Nonnull VirtualFile file, long modStamp) throws IOException {
        final int id = getFileId(file);
        FSRecords.setTimestamp(id, modStamp);
        getDelegate(file).setTimeStamp(file, modStamp);
    }

    private static int getFileId(@Nonnull VirtualFile file) {
        return ((VirtualFileWithId)file).getId();
    }

    @Override
    public boolean isSymLink(@Nonnull VirtualFile file) {
        return isSymLink(getFileAttributes(getFileId(file)));
    }

    @Override
    public String resolveSymLink(@Nonnull VirtualFile file) {
        return FSRecords.readSymlinkTarget(getFileId(file));
    }

    @Override
    public boolean isWritable(@Nonnull VirtualFile file) {
        return !BitUtil.isSet(getFileAttributes(getFileId(file)), IS_READ_ONLY);
    }

    @Override
    public boolean isHidden(@Nonnull VirtualFile file) {
        return BitUtil.isSet(getFileAttributes(getFileId(file)), IS_HIDDEN);
    }

    @Override
    @RequiredWriteAction
    public void setWritable(@Nonnull VirtualFile file, boolean writableFlag) throws IOException {
        getDelegate(file).setWritable(file, writableFlag);
        boolean oldWritable = isWritable(file);
        if (oldWritable != writableFlag) {
            processEvent(new VFilePropertyChangeEvent(this, file, VirtualFile.PROP_WRITABLE, oldWritable, writableFlag, false));
        }
    }

    @Override
    public int getId(@Nonnull VirtualFile parent, @Nonnull String childName, @Nonnull NewVirtualFileSystem fs) {
        int parentId = getFileId(parent);
        int[] children = FSRecords.list(parentId);

        int childId = findExistingId(childName, children, fs);
        if (childId > 0) {
            return childId;
        }

        Pair<FileAttributes, String> childData = getChildData(fs, parent, childName, null, null);
        if (childData != null) {
            String oldChildName = childName;
            childName = fs.getCanonicallyCasedName(new FakeVirtualFile(parent, oldChildName));
            if (StringUtil.isEmptyOrSpaces(childName)) {
                return 0;
            }

            if (!childName.equals(oldChildName)) {
                childId = findExistingId(childName, children, fs);
                if (childId > 0) {
                    return childId;
                }
            }
            childId = makeChildRecord(parentId, childName, childData, fs);
            FSRecords.updateList(parentId, ArrayUtil.append(children, childId));

            return childId;
        }

        return 0;
    }

    private static int findExistingId(@Nonnull String childName, @Nonnull int[] childIds, @Nonnull NewVirtualFileSystem fs) {
        if (childIds.length > 0) {
            // fast path, check that some child has same nameId as given name,
            // this avoid O(N) on retrieving names for processing non-cached children
            int nameId = FSRecords.getNameId(childName);
            for (int childId : childIds) {
                if (nameId == FSRecords.getNameId(childId)) {
                    return childId;
                }
            }
            // for case sensitive system the above check is exhaustive in consistent state of vfs
        }

        for (int childId : childIds) {
            if (namesEqual(fs, childName, FSRecords.getNameSequence(childId))) {
                return childId;
            }
        }
        return 0;
    }

    @Override
    public long getLength(@Nonnull VirtualFile file) {
        long length = getLengthIfUpToDate(file);
        return length == -1 ? reloadLengthFromDelegate(file, getDelegate(file)) : length;
    }

    @Override
    public long getLastRecordedLength(@Nonnull VirtualFile file) {
        int id = getFileId(file);
        return FSRecords.getLength(id);
    }

    @Nonnull
    @Override
    @RequiredWriteAction
    public VirtualFile copyFile(
        Object requestor,
        @Nonnull VirtualFile file,
        @Nonnull VirtualFile parent,
        @Nonnull String name
    ) throws IOException {
        getDelegate(file).copyFile(requestor, file, parent, name);
        processEvent(new VFileCopyEvent(requestor, file, parent, name));

        final VirtualFile child = parent.findChild(name);
        if (child == null) {
            throw new IOException("Cannot create child");
        }
        return child;
    }

    @Nonnull
    @Override
    @RequiredWriteAction
    public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException {
        getDelegate(parent).createChildDirectory(requestor, parent, dir);
        processEvent(new VFileCreateEvent(requestor, parent, dir, true, null, null, false, ChildInfo.EMPTY_ARRAY));

        final VirtualFile child = parent.findChild(dir);
        if (child == null) {
            throw new IOException("Cannot create child directory '" + dir + "' at " + parent.getPath());
        }
        return child;
    }

    @Nonnull
    @Override
    @RequiredWriteAction
    public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile parent, @Nonnull String file) throws IOException {
        getDelegate(parent).createChildFile(requestor, parent, file);
        processEvent(new VFileCreateEvent(requestor, parent, file, false, null, null, false, null));

        final VirtualFile child = parent.findChild(file);
        if (child == null) {
            throw new IOException("Cannot create child file '" + file + "' at " + parent.getPath());
        }

        if (child.getCharset().equals(StandardCharsets.UTF_8) && isUtf8BomRequired(child)) {
            child.setBOM(CharsetToolkit.UTF8_BOM);
        }
        return child;
    }

    private static boolean isUtf8BomRequired(@Nonnull VirtualFile file) {
        for (Utf8BomOptionProvider encodingProvider : Utf8BomOptionProvider.EP_NAME.getExtensionList()) {
            if (encodingProvider.shouldAddBOMForNewUtf8File(file)) {
                return true;
            }
        }
        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        EncodingManager encodingManager = project == null ? EncodingManager.getInstance() : EncodingProjectManager.getInstance(project);
        return encodingManager.shouldAddBOMForNewUtf8File();
    }

    @Override
    @RequiredWriteAction
    public void deleteFile(Object requestor, @Nonnull VirtualFile file) throws IOException {
        final NewVirtualFileSystem delegate = getDelegate(file);
        delegate.deleteFile(requestor, file);

        if (!delegate.exists(file)) {
            processEvent(new VFileDeleteEvent(requestor, file, false));
        }
    }

    @Override
    @RequiredWriteAction
    public void renameFile(Object requestor, @Nonnull VirtualFile file, @Nonnull String newName) throws IOException {
        getDelegate(file).renameFile(requestor, file, newName);
        String oldName = file.getName();
        if (!newName.equals(oldName)) {
            processEvent(new VFilePropertyChangeEvent(requestor, file, VirtualFile.PROP_NAME, oldName, newName, false));
        }
    }

    @Override
    @Nonnull
    public byte[] contentsToByteArray(@Nonnull VirtualFile file) throws IOException {
        return contentsToByteArray(file, true);
    }

    @Override
    @Nonnull
    public byte[] contentsToByteArray(@Nonnull VirtualFile file, boolean cacheContent) throws IOException {
        InputStream contentStream = null;
        boolean reloadFromDelegate;
        boolean outdated;
        int fileId;
        long length;

        synchronized (myInputLock) {
            fileId = getFileId(file);
            length = getLengthIfUpToDate(file);
            outdated = length == -1;
            reloadFromDelegate = outdated || (contentStream = readContent(file)) == null;
        }

        if (reloadFromDelegate) {
            final NewVirtualFileSystem delegate = getDelegate(file);

            final byte[] content;
            if (outdated) {
                // in this case, file can have out-of-date length. so, update it first (it's needed for correct contentsToByteArray() work)
                // see IDEA-90813 for possible bugs
                FSRecords.setLength(fileId, delegate.getLength(file));
                content = delegate.contentsToByteArray(file);
            }
            else {
                // a bit of optimization
                content = delegate.contentsToByteArray(file);
                FSRecords.setLength(fileId, content.length);
            }

            Application application = Application.get();
            // we should cache every local files content
            // because the local history feature is currently depends on this cache,
            // perforce offline mode as well
            if ((!delegate.isReadOnly()
                // do not cache archive content unless asked
                || cacheContent && !application.isInternal() && !application.isUnitTestMode())
                && content.length <= PersistentFSConstants.FILE_LENGTH_TO_CACHE_THRESHOLD) {
                synchronized (myInputLock) {
                    writeContent(file, new consulo.util.io.ByteArraySequence(content), delegate.isReadOnly());
                    setFlag(file, MUST_RELOAD_CONTENT, false);
                }
            }

            return content;
        }
        try {
            assert length >= 0 : file;
            return FileUtil.loadBytes(contentStream, (int)length);
        }
        catch (IOException e) {
            FSRecords.handleError(e);
        }
        return ArrayUtilRt.EMPTY_BYTE_ARRAY;
    }

    @Override
    @Nonnull
    public byte[] contentsToByteArray(int contentId) throws IOException {
        final DataInputStream stream = readContentById(contentId);
        return consulo.ide.impl.idea.openapi.util.io.FileUtil.loadBytes(stream);
    }

    @Override
    @Nonnull
    public InputStream getInputStream(@Nonnull VirtualFile file) throws IOException {
        synchronized (myInputLock) {
            InputStream contentStream;
            if (getLengthIfUpToDate(file) == -1 || RawFileLoader.getInstance().isTooLarge(file.getLength())
                || (contentStream = readContent(file)) == null) {
                NewVirtualFileSystem delegate = getDelegate(file);
                long len = reloadLengthFromDelegate(file, delegate);
                InputStream nativeStream = delegate.getInputStream(file);

                if (len > PersistentFSConstants.FILE_LENGTH_TO_CACHE_THRESHOLD) {
                    return nativeStream;
                }
                return createReplicator(file, nativeStream, len, delegate.isReadOnly());
            }
            return contentStream;
        }
    }

    private static long reloadLengthFromDelegate(@Nonnull VirtualFile file, @Nonnull NewVirtualFileSystem delegate) {
        final long len = delegate.getLength(file);
        FSRecords.setLength(getFileId(file), len);
        return len;
    }

    @Nonnull
    private InputStream createReplicator(@Nonnull VirtualFile file, @Nonnull InputStream nativeStream, long fileLength, boolean readOnly) {
        if (nativeStream instanceof BufferExposingByteArrayInputStream byteStream) {
            // optimization
            byte[] bytes = byteStream.getInternalBuffer();
            storeContentToStorage(fileLength, file, readOnly, bytes, bytes.length);
            return nativeStream;
        }
        final BufferExposingByteArrayOutputStream cache = new BufferExposingByteArrayOutputStream((int)fileLength);
        return new ReplicatorInputStream(nativeStream, cache) {
            @Override
            public void close() throws IOException {
                super.close();
                storeContentToStorage(fileLength, file, readOnly, cache.getInternalBuffer(), cache.size());
            }
        };
    }

    private void storeContentToStorage(
        long fileLength,
        @Nonnull VirtualFile file,
        boolean readOnly,
        @Nonnull byte[] bytes,
        int bytesLength
    ) {
        synchronized (myInputLock) {
            if (bytesLength == fileLength) {
                writeContent(file, new consulo.util.io.ByteArraySequence(bytes, 0, bytesLength), readOnly);
                setFlag(file, MUST_RELOAD_CONTENT, false);
            }
            else {
                setFlag(file, MUST_RELOAD_CONTENT, true);
            }
        }
    }

    // returns last recorded length or -1 if must reload from delegate
    private static long getLengthIfUpToDate(@Nonnull VirtualFile file) {
        int fileId = getFileId(file);
        return BitUtil.isSet(FSRecords.getFlags(fileId), MUST_RELOAD_CONTENT) ? -1 : FSRecords.getLength(fileId);
    }

    @Override
    @Nonnull
    public OutputStream getOutputStream(@Nonnull VirtualFile file, Object requestor, long modStamp, long timeStamp) {
        return new ByteArrayOutputStream() {
            private boolean closed; // protection against user calling .close() twice

            @Override
            @RequiredWriteAction
            public void close() throws IOException {
                if (closed) {
                    return;
                }
                super.close();

                Application.get().assertWriteAccessAllowed();

                VFileContentChangeEvent event = new VFileContentChangeEvent(requestor, file, file.getModificationStamp(), modStamp, false);
                List<VFileContentChangeEvent> events = Collections.singletonList(event);
                getPublisher().before(events);

                NewVirtualFileSystem delegate = getDelegate(file);
                // FSRecords.ContentOutputStream already buffered, no need to wrap in BufferedStream
                try (OutputStream persistenceStream = writeContent(file, delegate.isReadOnly())) {
                    persistenceStream.write(buf, 0, count);
                }
                finally {
                    try (OutputStream ioFileStream = delegate.getOutputStream(file, requestor, modStamp, timeStamp)) {
                        ioFileStream.write(buf, 0, count);
                    }
                    finally {
                        closed = true;

                        final FileAttributes attributes = delegate.getAttributes(file);
                        executeTouch(
                            file,
                            false,
                            event.getModificationStamp(),
                            attributes != null ? attributes.length : DEFAULT_LENGTH,
                            // due to fs rounding timestamp of written file can be significantly different from current time
                            attributes != null ? attributes.lastModified : DEFAULT_TIMESTAMP
                        );
                        getPublisher().after(events);
                    }
                }
            }
        };
    }

    @Override
    public int acquireContent(@Nonnull VirtualFile file) {
        return FSRecords.acquireFileContent(getFileId(file));
    }

    @Override
    public void releaseContent(int contentId) {
        FSRecords.releaseContent(contentId);
    }

    @Override
    public int getCurrentContentId(@Nonnull VirtualFile file) {
        return FSRecords.getContentId(getFileId(file));
    }

    @Override
    @RequiredWriteAction
    public void moveFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) throws IOException {
        getDelegate(file).moveFile(requestor, file, newParent);
        processEvent(new VFileMoveEvent(requestor, file, newParent));
    }

    @RequiredWriteAction
    private void processEvent(@Nonnull VFileEvent event) {
        Application.get().assertWriteAccessAllowed();
        // optimisation: skip all groupings
        if (event.isValid()) {
            List<VFileEvent> events = Collections.singletonList(event);
            getPublisher().before(events);

            applyEvent(event);

            getPublisher().after(events);
        }
    }

    // Tries to find a group of non-conflicting events in range [startIndex..inEvents.size()).
    // Two events are conflicting if the originating file of one event is an ancestor (non-strict) of the file from the other.
    // E.g. "change(a/b/c/x.txt)" and "delete(a/b/c)" are conflicting
    // because "a/b/c/x.txt" is under the "a/b/c" directory from the other event.
    //
    // returns index after the last grouped event.
    private static int groupByPath(
        @Nonnull List<? extends VFileEvent> events,
        int startIndex,
        @Nonnull MostlySingularMultiMap<String, VFileEvent> filesInvolved,
        @Nonnull Set<? super String> middleDirsInvolved,
        @Nonnull Set<? super String> deletedPaths,
        @Nonnull Set<? super String> createdPaths,
        @Nonnull Set<? super VFileEvent> eventsToRemove
    ) {
        // store all paths from all events (including all parents)
        // check the each new event's path against this set and if it's there, this event is conflicting

        int i;
        for (i = startIndex; i < events.size(); i++) {
            VFileEvent event = events.get(i);
            String path = event.getPath();
            if (event instanceof VFileDeleteEvent && removeNestedDelete(path, deletedPaths)) {
                eventsToRemove.add(event);
                continue;
            }
            if (event instanceof VFileCreateEvent && !createdPaths.add(path)) {
                eventsToRemove.add(event);
                continue;
            }

            if (checkIfConflictingPaths(event, path, filesInvolved, middleDirsInvolved)) {
                break;
            }
            // some synthetic events really are composite events, e.g. VFileMoveEvent = VFileDeleteEvent+VFileCreateEvent,
            // so both paths should be checked for conflicts
            String path2 = getAlternativePath(event);
            if (path2 != null && !FileUtil.PATH_HASHING_STRATEGY.equals(path2, path) && checkIfConflictingPaths(
                event,
                path2,
                filesInvolved,
                middleDirsInvolved
            )) {
                break;
            }
        }

        return i;
    }

    @Nullable
    private static String getAlternativePath(@Nonnull VFileEvent event) {
        String path2 = null;
        if (event instanceof VFilePropertyChangeEvent vFilePropertyChangeEvent
            && vFilePropertyChangeEvent.getPropertyName().equals(VirtualFile.PROP_NAME)) {
            VFilePropertyChangeEvent pce = (VFilePropertyChangeEvent)event;
            VirtualFile parent = pce.getFile().getParent();
            String newName = (String)pce.getNewValue();
            path2 = parent == null ? newName : parent.getPath() + "/" + newName;
        }
        else if (event instanceof VFileCopyEvent) {
            path2 = event.getFile().getPath();
        }
        else if (event instanceof VFileMoveEvent vme) {
            String newName = vme.getFile().getName();
            path2 = vme.getNewParent().getPath() + "/" + newName;
        }
        return path2;
    }

    private static boolean removeNestedDelete(@Nonnull String path, @Nonnull Set<? super String> deletedPaths) {
        if (!deletedPaths.add(path)) {
            return true;
        }
        int li = path.length();
        while (true) {
            int liPrev = path.lastIndexOf('/', li - 1);
            if (liPrev == -1) {
                break;
            }
            path = path.substring(0, liPrev);
            li = liPrev;
            if (deletedPaths.contains(path)) {
                return true;
            }
        }

        return false;
    }

    private static boolean checkIfConflictingPaths(
        @Nonnull VFileEvent event,
        @Nonnull String path,
        @Nonnull MostlySingularMultiMap<String, VFileEvent> files,
        @Nonnull Set<? super String> middleDirs
    ) {
        Iterable<VFileEvent> stored = files.get(path);
        if (!canReconcileEvents(event, stored)) {
            // conflicting event found for (non-strict) descendant, stop
            return true;
        }
        if (middleDirs.contains(path)) {
            // conflicting event found for (non-strict) descendant, stop
            return true;
        }
        files.add(path, event);
        int li = path.length();
        while (true) {
            int liPrev = path.lastIndexOf('/', li - 1);
            if (liPrev == -1) {
                break;
            }
            String parentDir = path.substring(0, liPrev);
            if (files.containsKey(parentDir)) {
                // conflicting event found for ancestor, stop
                return true;
            }
            if (!middleDirs.add(parentDir)) {
                break;  // all parents up already stored, stop
            }
            li = liPrev;
        }

        return false;
    }

    // true if {@code event} and events in {@code stored} can be applied in one batch.
    // E.g. "content change" and {"writable property change", "content change"}
    private static boolean canReconcileEvents(@Nonnull VFileEvent event, @Nonnull Iterable<? extends VFileEvent> stored) {
        return ContainerUtil.and(stored, e -> canReconcile(event, e));
    }

    private static boolean canReconcile(@Nonnull VFileEvent event1, @Nonnull VFileEvent event2) {
        return isContentChangeLikeHarmlessEvent(event1) && isContentChangeLikeHarmlessEvent(event2);
    }

    private static boolean isContentChangeLikeHarmlessEvent(@Nonnull VFileEvent event1) {
        return event1 instanceof VFileContentChangeEvent
            || event1 instanceof VFilePropertyChangeEvent vFilePrpChgEvt && (
            vFilePrpChgEvt.getPropertyName().equals(VirtualFile.PROP_WRITABLE)
                || vFilePrpChgEvt.getPropertyName().equals(VirtualFile.PROP_ENCODING
            )
        );
    }

    // finds a group of non-conflicting events, validate them.
    // "outApplyEvents" will contain handlers for applying the grouped events
    // "outValidatedEvents" will contain events for which VFileEvent.isValid() is true
    // return index after the last processed event
    private int groupAndValidate(
        @Nonnull List<? extends VFileEvent> events,
        int startIndex,
        @Nonnull List<? super Runnable> outApplyEvents,
        @Nonnull List<? super VFileEvent> outValidatedEvents,
        @Nonnull MostlySingularMultiMap<String, VFileEvent> filesInvolved,
        @Nonnull Set<? super String> middleDirsInvolved
    ) {
        Set<VFileEvent> toIgnore = Sets.newHashSet(ContainerUtil.identityStrategy()); // VFileEvents override equals()
        int endIndex = groupByPath(
            events,
            startIndex,
            filesInvolved,
            middleDirsInvolved,
            Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY),
            Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY),
            toIgnore
        );
        assert endIndex > startIndex : events.get(startIndex) + "; files: " + filesInvolved + "; middleDirs: " + middleDirsInvolved;
        // since all events in the group events[startIndex..endIndex) are mutually non-conflicting,
        // we can re-arrange creations/deletions together
        groupCreations(events, startIndex, endIndex, outValidatedEvents, outApplyEvents, toIgnore);
        groupDeletions(events, startIndex, endIndex, outValidatedEvents, outApplyEvents, toIgnore);
        groupOthers(events, startIndex, endIndex, outValidatedEvents, outApplyEvents);

        return endIndex;
    }

    // find all VFileCreateEvent events in [start..end)
    // group them by parent directory, validate in bulk for each directory, and return "applyCreations()" runnable
    private void groupCreations(
        @Nonnull List<? extends VFileEvent> events,
        int start,
        int end,
        @Nonnull List<? super VFileEvent> outValidated,
        @Nonnull List<? super Runnable> outApplyEvents,
        @Nonnull Set<? extends VFileEvent> toIgnore
    ) {
        MultiMap<VirtualDirectoryImpl, VFileCreateEvent> grouped = null;

        for (int i = start; i < end; i++) {
            VFileEvent e = events.get(i);
            if (!(e instanceof VFileCreateEvent) || toIgnore.contains(e)) {
                continue;
            }
            VFileCreateEvent event = (VFileCreateEvent)e;
            VirtualDirectoryImpl parent = (VirtualDirectoryImpl)event.getParent();
            if (grouped == null) {
                grouped = new MultiMap<>(new HashMap<>(end - start));
            }
            grouped.putValue(parent, event);
        }
        if (grouped != null) {
            // since the VCreateEvent.isValid() is extremely expensive, combine all creation events for the directory together
            // and use VirtualDirectoryImpl.validateChildrenToCreate() optimised for bulk validation
            boolean hasValidEvents = false;
            for (Map.Entry<VirtualDirectoryImpl, Collection<VFileCreateEvent>> entry : grouped.entrySet()) {
                VirtualDirectoryImpl directory = entry.getKey();
                List<VFileCreateEvent> createEvents = (List<VFileCreateEvent>)entry.getValue();
                directory.validateChildrenToCreate(createEvents);
                hasValidEvents |= !createEvents.isEmpty();
                outValidated.addAll(createEvents);
            }

            if (hasValidEvents) {
                MultiMap<VirtualDirectoryImpl, VFileCreateEvent> finalGrouped = grouped;
                outApplyEvents.add((Runnable)() -> {
                    applyCreations(finalGrouped);
                    incStructuralModificationCount();
                });
            }
        }
    }

    // find all VFileDeleteEvent events in [start..end)
    // group them by parent directory (can be null), filter out files which parent dir is to be deleted too,
    // and return "applyDeletions()" runnable
    private void groupDeletions(
        @Nonnull List<? extends VFileEvent> events,
        int start,
        int end,
        @Nonnull List<? super VFileEvent> outValidated,
        @Nonnull List<? super Runnable> outApplyEvents,
        @Nonnull Set<? extends VFileEvent> toIgnore
    ) {
        MultiMap<VirtualDirectoryImpl, VFileDeleteEvent> grouped = null;
        boolean hasValidEvents = false;
        for (int i = start; i < end; i++) {
            VFileEvent event = events.get(i);
            if (!(event instanceof VFileDeleteEvent) || toIgnore.contains(event) || !event.isValid()) {
                continue;
            }
            VFileDeleteEvent de = (VFileDeleteEvent)event;
            @Nullable VirtualDirectoryImpl parent = (VirtualDirectoryImpl)de.getFile().getParent();
            if (grouped == null) {
                grouped = new MultiMap<>(new HashMap<>(end - start));
            }
            grouped.putValue(parent, de);
            outValidated.add(event);
            hasValidEvents = true;
        }

        if (hasValidEvents) {
            MultiMap<VirtualDirectoryImpl, VFileDeleteEvent> finalGrouped = grouped;
            outApplyEvents.add((Runnable)() -> {
                clearIdCache();
                applyDeletions(finalGrouped);
                incStructuralModificationCount();
            });
        }
    }

    // find events other than VFileCreateEvent or VFileDeleteEvent in [start..end)
    // validate and return "applyEvent()" runnable for each event because it's assumed there won't be too many of them
    private void groupOthers(
        @Nonnull List<? extends VFileEvent> events,
        int start,
        int end,
        @Nonnull List<? super VFileEvent> outValidated,
        @Nonnull List<? super Runnable> outApplyEvents
    ) {
        for (int i = start; i < end; i++) {
            VFileEvent event = events.get(i);
            if (event instanceof VFileCreateEvent || event instanceof VFileDeleteEvent || !event.isValid()) {
                continue;
            }
            outValidated.add(event);
            outApplyEvents.add((Runnable)() -> applyEvent(event));
        }
    }

    private static final int INNER_ARRAYS_THRESHOLD = 1024; // max initial size, to avoid OOM on million-events processing

    @Override
    @RequiredWriteAction
    public void processEvents(@Nonnull List<? extends VFileEvent> events) {
        Application.get().assertWriteAccessAllowed();

        int startIndex = 0;
        int cappedInitialSize = Math.min(events.size(), INNER_ARRAYS_THRESHOLD);
        List<Runnable> applyEvents = new ArrayList<>(cappedInitialSize);
        MostlySingularMultiMap<String, VFileEvent> files = new MostlySingularMultiMap<>() {
            @Nonnull
            @Override
            protected Map<String, Object> createMap() {
                return Maps.newHashMap(cappedInitialSize, FileUtil.PATH_HASHING_STRATEGY);
            }
        };
        Set<String> middleDirs = Sets.newHashSet(cappedInitialSize, FileUtil.PATH_HASHING_STRATEGY);
        List<VFileEvent> validated = new ArrayList<>(cappedInitialSize);
        BulkFileListener publisher = getPublisher();
        while (startIndex != events.size()) {
            PingProgress.interactWithEdtProgress();

            applyEvents.clear();
            files.clear();
            middleDirs.clear();
            validated.clear();
            startIndex = groupAndValidate(events, startIndex, applyEvents, validated, files, middleDirs);

            if (!validated.isEmpty()) {
                PingProgress.interactWithEdtProgress();
                // do defensive copy to cope with ill-written listeners that save passed list for later processing
                List<VFileEvent> toSend = ContainerUtil.immutableList(validated.toArray(new VFileEvent[0]));
                publisher.before(toSend);

                PingProgress.interactWithEdtProgress();
                applyEvents.forEach(Runnable::run);

                PingProgress.interactWithEdtProgress();
                publisher.after(toSend);
            }
        }
    }

    // remove children from specified directories using VirtualDirectoryImpl.removeChildren() optimised for bulk removals
    private void applyDeletions(@Nonnull MultiMap<VirtualDirectoryImpl, VFileDeleteEvent> deletions) {
        for (Map.Entry<VirtualDirectoryImpl, Collection<VFileDeleteEvent>> entry : deletions.entrySet()) {
            VirtualDirectoryImpl parent = entry.getKey();
            Collection<VFileDeleteEvent> deleteEvents = entry.getValue();
            // no valid containing directory, apply events the old way - one by one
            if (parent == null || !parent.isValid()) {
                deleteEvents.forEach(this::applyEvent);
                return;
            }

            int parentId = getFileId(parent);
            int[] oldIds = FSRecords.list(parentId);
            IntSet parentChildrenIds = IntSets.newHashSet(oldIds);

            List<CharSequence> childrenNamesDeleted = new ArrayList<>(deleteEvents.size());
            IntSet childrenIdsDeleted = IntSets.newHashSet(deleteEvents.size());

            for (VFileDeleteEvent event : deleteEvents) {
                VirtualFile file = event.getFile();
                int id = getFileId(file);
                childrenNamesDeleted.add(file.getNameSequence());
                childrenIdsDeleted.add(id);
                FSRecords.deleteRecordRecursively(id);
                invalidateSubtree(file);
                parentChildrenIds.remove(id);
            }
            FSRecords.updateList(parentId, parentChildrenIds.toArray());
            parent.removeChildren(childrenIdsDeleted, childrenNamesDeleted);
        }
    }

    // add children to specified directories using VirtualDirectoryImpl.createAndAddChildren() optimised for bulk additions
    private void applyCreations(@Nonnull MultiMap<VirtualDirectoryImpl, VFileCreateEvent> creations) {
        for (Map.Entry<VirtualDirectoryImpl, Collection<VFileCreateEvent>> entry : creations.entrySet()) {
            VirtualDirectoryImpl parent = entry.getKey();
            Collection<VFileCreateEvent> createEvents = entry.getValue();
            applyCreateEventsInDirectory(parent, createEvents);
        }
    }

    private void applyCreateEventsInDirectory(
        @Nonnull VirtualDirectoryImpl parent,
        @Nonnull Collection<? extends VFileCreateEvent> createEvents
    ) {
        int parentId = getFileId(parent);
        NewVirtualFile vf = findFileById(parentId);
        if (!(vf instanceof VirtualDirectoryImpl)) {
            return;
        }
        // retain in myIdToDirCache at least for the duration of this block in order to subsequent findFileById() won't crash
        parent = (VirtualDirectoryImpl)vf;
        final NewVirtualFileSystem delegate = getDelegate(parent);
        IntSet parentChildrenIds = IntSets.newHashSet(createEvents.size());

        List<ChildInfo> childrenAdded = getOrCreateChildInfos(
            parent,
            createEvents,
            VFileCreateEvent::getChildName,
            parentChildrenIds,
            delegate,
            (createEvent, childId) -> {
                createEvent.resetCache();
                String name = createEvent.getChildName();
                Pair<FileAttributes, String> childData =
                    getChildData(delegate, createEvent.getParent(), name, createEvent.getAttributes(), createEvent.getSymlinkTarget());
                if (childData == null) {
                    return null;
                }
                childId = makeChildRecord(parentId, name, childData, delegate);
                return new ChildInfoImpl(childId, name, childData.first, createEvent.getChildren(), createEvent.getSymlinkTarget());
            }
        );
        FSRecords.updateList(parentId, parentChildrenIds.toArray());
        parent.createAndAddChildren(childrenAdded, false, (__, ___) -> {
        });

        saveScannedChildrenRecursively(createEvents, delegate);
    }

    private static void saveScannedChildrenRecursively(
        @Nonnull Collection<? extends VFileCreateEvent> createEvents,
        @Nonnull NewVirtualFileSystem delegate
    ) {
        for (VFileCreateEvent createEvent : createEvents) {
            ChildInfo[] children = createEvent.getChildren();
            if (children == null || !createEvent.isDirectory()) {
                continue;
            }
            // todo avoid expensive findFile
            VirtualFile createdDir = createEvent.getFile();
            if (createdDir instanceof VirtualDirectoryImpl virtualDirectory) {
                Queue<Pair<VirtualDirectoryImpl, ChildInfo[]>> queue = new ArrayDeque<>();
                queue.add(Pair.create(virtualDirectory, children));
                while (!queue.isEmpty()) {
                    Pair<VirtualDirectoryImpl, ChildInfo[]> queued = queue.remove();
                    VirtualDirectoryImpl directory = queued.first;
                    IntSet childIds = IntSets.newHashSet();
                    List<ChildInfo> scannedChildren = Arrays.asList(queued.second);
                    List<ChildInfo> added = getOrCreateChildInfos(
                        directory,
                        scannedChildren,
                        childInfo -> childInfo.getName(),
                        childIds,
                        delegate,
                        (childInfo, childId) -> {
                            // passed children have no ChildInfo.id, have to create new ones
                            if (childId < 0) {
                                String childName = childInfo.getName().toString();
                                Pair<FileAttributes, String> childData = getChildData(
                                    delegate,
                                    directory,
                                    childName,
                                    childInfo.getFileAttributes(),
                                    childInfo.getSymLinkTarget()
                                );
                                if (childData == null) {
                                    return null;
                                }
                                childId = makeChildRecord(directory.getId(), childName, childData, delegate);
                            }
                            return new ChildInfoImpl(
                                childId,
                                childInfo.getNameId(),
                                childInfo.getFileAttributes(),
                                childInfo.getChildren(),
                                childInfo.getSymLinkTarget()
                            );
                        }
                    );

                    FSRecords.updateList(directory.getId(), childIds.toArray());
                    setChildrenCached(directory.getId());
                    // set "all children loaded" because the first "fileCreated" listener (looking at you, local history)
                    // will call getChildren() anyway, beyond a shadow of a doubt
                    directory.createAndAddChildren(added, true, (childCreated, childInfo) -> {
                        // enqueue recursive children
                        if (childCreated instanceof VirtualDirectoryImpl childVDir && childInfo.getChildren() != null) {
                            queue.add(Pair.create(childVDir, childInfo.getChildren()));
                        }
                    });
                }
            }
        }
    }

    // create ChildInfos and fill parentChildrenIds from createEvents.
    // N.B. be aware that the child can already exist for particular VFileCreateEvent,
    // due to asynchronicity of "refresh created directory" process
    @Nonnull
    private static <T> List<ChildInfo> getOrCreateChildInfos(
        @Nonnull VirtualDirectoryImpl parent,
        @Nonnull Collection<? extends T> createEvents,
        @Nonnull Function<? super T, ? extends CharSequence> nameExtractor,
        @Nonnull IntSet parentChildrenIds,
        @Nonnull NewVirtualFileSystem delegate,
        @Nonnull PairFunction<? super T, ? super Integer, ? extends ChildInfo> convertor
    ) {
        int parentId = parent.getId();
        FSRecords.NameId[] oldNameIds = FSRecords.listAll(parentId);
        int[] oldIds = new int[oldNameIds.length];
        CharSequenceHashingStrategy strategy = delegate.isCaseSensitive()
            ? CharSequenceHashingStrategy.CASE_SENSITIVE : CharSequenceHashingStrategy.CASE_INSENSITIVE;
        Set<CharSequence> persistedNames = Sets.newHashSet(oldNameIds.length, strategy);
        for (int i = 0; i < oldNameIds.length; i++) {
            FSRecords.NameId nameId = oldNameIds[i];
            parentChildrenIds.add(nameId.id);
            persistedNames.add(nameId.name);
            oldIds[i] = nameId.id;
        }

        List<ChildInfo> childrenAdded = new ArrayList<>(createEvents.size());
        for (T createEvent : createEvents) {
            CharSequence name = nameExtractor.apply(createEvent);
            int childId = -1;
            if (persistedNames.contains(name)) {
                childId = findExistingId(name.toString(), oldIds, delegate);
            }
            ChildInfo childInfo = convertor.fun(createEvent, childId);
            if (childInfo == null) {
                continue;
            }
            childrenAdded.add(childInfo);
            parentChildrenIds.add(childInfo.getId());
        }
        return childrenAdded;
    }

    @Override
    @Nullable
    public VirtualFileSystemEntry findRoot(@Nonnull String path, @Nonnull NewVirtualFileSystem fs) {
        if (path.isEmpty()) {
            LOG.error("Invalid root, fs=" + fs);
            return null;
        }

        String optimisticUrl = VirtualFileManager.constructUrl(fs.getProtocol(), StringUtil.trimTrailing(path, '/'));
        VirtualFileSystemEntry root = myRoots.get(optimisticUrl);
        if (root != null) {
            return root;
        }
        String rootUrl = normalizeRootUrl(path, fs);

        root = rootUrl.equals(optimisticUrl) ? null : myRoots.get(rootUrl);
        if (root != null) {
            return root;
        }

        CharSequence rootName;
        String rootPath;
        if (fs instanceof ArchiveFileSystem afs) {
            VirtualFile localFile = afs.findLocalByRootPath(path);
            if (localFile == null) {
                return null;
            }
            rootName = localFile.getNameSequence();
            rootPath = afs.getRootPathByLocal(localFile); // make sure to not create FsRoot with ".." garbage in path
        }
        else {
            rootName = rootPath = path;
        }

        FileAttributes attributes = fs.getAttributes(new StubVirtualFile() {
            @Nonnull
            @Override
            public String getPath() {
                return rootPath;
            }

            @Nullable
            @Override
            public VirtualFile getParent() {
                return null;
            }
        });
        if (attributes == null || !attributes.isDirectory()) {
            return null;
        }

        // avoid creating zillion of roots which are not actual roots
        String parentPath = fs instanceof LocalFileSystem ? PathUtil.getParentPath(rootPath) : "";
        if (!parentPath.isEmpty()) {
            FileAttributes parentAttributes = fs.getAttributes(new StubVirtualFile() {
                @Nonnull
                @Override
                public String getPath() {
                    return parentPath;
                }

                @Nullable
                @Override
                public VirtualFile getParent() {
                    return null;
                }
            });
            if (parentAttributes != null) {
                throw new IllegalArgumentException(
                    "Must pass FS root path, but got: '" + path + "', which has a parent '" + parentPath + "')." +
                        " Use NewVirtualFileSystem.extractRootPath() for obtaining root path"
                );
            }
        }

        int rootId = FSRecords.findRootRecord(rootUrl);

        int rootNameId = FileNameCache.storeName(rootName.toString());
        boolean mark;
        VirtualFileSystemEntry newRoot;
        synchronized (myRoots) {
            root = myRoots.get(rootUrl);
            if (root != null) {
                return root;
            }

            try {
                newRoot = new FsRoot(rootId, rootNameId, myVfsData, fs, StringUtil.trimTrailing(rootPath, '/'));
            }
            catch (VfsData.FileAlreadyCreatedException e) {
                for (Map.Entry<String, VirtualFileSystemEntry> entry : myRoots.entrySet()) {
                    final VirtualFileSystemEntry existingRoot = entry.getValue();
                    if (existingRoot.getId() == rootId) {
                        String message =
                            "Duplicate FS roots: " + rootUrl + " / " + entry.getKey() + " id=" + rootId + " valid=" + existingRoot.isValid();
                        throw new RuntimeException(message, e);
                    }
                }
                throw new RuntimeException(
                    "No root duplication," +
                        " rootName='" + rootName + "';" +
                        " rootNameId=" + rootNameId + ";" +
                        " rootId=" + rootId + ";" +
                        " path='" + path + "';" +
                        " fs=" + fs + ";" +
                        " rootUrl='" + rootUrl + "'",
                    e
                );
            }
            incStructuralModificationCount();
            mark = writeAttributesToRecord(rootId, 0, rootName, fs, attributes, null);

            myRoots.put(rootUrl, newRoot);
            myIdToDirCache.put(rootId, newRoot);
        }

        if (!mark && attributes.lastModified != FSRecords.getTimestamp(rootId)) {
            newRoot.markDirtyRecursively();
        }

        LOG.assertTrue(rootId == newRoot.getId(), "root=" + newRoot + " expected=" + rootId + " actual=" + newRoot.getId());

        return newRoot;
    }

    @Nonnull
    private static String normalizeRootUrl(@Nonnull String basePath, @Nonnull NewVirtualFileSystem fs) {
        // need to protect against relative path of the form "/x/../y"
        String normalized = VfsImplUtil.normalize(fs, FileUtil.toCanonicalPath(basePath));
        return VirtualFileManager.constructUrl(fs.getProtocol(), UriUtil.trimTrailingSlashes(normalized));
    }

    @Override
    public void clearIdCache() {
        // remove all except roots
        myIdToDirCache.entrySet().removeIf(e -> e.getValue().getParent() != null);
    }

    @Override
    @Nullable
    public NewVirtualFile findFileById(int id) {
        VirtualFileSystemEntry cached = myIdToDirCache.get(id);
        return cached != null ? cached : FSRecords.findFileById(id, myIdToDirCache);
    }

    @Override
    public NewVirtualFile findFileByIdIfCached(int id) {
        return myVfsData.hasLoadedFile(id) ? findFileById(id) : null;
    }

    @Override
    @Nonnull
    public VirtualFile[] getRoots() {
        Collection<VirtualFileSystemEntry> roots = myRoots.values();
        return VfsUtilCore.toVirtualFileArray(roots); // ConcurrentHashMap.keySet().toArray(new T[0]) guaranteed to return array with no nulls
    }

    @Override
    @Nonnull
    public VirtualFile[] getRoots(@Nonnull NewVirtualFileSystem fs) {
        final List<VirtualFile> roots = new ArrayList<>();

        for (NewVirtualFile root : myRoots.values()) {
            if (root.getFileSystem() == fs) {
                roots.add(root);
            }
        }

        return VfsUtilCore.toVirtualFileArray(roots);
    }

    @Override
    @Nonnull
    public VirtualFile[] getLocalRoots() {
        List<VirtualFile> roots = new SmartList<>();

        for (NewVirtualFile root : myRoots.values()) {
            if (root.isInLocalFileSystem() && !(root.getFileSystem() instanceof TempFileSystem)) {
                roots.add(root);
            }
        }
        return VfsUtilCore.toVirtualFileArray(roots);
    }

    private void applyEvent(@Nonnull VFileEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Applying " + event);
        }
        try {
            if (event instanceof VFileCreateEvent ce) {
                executeCreateChild(ce.getParent(), ce.getChildName(), ce.getAttributes(), ce.getSymlinkTarget(), ce.isEmptyDirectory());
            }
            else if (event instanceof VFileDeleteEvent deleteEvent) {
                executeDelete(deleteEvent.getFile());
            }
            else if (event instanceof VFileContentChangeEvent contentUpdateEvent) {
                VirtualFile file = contentUpdateEvent.getFile();
                long length = contentUpdateEvent.getNewLength();
                long timestamp = contentUpdateEvent.getNewTimestamp();

                if (!contentUpdateEvent.isLengthAndTimestampDiffProvided()) {
                    final NewVirtualFileSystem delegate = getDelegate(file);
                    final FileAttributes attributes = delegate.getAttributes(file);
                    length = attributes != null ? attributes.length : DEFAULT_LENGTH;
                    timestamp = attributes != null ? attributes.lastModified : DEFAULT_TIMESTAMP;
                }

                executeTouch(file, contentUpdateEvent.isFromRefresh(), contentUpdateEvent.getModificationStamp(), length, timestamp);
            }
            else if (event instanceof VFileCopyEvent copyEvent) {
                executeCreateChild(
                    copyEvent.getNewParent(),
                    copyEvent.getNewChildName(),
                    null,
                    null,
                    copyEvent.getFile().getChildren().length == 0
                );
            }
            else if (event instanceof VFileMoveEvent moveEvent) {
                executeMove(moveEvent.getFile(), moveEvent.getNewParent());
            }
            else if (event instanceof VFilePropertyChangeEvent propertyChangeEvent) {
                VirtualFile file = propertyChangeEvent.getFile();
                Object newValue = propertyChangeEvent.getNewValue();
                switch (propertyChangeEvent.getPropertyName()) {
                    case VirtualFile.PROP_NAME:
                        executeRename(file, (String)newValue);
                        break;
                    case VirtualFile.PROP_WRITABLE:
                        executeSetWritable(file, (Boolean)newValue);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("File " + file + " writable=" + file.isWritable() + " id=" + getFileId(file));
                        }
                        break;
                    case VirtualFile.PROP_HIDDEN:
                        executeSetHidden(file, (Boolean)newValue);
                        break;
                    case VirtualFile.PROP_SYMLINK_TARGET:
                        executeSetTarget(file, (String)newValue);
                        markForContentReloadRecursively(getFileId(file));
                        break;
                }
            }
        }
        catch (Exception e) {
            // Exception applying single event should not prevent other events from applying.
            LOG.error(e);
        }
    }

    @Override
    @Nonnull
    @NonNls
    public String toString() {
        return "PersistentFS";
    }

    private void executeCreateChild(
        @Nonnull VirtualFile parent,
        @Nonnull String name,
        @Nullable FileAttributes attributes,
        @Nullable String symlinkTarget,
        boolean isEmptyDirectory
    ) {
        NewVirtualFileSystem delegate = getDelegate(parent);
        int parentId = getFileId(parent);
        Pair<FileAttributes, String> childData = getChildData(delegate, parent, name, attributes, symlinkTarget);
        if (childData != null) {
            int childId = makeChildRecord(parentId, name, childData, delegate);
            appendIdToParentList(parentId, childId);
            assert parent instanceof VirtualDirectoryImpl : parent;
            VirtualDirectoryImpl dir = (VirtualDirectoryImpl)parent;
            VirtualFileSystemEntry child = dir.createChild(name, childId, dir.getFileSystem(), childData.first, isEmptyDirectory);
            if (isEmptyDirectory) {
                // when creating empty directory we need to make sure every file crested inside will fire "file created" event
                // in order to virtual file pointer manager get those events to update its pointers properly
                // (because currently VirtualFilePointerManager ignores empty directory creation events for performance reasons)
                setChildrenCached(childId);
            }
            dir.addChild(child);
            incStructuralModificationCount();
        }
    }

    private static int makeChildRecord(
        int parentId,
        @Nonnull CharSequence name,
        @Nonnull Pair<FileAttributes, String> childData,
        @Nonnull NewVirtualFileSystem fs
    ) {
        int childId = FSRecords.createRecord();
        writeAttributesToRecord(childId, parentId, name, fs, childData.first, childData.second);
        assert childId > 0 : childId;
        return childId;
    }

    @Nullable // null when file not found
    private static Pair<FileAttributes, String> getChildData(
        @Nonnull NewVirtualFileSystem fs,
        @Nonnull VirtualFile parent,
        @Nonnull String name,
        @Nullable FileAttributes attributes,
        @Nullable String symlinkTarget
    ) {
        if (attributes == null) {
            FakeVirtualFile virtualFile = new FakeVirtualFile(parent, name);
            attributes = fs.getAttributes(virtualFile);
            symlinkTarget = attributes != null && attributes.isSymLink() ? fs.resolveSymLink(virtualFile) : null;
        }
        return attributes == null ? null : Pair.create(attributes, symlinkTarget);
    }

    private static void appendIdToParentList(int parentId, int childId) {
        int[] childrenList = FSRecords.list(parentId);
        childrenList = ArrayUtil.append(childrenList, childId);
        FSRecords.updateList(parentId, childrenList);
    }

    private void executeDelete(@Nonnull VirtualFile file) {
        if (!file.exists()) {
            LOG.error("Deleting a file which does not exist: " + ((VirtualFileWithId)file).getId() + " " + file.getPath());
            return;
        }
        clearIdCache();

        int id = getFileId(file);

        final VirtualFile parent = file.getParent();
        final int parentId = parent == null ? 0 : getFileId(parent);

        if (parentId == 0) {
            String rootUrl = normalizeRootUrl(file.getPath(), (NewVirtualFileSystem)file.getFileSystem());
            synchronized (myRoots) {
                myRoots.remove(rootUrl);
                myIdToDirCache.remove(id);
                FSRecords.deleteRootRecord(id);
            }
        }
        else {
            removeIdFromParentList(parentId, id, parent, file);
            VirtualDirectoryImpl directory = (VirtualDirectoryImpl)file.getParent();
            assert directory != null : file;
            directory.removeChild(file);
        }

        FSRecords.deleteRecordRecursively(id);

        invalidateSubtree(file);
        incStructuralModificationCount();
    }

    private static void invalidateSubtree(@Nonnull VirtualFile file) {
        final VirtualFileSystemEntry impl = (VirtualFileSystemEntry)file;
        impl.invalidate();
        for (VirtualFile child : impl.getCachedChildren()) {
            invalidateSubtree(child);
        }
    }

    private static void removeIdFromParentList(int parentId, int id, @Nonnull VirtualFile parent, VirtualFile file) {
        int[] childList = FSRecords.list(parentId);

        int index = ArrayUtil.indexOf(childList, id);
        if (index == -1) {
            throw new RuntimeException(
                "Cannot find child (" + id + ")" + file + "\n\tin (" + parentId + ")" + parent + "\n" +
                    "\tactual children:" + Arrays.toString(childList)
            );
        }
        childList = ArrayUtil.remove(childList, index);
        FSRecords.updateList(parentId, childList);
    }

    private static void executeRename(@Nonnull VirtualFile file, @Nonnull String newName) {
        final int id = getFileId(file);
        FSRecords.setName(id, newName);
        ((VirtualFileSystemEntry)file).setNewName(newName);
    }

    private static void executeSetWritable(@Nonnull VirtualFile file, boolean writableFlag) {
        setFlag(file, IS_READ_ONLY, !writableFlag);
        ((VirtualFileSystemEntry)file).updateProperty(VirtualFile.PROP_WRITABLE, writableFlag);
    }

    private static void executeSetHidden(@Nonnull VirtualFile file, boolean hiddenFlag) {
        setFlag(file, IS_HIDDEN, hiddenFlag);
        ((VirtualFileSystemEntry)file).updateProperty(VirtualFile.PROP_HIDDEN, hiddenFlag);
    }

    private static void executeSetTarget(@Nonnull VirtualFile file, @Nullable String target) {
        FSRecords.storeSymlinkTarget(getFileId(file), target);
    }

    private static void setFlag(@Nonnull VirtualFile file, int mask, boolean value) {
        setFlag(getFileId(file), mask, value);
    }

    private static void setFlag(int id, int mask, boolean value) {
        int oldFlags = FSRecords.getFlags(id);
        int flags = value ? oldFlags | mask : oldFlags & ~mask;

        if (oldFlags != flags) {
            FSRecords.setFlags(id, flags, true);
        }
    }

    private static void executeTouch(
        @Nonnull VirtualFile file,
        boolean reloadContentFromDelegate,
        long newModificationStamp,
        long newLength,
        long newTimestamp
    ) {
        if (reloadContentFromDelegate) {
            setFlag(file, MUST_RELOAD_CONTENT, true);
        }

        int fileId = getFileId(file);
        FSRecords.setLength(fileId, newLength);
        FSRecords.setTimestamp(fileId, newTimestamp);

        ((VirtualFileSystemEntry)file).setModificationStamp(newModificationStamp);
    }

    private void executeMove(@Nonnull VirtualFile file, @Nonnull VirtualFile newParent) {
        clearIdCache();

        final int fileId = getFileId(file);
        final int newParentId = getFileId(newParent);
        final int oldParentId = getFileId(file.getParent());

        removeIdFromParentList(oldParentId, fileId, file.getParent(), file);
        FSRecords.setParent(fileId, newParentId);
        appendIdToParentList(newParentId, fileId);
        ((VirtualFileSystemEntry)file).setParent(newParent);
    }

    @Override
    public String getName(int id) {
        assert id > 0;
        return FSRecords.getName(id);
    }

    @TestOnly
    public void cleanPersistedContent(int id) {
        doCleanPersistedContent(id);
    }

    @TestOnly
    public void cleanPersistedContents() {
        int[] roots = FSRecords.listRoots();
        for (int root : roots) {
            markForContentReloadRecursively(root);
        }
    }

    private void markForContentReloadRecursively(int id) {
        if (isDirectory(getFileAttributes(id))) {
            for (int child : FSRecords.list(id)) {
                markForContentReloadRecursively(child);
            }
        }
        else {
            doCleanPersistedContent(id);
        }
    }

    private static void doCleanPersistedContent(int id) {
        setFlag(id, MUST_RELOAD_CONTENT, true);
    }

    @Override
    public boolean mayHaveChildren(int id) {
        return FSRecords.mayHaveChildren(id);
    }
}