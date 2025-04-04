// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.impl.local;

import consulo.application.Application;
import consulo.application.io.SafeOutputStreamFactory;
import consulo.ide.impl.idea.openapi.vfs.SafeWriteRequestor;
import consulo.ide.impl.idea.openapi.vfs.newvfs.VfsImplUtil;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.FakeVirtualFile;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileTooBigException;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.ThrowableConsumer;
import consulo.virtualFileSystem.*;
import consulo.virtualFileSystem.impl.internal.DiskQueryRelay;
import consulo.virtualFileSystem.impl.internal.mediator.FileSystemUtil;
import consulo.virtualFileSystem.internal.VirtualFileManagerEx;
import consulo.virtualFileSystem.localize.VirtualFileSystemLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public abstract class LocalFileSystemBase extends LocalFileSystem {
    protected static final Logger LOG = Logger.getInstance(LocalFileSystemBase.class);

    private static final FileAttributes UNC_ROOT_ATTRIBUTES =
        new FileAttributes(true, false, false, false, DEFAULT_LENGTH, DEFAULT_TIMESTAMP, false);

    private final List<LocalFileOperationsHandler> myHandlers = new ArrayList<>();

    private final DiskQueryRelay<VirtualFile, FileAttributes> myAttrGetter =
        new DiskQueryRelay<>(LocalFileSystemBase::getAttributesWithCustomTimestamp);
    private final DiskQueryRelay<Path, String[]> myNioChildrenGetter = new DiskQueryRelay<>(LocalFileSystemBase::listPathChildren);

    @Override
    @Nullable
    public VirtualFile findFileByPath(@Nonnull String path) {
        return VfsImplUtil.findFileByPath(this, path);
    }

    @Override
    public VirtualFile findFileByPathIfCached(@Nonnull String path) {
        return VfsImplUtil.findFileByPathIfCached(this, path);
    }

    @Override
    @Nullable
    public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
        return VfsImplUtil.refreshAndFindFileByPath(this, path);
    }

    @Nonnull
    private static File convertToIOFile(@Nonnull VirtualFile file) {
        return new File(toIoPath(file));
    }

    @Nonnull
    protected static String toIoPath(@Nonnull VirtualFile file) {
        String path = file.getPath();
        if (path.length() == 2 && Platform.current().os().isWindows() && consulo.util.io.PathUtil.startsWithWindowsDrive(path)) {
            // makes 'C:' resolve to a root directory of the drive C:, not the current directory on that drive
            path += '/';
        }
        return path;
    }

    @Nonnull
    private static File convertToIOFileAndCheck(@Nonnull VirtualFile file) throws FileNotFoundException {
        File ioFile = convertToIOFile(file);

        if (Platform.current().os().isUnix() && file.is(VFileProperty.SPECIAL)) { // avoid opening fifo files
            throw new FileNotFoundException("Not a file: " + ioFile + " (type=" + FileSystemUtil.getAttributes(ioFile) + ')');
        }

        return ioFile;
    }

    @Override
    public boolean exists(@Nonnull VirtualFile file) {
        return getAttributes(file) != null;
    }

    @Override
    public long getLength(@Nonnull VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null ? attributes.length : DEFAULT_LENGTH;
    }

    @Override
    public long getTimeStamp(@Nonnull VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null ? attributes.lastModified : DEFAULT_TIMESTAMP;
    }

    @Override
    public boolean isDirectory(@Nonnull VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isDirectory();
    }

    @Override
    public boolean isWritable(@Nonnull VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isWritable();
    }

    @Override
    public boolean isSymLink(@Nonnull VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isSymLink();
    }

    @Override
    public String resolveSymLink(@Nonnull VirtualFile file) {
        return FileSystemUtil.resolveSymLink(file.getPath());
    }

    @Nonnull
    @Override
    public String[] list(@Nonnull VirtualFile file) {
        Path path = getNioPath(file);
        String[] names = path == null ? null : myNioChildrenGetter.accessDiskWithCheckCanceled(path);
        return names == null ? ArrayUtil.EMPTY_STRING_ARRAY : names;
    }

    @Override
    @Nonnull
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    @Nullable
    public String normalize(@Nonnull String path) {
        if (path.isEmpty()) {
            try {
                path = new File("").getCanonicalPath();
            }
            catch (IOException e) {
                return path;
            }
        }
        else if (Platform.current().os().isWindows()) {
            if (path.charAt(0) == '/' && !path.startsWith("//")) {
                path = path.substring(1);  // hack over new File(path).toURI().toURL().getFile()
            }

            try {
                path = consulo.ide.impl.idea.openapi.util.io.FileUtil.resolveShortWindowsName(path);
            }
            catch (IOException e) {
                return null;
            }
        }

        File file = new File(path);
        if (!isAbsoluteFileOrDriveLetter(file)) {
            path = file.getAbsolutePath();
        }

        return consulo.ide.impl.idea.openapi.util.io.FileUtil.normalize(path);
    }

    private static boolean isAbsoluteFileOrDriveLetter(@Nonnull File file) {
        String path = file.getPath();
        if (Platform.current().os().isWindows() && path.length() == 2 && path.charAt(1) == ':') {
            // just drive letter.
            // return true, despite the fact that technically it's not an absolute path
            return true;
        }
        return file.isAbsolute();
    }

    @Override
    public void refreshIoFiles(@Nonnull Iterable<? extends File> files) {
        refreshIoFiles(files, false, false, null);
    }

    @Override
    public void refreshIoFiles(@Nonnull Iterable<? extends File> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {
        VirtualFileManagerEx manager = (VirtualFileManagerEx) VirtualFileManager.getInstance();

        Application app = Application.get();
        boolean fireCommonRefreshSession = app.isDispatchThread() || app.isWriteAccessAllowed();
        if (fireCommonRefreshSession) {
            manager.fireBeforeRefreshStart(false);
        }

        try {
            List<VirtualFile> filesToRefresh = new ArrayList<>();

            for (File file : files) {
                VirtualFile virtualFile = refreshAndFindFileByIoFile(file);
                if (virtualFile != null) {
                    filesToRefresh.add(virtualFile);
                }
            }

            RefreshQueue.getInstance().refresh(async, recursive, onFinish, filesToRefresh);
        }
        finally {
            if (fireCommonRefreshSession) {
                manager.fireAfterRefreshFinish(false);
            }
        }
    }

    @Override
    public void refreshFiles(@Nonnull Iterable<? extends VirtualFile> files) {
        refreshFiles(files, false, false, null);
    }

    @Override
    public void refreshFiles(@Nonnull Iterable<? extends VirtualFile> files, boolean async, boolean recursive, @Nullable Runnable onFinish) {
        RefreshQueue.getInstance().refresh(async, recursive, onFinish, consulo.ide.impl.idea.util.containers.ContainerUtil.toCollection(files));
    }

    @Override
    public void registerAuxiliaryFileOperationsHandler(@Nonnull LocalFileOperationsHandler handler) {
        if (myHandlers.contains(handler)) {
            LOG.error("Handler " + handler + " already registered.");
        }
        myHandlers.add(handler);
    }

    @Override
    public void unregisterAuxiliaryFileOperationsHandler(@Nonnull LocalFileOperationsHandler handler) {
        if (!myHandlers.remove(handler)) {
            LOG.error("Handler " + handler + " haven't been registered or already unregistered.");
        }
    }

    private boolean auxDelete(@Nonnull VirtualFile file) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.delete(file)) {
                return true;
            }
        }

        return false;
    }

    private boolean auxMove(@Nonnull VirtualFile file, @Nonnull VirtualFile toDir) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.move(file, toDir)) {
                return true;
            }
        }
        return false;
    }

    private boolean auxCopy(@Nonnull VirtualFile file, @Nonnull VirtualFile toDir, @Nonnull String copyName) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            File copy = handler.copy(file, toDir, copyName);
            if (copy != null) {
                return true;
            }
        }
        return false;
    }

    private boolean auxRename(@Nonnull VirtualFile file, @Nonnull String newName) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.rename(file, newName)) {
                return true;
            }
        }
        return false;
    }

    private boolean auxCreateFile(@Nonnull VirtualFile dir, @Nonnull String name) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.createFile(dir, name)) {
                return true;
            }
        }
        return false;
    }

    private boolean auxCreateDirectory(@Nonnull VirtualFile dir, @Nonnull String name) throws IOException {
        for (LocalFileOperationsHandler handler : myHandlers) {
            if (handler.createDirectory(dir, name)) {
                return true;
            }
        }
        return false;
    }

    private void auxNotifyCompleted(@Nonnull ThrowableConsumer<LocalFileOperationsHandler, IOException> consumer) {
        for (LocalFileOperationsHandler handler : myHandlers) {
            handler.afterDone(consumer);
        }
    }

    @Override
    @Nonnull
    public VirtualFile createChildDirectory(Object requestor, @Nonnull VirtualFile parent, @Nonnull String dir) throws IOException {
        if (!isValidName(dir)) {
            throw new IOException(VirtualFileSystemLocalize.directoryInvalidNameError(dir).get());
        }

        if (!parent.exists() || !parent.isDirectory()) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetNotDirectoryError(parent.getPath()).get());
        }
        if (parent.findChild(dir) != null) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetAlreadyExistsError(parent.getPath() + "/" + dir).get());
        }

        File ioParent = convertToIOFile(parent);
        if (!ioParent.isDirectory()) {
            throw new IOException(VirtualFileSystemLocalize.targetNotDirectoryError(ioParent.getPath()).get());
        }

        if (!auxCreateDirectory(parent, dir)) {
            File ioDir = new File(ioParent, dir);
            if (!(ioDir.mkdirs() || ioDir.isDirectory())) {
                throw new IOException(VirtualFileSystemLocalize.newDirectoryFailedError(ioDir.getPath()).get());
            }
        }

        auxNotifyCompleted(handler -> handler.createDirectory(parent, dir));

        return new FakeVirtualFile(parent, dir);
    }

    @Nonnull
    @Override
    public VirtualFile createChildFile(Object requestor, @Nonnull VirtualFile parent, @Nonnull String file) throws IOException {
        if (!isValidName(file)) {
            throw new IOException(VirtualFileSystemLocalize.fileInvalidNameError(file).get());
        }

        if (!parent.exists() || !parent.isDirectory()) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetNotDirectoryError(parent.getPath()).get());
        }
        if (parent.findChild(file) != null) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetAlreadyExistsError(parent.getPath() + "/" + file).get());
        }

        File ioParent = convertToIOFile(parent);
        if (!ioParent.isDirectory()) {
            throw new IOException(VirtualFileSystemLocalize.targetNotDirectoryError(ioParent.getPath()).get());
        }

        if (!auxCreateFile(parent, file)) {
            File ioFile = new File(ioParent, file);
            if (!FileUtil.createIfDoesntExist(ioFile)) {
                throw new IOException(VirtualFileSystemLocalize.newFileFailedError(ioFile.getPath()).get());
            }
        }

        auxNotifyCompleted(handler -> handler.createFile(parent, file));

        return new FakeVirtualFile(parent, file);
    }

    @Override
    public void deleteFile(Object requestor, @Nonnull VirtualFile file) throws IOException {
        if (file.getParent() == null) {
            throw new IOException(VirtualFileSystemLocalize.cannotDeleteRootDirectory(file.getPath()).get());
        }

        if (!auxDelete(file)) {
            File ioFile = convertToIOFile(file);
            if (!FileUtil.delete(ioFile)) {
                throw new IOException(VirtualFileSystemLocalize.deleteFailedError(ioFile.getPath()).get());
            }
        }

        auxNotifyCompleted(handler -> handler.delete(file));
    }

    @Override
    public boolean isCaseSensitive() {
        return Platform.current().fs().isCaseSensitive();
    }

    @Override
    public boolean isValidName(@Nonnull String name) {
        return PathUtil.isValidFileName(name, false);
    }

    @Override
    @Nonnull
    public InputStream getInputStream(@Nonnull VirtualFile file) throws IOException {
        return new BufferedInputStream(new FileInputStream(convertToIOFileAndCheck(file)));
    }

    @Override
    @Nonnull
    public byte[] contentsToByteArray(@Nonnull VirtualFile file) throws IOException {
        try (InputStream stream = new FileInputStream(convertToIOFileAndCheck(file))) {
            long l = file.getLength();
            if (RawFileLoader.getInstance().isLargeForContentLoading(l)) {
                throw new FileTooBigException(file.getPath());
            }
            int length = (int) l;
            if (length < 0) {
                throw new IOException("Invalid file length: " + length + ", " + file);
            }
            // io_util.c#readBytes allocates custom native stack buffer for io operation with malloc if io request > 8K
            // so let's do buffered requests with buffer size 8192 that will use stack allocated buffer
            return loadBytes(length <= 8192 ? stream : new BufferedInputStream(stream), length);
        }
    }

    @Nonnull
    private static byte[] loadBytes(@Nonnull InputStream stream, int length) throws IOException {
        byte[] bytes = new byte[length];
        int count = 0;
        while (count < length) {
            int n = stream.read(bytes, count, length - count);
            if (n <= 0) {
                break;
            }
            count += n;
        }
        if (count < length) {
            // this may happen with encrypted files, see IDEA-143773
            return Arrays.copyOf(bytes, count);
        }
        return bytes;
    }

    @Override
    @Nonnull
    public OutputStream getOutputStream(@Nonnull VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
        File ioFile = convertToIOFileAndCheck(file);
        OutputStream stream;

        if (SafeWriteRequestor.shouldUseSafeWrite(requestor)) {
            SafeOutputStreamFactory factory = Application.get().getInstance(SafeOutputStreamFactory.class);
            stream = factory.create(ioFile);
        }
        else {
            stream = new FileOutputStream(ioFile);
        }

        return new BufferedOutputStream(stream) {
            @Override
            public void close() throws IOException {
                super.close();
                if (timeStamp > 0 && ioFile.exists()) {
                    if (!ioFile.setLastModified(timeStamp)) {
                        LOG.warn("Failed: " + ioFile.getPath() + ", new:" + timeStamp + ", old:" + ioFile.lastModified());
                    }
                }
            }
        };
    }

    @Override
    public void moveFile(Object requestor, @Nonnull VirtualFile file, @Nonnull VirtualFile newParent) throws IOException {
        String name = file.getName();

        if (!file.exists()) {
            throw new IOException(VirtualFileSystemLocalize.vfsFileNotExistError(file.getPath()).get());
        }
        if (file.getParent() == null) {
            throw new IOException(VirtualFileSystemLocalize.cannotRenameRootDirectory(file.getPath()).get());
        }
        if (!newParent.exists() || !newParent.isDirectory()) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetNotDirectoryError(newParent.getPath()).get());
        }
        if (newParent.findChild(name) != null) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetAlreadyExistsError(newParent.getPath() + "/" + name).get());
        }

        File ioFile = convertToIOFile(file);
        if (FileSystemUtil.getAttributes(ioFile) == null) {
            throw new FileNotFoundException(VirtualFileSystemLocalize.fileNotExistError(ioFile.getPath()).get());
        }
        File ioParent = convertToIOFile(newParent);
        if (!ioParent.isDirectory()) {
            throw new IOException(VirtualFileSystemLocalize.targetNotDirectoryError(ioParent.getPath()).get());
        }
        File ioTarget = new File(ioParent, name);
        if (ioTarget.exists()) {
            throw new IOException(VirtualFileSystemLocalize.targetAlreadyExistsError(ioTarget.getPath()).get());
        }

        if (!auxMove(file, newParent)) {
            if (!ioFile.renameTo(ioTarget)) {
                throw new IOException(VirtualFileSystemLocalize.moveFailedError(ioFile.getPath(), ioParent.getPath()).get());
            }
        }

        auxNotifyCompleted(handler -> handler.move(file, newParent));
    }

    @Override
    public void renameFile(Object requestor, @Nonnull VirtualFile file, @Nonnull String newName) throws IOException {
        if (!isValidName(newName)) {
            throw new IOException(VirtualFileSystemLocalize.fileInvalidNameError(newName).get());
        }

        boolean sameName = !isCaseSensitive() && newName.equalsIgnoreCase(file.getName());

        if (!file.exists()) {
            throw new IOException(VirtualFileSystemLocalize.vfsFileNotExistError(file.getPath()).get());
        }
        VirtualFile parent = file.getParent();
        if (parent == null) {
            throw new IOException(VirtualFileSystemLocalize.cannotRenameRootDirectory(file.getPath()).get());
        }
        if (!sameName && parent.findChild(newName) != null) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetAlreadyExistsError(parent.getPath() + "/" + newName).get());
        }

        File ioFile = convertToIOFile(file);
        if (!ioFile.exists()) {
            throw new FileNotFoundException(VirtualFileSystemLocalize.fileNotExistError(ioFile.getPath()).get());
        }
        File ioTarget = new File(convertToIOFile(parent), newName);
        if (!sameName && ioTarget.exists()) {
            throw new IOException(VirtualFileSystemLocalize.targetAlreadyExistsError(ioTarget.getPath()).get());
        }

        if (!auxRename(file, newName)) {
            if (!consulo.ide.impl.idea.openapi.util.io.FileUtil.rename(ioFile, newName)) {
                throw new IOException(VirtualFileSystemLocalize.renameFailedError(ioFile.getPath(), newName).get());
            }
        }

        auxNotifyCompleted(handler -> handler.rename(file, newName));
    }

    @Nonnull
    @Override
    public VirtualFile copyFile(
        Object requestor,
        @Nonnull VirtualFile file,
        @Nonnull VirtualFile newParent,
        @Nonnull String copyName
    ) throws IOException {
        if (!isValidName(copyName)) {
            throw new IOException(VirtualFileSystemLocalize.fileInvalidNameError(copyName).get());
        }

        if (!file.exists()) {
            throw new IOException(VirtualFileSystemLocalize.vfsFileNotExistError(file.getPath()).get());
        }
        if (!newParent.exists() || !newParent.isDirectory()) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetNotDirectoryError(newParent.getPath()).get());
        }
        if (newParent.findChild(copyName) != null) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetAlreadyExistsError(newParent.getPath() + "/" + copyName).get());
        }

        FileAttributes attributes = getAttributes(file);
        if (attributes == null) {
            throw new FileNotFoundException(VirtualFileSystemLocalize.fileNotExistError(file.getPath()).get());
        }
        if (attributes.isSpecial()) {
            throw new FileNotFoundException("Not a file: " + file);
        }
        File ioParent = convertToIOFile(newParent);
        if (!ioParent.isDirectory()) {
            throw new IOException(VirtualFileSystemLocalize.targetNotDirectoryError(ioParent.getPath()).get());
        }
        File ioTarget = new File(ioParent, copyName);
        if (ioTarget.exists()) {
            throw new IOException(VirtualFileSystemLocalize.targetAlreadyExistsError(ioTarget.getPath()).get());
        }

        if (!auxCopy(file, newParent, copyName)) {
            try {
                File ioFile = convertToIOFile(file);
                consulo.ide.impl.idea.openapi.util.io.FileUtil.copyFileOrDir(ioFile, ioTarget, attributes.isDirectory());
            }
            catch (IOException e) {
                FileUtil.delete(ioTarget);
                throw e;
            }
        }

        auxNotifyCompleted(handler -> handler.copy(file, newParent, copyName));

        return new FakeVirtualFile(newParent, copyName);
    }

    @Override
    public void setTimeStamp(@Nonnull VirtualFile file, long timeStamp) {
        File ioFile = convertToIOFile(file);
        if (ioFile.exists() && !ioFile.setLastModified(timeStamp)) {
            LOG.warn("Failed: " + file.getPath() + ", new:" + timeStamp + ", old:" + ioFile.lastModified());
        }
    }

    @Override
    public void setWritable(@Nonnull VirtualFile file, boolean writableFlag) throws IOException {
        String path = FileUtil.toSystemDependentName(file.getPath());
        consulo.ide.impl.idea.openapi.util.io.FileUtil.setReadOnlyAttribute(path, !writableFlag);
        if (consulo.ide.impl.idea.openapi.util.io.FileUtil.canWrite(path) != writableFlag) {
            throw new IOException("Failed to change read-only flag for " + path);
        }
    }

    private static final String[] ourRootPaths;

    static {
        //noinspection SpellCheckingInspection
        List<String> roots = StringUtil.split(System.getProperty("idea.persistentfs.roots", ""), File.pathSeparator);
        Collections.sort(roots, (o1, o2) -> o2.length() - o1.length());  // longest first
        ourRootPaths = ArrayUtil.toStringArray(roots);
    }

    @Nonnull
    @Override
    public String extractRootPath(@Nonnull String path) {
        if (path.isEmpty()) {
            try {
                path = new File("").getCanonicalPath();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (String customRootPath : ourRootPaths) {
            if (path.startsWith(customRootPath)) {
                return customRootPath;
            }
        }

        if (Platform.current().os().isWindows()) {
            if (path.length() >= 2 && path.charAt(1) == ':') {
                // Drive letter
                return StringUtil.toUpperCase(path.substring(0, 2));
            }

            if (path.startsWith("//") || path.startsWith("\\\\")) {
                // UNC. Must skip exactly two path elements like [\\ServerName\ShareName]\pathOnShare\file.txt
                // Root path is in square brackets here.

                int slashCount = 0;
                int idx;
                boolean isSlash = false;
                for (idx = 2; idx < path.length() && slashCount < 2; idx++) {
                    char c = path.charAt(idx);
                    isSlash = c == '\\' || c == '/';
                    if (isSlash) {
                        slashCount++;
                        if (slashCount == 2) {
                            idx--;
                        }
                    }
                }

                if (slashCount == 2 || slashCount == 1 && !isSlash) {
                    return path.substring(0, idx);
                }
            }

            return "";
        }

        return StringUtil.startsWithChar(path, '/') ? "/" : "";
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public boolean markNewFilesAsDirty() {
        return true;
    }

    @Nonnull
    @Override
    public String getCanonicallyCasedName(@Nonnull VirtualFile file) {
        if (isCaseSensitive()) {
            return super.getCanonicallyCasedName(file);
        }

        String originalFileName = file.getName();
        long t = LOG.isTraceEnabled() ? System.nanoTime() : 0;
        try {
            File ioFile = convertToIOFile(file);

            File canonicalFile = ioFile.getCanonicalFile();
            String canonicalFileName = canonicalFile.getName();
            if (!Platform.current().os().isUnix()) {
                return canonicalFileName;
            }

            // linux & mac support symbolic links
            // unfortunately canonical file resolves sym links
            // so its name may differ from name of origin file
            //
            // Here FS is case sensitive, so let's check that original and
            // canonical file names are equal if we ignore name case
            if (canonicalFileName.compareToIgnoreCase(originalFileName) == 0) {
                // p.s. this should cover most cases related to not symbolic links
                return canonicalFileName;
            }

            // Ok, names are not equal. Let's try to find corresponding file name
            // among original file parent directory
            File parentFile = ioFile.getParentFile();
            if (parentFile != null) {
                // I hope ls works fast on Unix
                String[] canonicalFileNames = parentFile.list();
                if (canonicalFileNames != null) {
                    for (String name : canonicalFileNames) {
                        // if names are equals
                        if (name.compareToIgnoreCase(originalFileName) == 0) {
                            return name;
                        }
                    }
                }
            }
            // No luck. So ein mist!
            // Ok, garbage in, garbage out. We may return original or canonical name
            // no difference. Let's return canonical name just to preserve previous
            // behaviour of this code.
            return canonicalFileName;
        }
        catch (IOException | InvalidPathException e) {
            return originalFileName;
        }
        finally {
            if (t != 0) {
                t = (System.nanoTime() - t) / 1000;
                LOG.trace("getCanonicallyCasedName(" + file + "): " + t + " mks");
            }
        }
    }

    @Override
    public FileAttributes getAttributes(@Nonnull VirtualFile file) {
        return Platform.current().os().isWindows() && file.getParent() == null && file.getPath().startsWith("//")
            ? UNC_ROOT_ATTRIBUTES
            : myAttrGetter.accessDiskWithCheckCanceled(file);
    }

    @Override
    public void refresh(boolean asynchronous) {
        RefreshQueue.getInstance().refresh(asynchronous, true, null, ManagingFS.getInstance().getRoots(this));
    }

    @Override
    public boolean hasChildren(@Nonnull VirtualFile file) {
        if (file.getParent() == null) {
            return true;  // assume roots always have children
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(file.getPath()))) {
            return stream.iterator().hasNext();  // make sure to not load all children
        }
        catch (DirectoryIteratorException e) {
            return false;  // a directory can't be iterated over
        }
        catch (InvalidPathException | IOException | SecurityException e) {
            return true;
        }
    }

    @Override
    @Nullable
    public Path getNioPath(@Nonnull VirtualFile file) {
        return file.getFileSystem() == this ? Paths.get(toIoPath(file)) : null;
    }

    @Nullable
    private static FileAttributes getAttributesWithCustomTimestamp(VirtualFile file) {
        var pathStr = FileUtil.toSystemDependentName(file.getPath());
        if (pathStr.length() == 2 && pathStr.charAt(1) == ':') {
            pathStr += '\\';
        }
        var attributes = FileSystemUtil.getAttributes(pathStr);
        return copyWithCustomTimestamp(file, attributes);
    }

    @Nullable
    private static FileAttributes copyWithCustomTimestamp(VirtualFile file, @Nullable FileAttributes attributes) {
        return attributes;
    }

    private static String[] listPathChildren(@Nonnull Path dir) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            List<String> result = new ArrayList<>();
            for (Path path : dirStream) {
                result.add(path.getFileName().toString());
            }
            return result.toArray(String[]::new);
        }
        catch (IOException e) {
            LOG.warn("Unable to list children for path: " + dir, e);
            return null;
        }
    }
}