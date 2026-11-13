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

import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.SimpleModificationTracker;
import consulo.logging.Logger;
import consulo.platform.PlatformNioFileSystem;
import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.RefreshableFileSystem;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.impl.internal.BatchingFileSystem;
import consulo.virtualFileSystem.impl.internal.FileSystemUnavailableException;
import consulo.virtualFileSystem.impl.internal.UnavailabilityAwareFileSystem;
import consulo.virtualFileSystem.impl.internal.watcher.WatchedFileSystem;
import consulo.virtualFileSystem.internal.FakeVirtualFile;
import consulo.virtualFileSystem.internal.PlatformPathVirtualFileSystem;
import consulo.virtualFileSystem.internal.VfsImplUtil;
import consulo.virtualFileSystem.localize.VirtualFileSystemLocalize;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import org.jspecify.annotations.Nullable;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Files of other machines, read through the {@link PlatformNioFileSystem} of each - persistent (the files have ids, are
 * indexed, can be edited), like the local file system.
 * <p>
 * A path is {@code <platform id><path on the platform>} - a platform id has no slash - so a stored url names its machine
 * and resolves again once that machine is reachable. The root of a platform is {@code <platform id>}. While a platform
 * is not reachable its files are unavailable (not deleted), see {@link UnavailabilityAwareFileSystem}.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ExtensionImpl
public class PlatformVirtualFileSystem extends NewVirtualFileSystem
    implements PlatformPathVirtualFileSystem, RefreshableFileSystem, BatchingFileSystem, UnavailabilityAwareFileSystem, WatchedFileSystem {
    private static final Logger LOG = Logger.getInstance(PlatformVirtualFileSystem.class);

    private static final char SEPARATOR = '/';

    private final Map<String, FileSystem> myFileSystems = new ConcurrentHashMap<>();

    @Override
    public String getProtocol() {
        return StandardFileSystems.PLATFORM_PROTOCOL;
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public boolean isCaseSensitive() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean markNewFilesAsDirty() {
        return true;
    }

    // region paths

    @Override
    public @Nullable VirtualFile findFileByNioPath(Path path, boolean refresh) {
        if (!(path.getFileSystem() instanceof PlatformNioFileSystem)) {
            return null;
        }
        String vfsPath = toVfsPath(path);
        return refresh ? refreshAndFindFileByPath(vfsPath) : findFileByPath(vfsPath);
    }

    @Override
    public String toVfsPath(Path path) {
        String filePath = FileUtil.toSystemIndependentName(path.toString());
        if (!(path.getFileSystem() instanceof PlatformNioFileSystem platformFileSystem)) {
            return filePath;
        }
        if (myFileSystems.put(platformFileSystem.getPlatformId(), path.getFileSystem()) != path.getFileSystem()) {
            // urls of this platform resolve from now on
            if (VirtualFilePointerManager.getInstance() instanceof SimpleModificationTracker tracker) {
                tracker.incModificationCount();
            }
        }
        return platformFileSystem.getPlatformId() + (filePath.startsWith("/") ? filePath : SEPARATOR + filePath);
    }

    @Override
    public @Nullable VirtualFile findFileByPath(String path) {
        return VfsImplUtil.findFileByPath(this, path);
    }

    @Override
    public @Nullable VirtualFile findFileByPathIfCached(String path) {
        return VfsImplUtil.findFileByPathIfCached(this, path);
    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(String path) {
        return VfsImplUtil.refreshAndFindFileByPath(this, path);
    }

    @Override
    public void refresh(boolean asynchronous) {
        RefreshQueue.getInstance().refresh(asynchronous, true, null, ManagingFS.getInstance().getRoots(this));
    }

    /**
     * Nothing watches the files of other machines - a refresh by the user rescans all that is known of them.
     */
    @Override
    public void refreshWithoutFileWatcher(boolean asynchronous) {
        VirtualFile[] roots = ManagingFS.getInstance().getRoots(this);
        for (VirtualFile root : roots) {
            ((NewVirtualFile) root).markDirtyRecursively();
        }
        RefreshQueue.getInstance().refresh(asynchronous, true, null, roots);
    }

    /**
     * The root of a platform is named by its id alone - a root name must not contain a separator - and stands for the
     * {@code /} of that platform.
     * <p>
     * TODO a windows platform: its drives need roots of their own ({@code C:} under the platform id)
     */
    @Override
    public String extractRootPath(String path) {
        int separator = path.indexOf(SEPARATOR);
        return separator < 0 ? path : path.substring(0, separator);
    }

    /**
     * The path as it is on its machine - the platform id is internal, the user knows the machine from the context.
     */
    @Override
    public String extractPresentableUrl(String path) {
        int separator = path.indexOf(SEPARATOR);
        if (separator < 0) {
            return String.valueOf(SEPARATOR);
        }
        String filePath = path.substring(separator);
        if (filePath.length() > 2 && filePath.charAt(2) == ':') {
            // a drive of a windows platform: /C:/... -> C:\...
            String windowsPath = filePath.substring(1).replace(SEPARATOR, '\\');
            return windowsPath.length() == 2 ? windowsPath + '\\' : windowsPath;
        }
        return filePath;
    }

    @Override
    public @Nullable Path getNioPath(VirtualFile file) {
        try {
            return toNioPath(file);
        }
        catch (FileSystemUnavailableException e) {
            return null;
        }
    }

    // region watching

    @Override
    public boolean isWatchedPath(Path path) {
        return path.getFileSystem() instanceof PlatformNioFileSystem;
    }

    /**
     * Nothing refreshes the files of other machines on its own - the changes the watcher reported are refreshed now.
     */
    @Override
    public void dirtyFilesMarked() {
        refresh(true);
    }

    // endregion

    @Override
    public @Nullable Path toNioPath(String vfsPath) {
        if (!isPlatformPath(vfsPath)) {
            return null;
        }
        try {
            return toNioPathOrThrow(vfsPath);
        }
        catch (FileSystemUnavailableException e) {
            return null;
        }
    }

    @Override
    public boolean isPlatformPath(String vfsPath) {
        return !vfsPath.isEmpty() && myFileSystems.containsKey(extractRootPath(vfsPath));
    }

    private Path toNioPath(VirtualFile file) throws FileSystemUnavailableException {
        return toNioPathOrThrow(file.getPath());
    }

    private Path toNioPathOrThrow(String vfsPath) throws FileSystemUnavailableException {
        int separator = vfsPath.indexOf(SEPARATOR);
        String platformId = separator < 0 ? vfsPath : vfsPath.substring(0, separator);
        FileSystem fileSystem = myFileSystems.get(platformId);
        if (fileSystem == null || !fileSystem.isOpen()) {
            throw new FileSystemUnavailableException(new IOException("The platform " + platformId + " is not connected"));
        }
        String filePath = separator < 0 ? "/" : vfsPath.substring(separator);
        if (filePath.length() > 2 && filePath.charAt(2) == ':') {
            // a drive of a windows platform: /C:/... -> C:/...
            filePath = filePath.substring(1);
            if (filePath.length() == 2) {
                // C: alone is the current directory of the drive, its root is C:/
                filePath += SEPARATOR;
            }
        }
        return fileSystem.getPath(filePath);
    }

    // endregion

    // region attributes

    @Override
    public @Nullable FileAttributes getAttributes(VirtualFile file) {
        try {
            return getAttributesOrThrowIfUnavailable(file);
        }
        catch (FileSystemUnavailableException e) {
            return null;
        }
    }

    @Override
    public @Nullable FileAttributes getAttributesOrThrowIfUnavailable(VirtualFile file) throws FileSystemUnavailableException {
        Path path = toNioPath(file);
        if (file.getParent() == null && driveRoots(path.getFileSystem()) != null) {
            // the root of a windows platform has no file of its own - it holds the drives
            return new FileAttributes(true, false, false, false, 0, 0, false);
        }
        try {
            return readAttributes(path);
        }
        catch (NoSuchFileException e) {
            return null;
        }
        catch (IOException e) {
            if (!path.getFileSystem().isOpen()) {
                throw new FileSystemUnavailableException(e);
            }
            LOG.debug(e);
            return null;
        }
    }

    /**
     * Built here, not by {@link FileAttributes#fromNio} - that one looks at the operating system of the IDE, not of the platform.
     */
    private static FileAttributes readAttributes(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        boolean symlink = attributes.isSymbolicLink();
        if (symlink) {
            try {
                attributes = Files.readAttributes(path, BasicFileAttributes.class);
            }
            catch (IOException e) {
                return FileAttributes.BROKEN_SYMLINK;
            }
        }
        boolean directory = attributes.isDirectory();
        boolean writable = directory || Files.isWritable(path);
        return new FileAttributes(directory, attributes.isOther(), symlink, false, attributes.size(), attributes.lastModifiedTime().toMillis(), writable);
    }

    @Override
    public boolean exists(VirtualFile file) {
        return getAttributes(file) != null;
    }

    @Override
    public boolean isDirectory(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isDirectory();
    }

    @Override
    public long getLength(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null ? attributes.length : DEFAULT_LENGTH;
    }

    @Override
    public long getTimeStamp(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null ? attributes.lastModified : DEFAULT_TIMESTAMP;
    }

    @Override
    public boolean isWritable(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isWritable();
    }

    @Override
    public boolean isSymLink(VirtualFile file) {
        FileAttributes attributes = getAttributes(file);
        return attributes != null && attributes.isSymLink();
    }

    @Override
    public @Nullable String resolveSymLink(VirtualFile file) {
        try {
            return toVfsPath(toNioPath(file).toRealPath());
        }
        catch (IOException e) {
            LOG.debug(e);
            return null;
        }
    }

    @Override
    public void setTimeStamp(VirtualFile file, long timeStamp) throws IOException {
        try {
            Files.setLastModifiedTime(toNioPath(file), FileTime.fromMillis(timeStamp));
        }
        catch (UnsupportedOperationException e) {
            // the platform cannot set the time - the next refresh picks up the actual one
            LOG.debug(e);
        }
    }

    @Override
    public void setWritable(VirtualFile file, boolean writableFlag) throws IOException {
        throw new IOException("Changing the writable flag is not supported on " + file.getPresentableUrl());
    }

    // endregion

    // region children

    @Override
    public String[] list(VirtualFile file) {
        try {
            Path dir = toNioPath(file);
            if (file.getParent() == null) {
                List<Path> drives = driveRoots(dir.getFileSystem());
                if (drives != null) {
                    return drives.stream().map(PlatformVirtualFileSystem::driveName).toArray(String[]::new);
                }
            }
            List<String> names = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path child : stream) {
                    names.add(child.getFileName().toString());
                }
            }
            return names.toArray(String[]::new);
        }
        catch (IOException e) {
            LOG.debug(e);
            return new String[0];
        }
    }

    @Override
    public Map<String, FileAttributes> listWithAttributes(VirtualFile dir, @Nullable Set<String> childrenNames) {
        try {
            return listWithAttributesOrThrowIfUnavailable(dir, childrenNames);
        }
        catch (FileSystemUnavailableException e) {
            return Map.of();
        }
    }

    @Override
    public Map<String, FileAttributes> listWithAttributesOrThrowIfUnavailable(VirtualFile dir, @Nullable Set<String> names)
        throws FileSystemUnavailableException {
        Path dirPath = toNioPath(dir);
        // a 'normal' (case-sensitive) map, see the BatchingFileSystem contract
        Map<String, FileAttributes> result = new HashMap<>();
        if (dir.getParent() == null) {
            List<Path> drives = driveRoots(dirPath.getFileSystem());
            if (drives != null) {
                for (Path drive : drives) {
                    String name = driveName(drive);
                    if (names != null && !names.contains(name)) {
                        continue;
                    }
                    try {
                        result.put(name, readAttributes(drive));
                    }
                    catch (IOException e) {
                        LOG.debug(e);
                    }
                }
                return result;
            }
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path child : stream) {
                String name = child.getFileName().toString();
                if (names != null && !names.contains(name)) {
                    continue;
                }
                try {
                    // the listing has fetched the attributes of every child already - this is answered without a round trip
                    result.put(name, readAttributes(child));
                }
                catch (IOException e) {
                    LOG.debug(e);
                }
            }
        }
        catch (NoSuchFileException e) {
            return Map.of();
        }
        catch (IOException e) {
            if (!dirPath.getFileSystem().isOpen()) {
                throw new FileSystemUnavailableException(e);
            }
            LOG.debug(e);
        }
        return result;
    }

    /**
     * The drives of a windows platform, which are the children of its root here, or null for a platform with the single root {@code /}.
     */
    private static @Nullable List<Path> driveRoots(FileSystem fileSystem) {
        List<Path> roots = new ArrayList<>();
        for (Path root : fileSystem.getRootDirectories()) {
            roots.add(root);
        }
        if (roots.size() == 1 && roots.get(0).toString().equals("/")) {
            return null;
        }
        return roots;
    }

    /**
     * {@code C:/} -> {@code C:}
     */
    private static String driveName(Path drive) {
        String name = FileUtil.toSystemIndependentName(drive.toString());
        return name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    }

    // endregion

    // region content

    @Override
    public byte[] contentsToByteArray(VirtualFile file) throws IOException {
        return Files.readAllBytes(toNioPath(file));
    }

    @Override
    public InputStream getInputStream(VirtualFile file) throws IOException {
        return Files.newInputStream(toNioPath(file));
    }

    @Override
    public OutputStream getOutputStream(VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
        Path path = toNioPath(file);
        return new FilterOutputStream(Files.newOutputStream(path)) {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                super.close();
                if (timeStamp > 0) {
                    setTimeStamp(file, timeStamp);
                }
            }
        };
    }

    // endregion

    // region modifications

    @Override
    public VirtualFile createChildDirectory(@Nullable Object requestor, VirtualFile parent, String dir) throws IOException {
        if (!isValidName(dir)) {
            throw new IOException(VirtualFileSystemLocalize.directoryInvalidNameError(dir).get());
        }
        if (parent.findChild(dir) != null) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetAlreadyExistsError(parent.getPath() + "/" + dir).get());
        }
        Files.createDirectories(toNioPath(parent).resolve(dir));
        return new FakeVirtualFile(parent, dir);
    }

    @Override
    public VirtualFile createChildFile(@Nullable Object requestor, VirtualFile parent, String file) throws IOException {
        if (!isValidName(file)) {
            throw new IOException(VirtualFileSystemLocalize.fileInvalidNameError(file).get());
        }
        if (parent.findChild(file) != null) {
            throw new IOException(VirtualFileSystemLocalize.vfsTargetAlreadyExistsError(parent.getPath() + "/" + file).get());
        }
        Files.createFile(toNioPath(parent).resolve(file));
        return new FakeVirtualFile(parent, file);
    }

    @Override
    public void deleteFile(@Nullable Object requestor, VirtualFile file) throws IOException {
        Path path = toNioPath(file);
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.deleteIfExists(path);
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path child, BasicFileAttributes attrs) throws IOException {
                Files.delete(child);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void moveFile(@Nullable Object requestor, VirtualFile file, VirtualFile newParent) throws IOException {
        Path target = toNioPath(newParent).resolve(file.getName());
        if (Files.exists(target)) {
            throw new IOException(VirtualFileSystemLocalize.targetAlreadyExistsError(target.toString()).get());
        }
        Files.move(toNioPath(file), target);
    }

    @Override
    public void renameFile(@Nullable Object requestor, VirtualFile file, String newName) throws IOException {
        if (!isValidName(newName)) {
            throw new IOException(VirtualFileSystemLocalize.fileInvalidNameError(newName).get());
        }
        Path path = toNioPath(file);
        Path target = path.resolveSibling(newName);
        if (Files.exists(target)) {
            throw new IOException(VirtualFileSystemLocalize.targetAlreadyExistsError(target.toString()).get());
        }
        Files.move(path, target);
    }

    @Override
    public VirtualFile copyFile(@Nullable Object requestor, VirtualFile file, VirtualFile newParent, String copyName) throws IOException {
        if (!isValidName(copyName)) {
            throw new IOException(VirtualFileSystemLocalize.fileInvalidNameError(copyName).get());
        }
        Path source = toNioPath(file);
        if (Files.isDirectory(source)) {
            throw new IOException("Not a file: " + file.getPresentableUrl());
        }
        Path target = toNioPath(newParent).resolve(copyName);
        if (Files.exists(target)) {
            throw new IOException(VirtualFileSystemLocalize.targetAlreadyExistsError(target.toString()).get());
        }
        Files.copy(source, target);
        return new FakeVirtualFile(newParent, copyName);
    }

    // endregion

    @Override
    public String toString() {
        return "PlatformVirtualFileSystem";
    }
}
