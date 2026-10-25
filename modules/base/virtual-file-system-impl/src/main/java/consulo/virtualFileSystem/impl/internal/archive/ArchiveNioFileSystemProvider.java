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
package consulo.virtualFileSystem.impl.internal.archive;

import consulo.util.io.FileAttributes;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

/**
 * Serves the {@link ArchiveNioFileSystem}s. It is not installed - file systems are created by
 * {@link ArchiveNioFileSystemManagerImpl} only, so {@link #getScheme()} never clashes with the JDK zip file system.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
final class ArchiveNioFileSystemProvider extends FileSystemProvider {
    @Override
    public String getScheme() {
        return "consulo-archive";
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getPath(URI uri) {
        throw new UnsupportedOperationException();
    }

    private static ArchivePathImpl toArchivePath(Path path) {
        if (!(path instanceof ArchivePathImpl archivePath)) {
            throw new ProviderMismatchException();
        }
        return archivePath;
    }

    private static FileAttributes getAttributes(ArchivePathImpl path) throws NoSuchFileException {
        FileAttributes attributes = path.getFileSystem().getHandler().getAttributes(path.getEntryPath());
        if (attributes == null) {
            throw new NoSuchFileException(path.toString());
        }
        return attributes;
    }

    private static byte[] readBytes(ArchivePathImpl path) throws IOException {
        FileAttributes attributes = getAttributes(path);
        if (attributes.isDirectory()) {
            throw new IOException(path + " is a directory");
        }
        return path.getFileSystem().getHandler().contentsToByteArray(path.getEntryPath());
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        for (OpenOption option : options) {
            if (option != StandardOpenOption.READ && option != LinkOption.NOFOLLOW_LINKS) {
                throw new ReadOnlyFileSystemException();
            }
        }
        return new ReadOnlyByteChannel(readBytes(toArchivePath(path)));
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        for (OpenOption option : options) {
            if (option != StandardOpenOption.READ && option != LinkOption.NOFOLLOW_LINKS) {
                throw new ReadOnlyFileSystemException();
            }
        }
        return new ByteArrayInputStream(readBytes(toArchivePath(path)));
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        ArchivePathImpl directory = toArchivePath(dir);
        if (!getAttributes(directory).isDirectory()) {
            throw new NotDirectoryException(directory.toString());
        }

        String[] names = directory.getFileSystem().getHandler().list(directory.getEntryPath());
        Arrays.sort(names);

        List<Path> children = new ArrayList<>(names.length);
        for (String name : names) {
            Path child = directory.resolve(name);
            if (filter.accept(child)) {
                children.add(child);
            }
        }

        return new DirectoryStream<>() {
            @Override
            public Iterator<Path> iterator() {
                return children.iterator();
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void delete(Path path) {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) {
        return path.toAbsolutePath().normalize().equals(path2.toAbsolutePath().normalize());
    }

    @Override
    public boolean isHidden(Path path) {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        ArchivePathImpl archivePath = toArchivePath(path);
        getAttributes(archivePath);
        for (AccessMode mode : modes) {
            if (mode == AccessMode.WRITE) {
                throw new AccessDeniedException(archivePath.toString());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> @Nullable V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        if (type != BasicFileAttributeView.class) {
            return null;
        }

        ArchivePathImpl archivePath = toArchivePath(path);
        return (V) new BasicFileAttributeView() {
            @Override
            public String name() {
                return "basic";
            }

            @Override
            public BasicFileAttributes readAttributes() throws IOException {
                return new Attributes(archivePath, getAttributes(archivePath));
            }

            @Override
            public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
                throw new ReadOnlyFileSystemException();
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (type != BasicFileAttributes.class) {
            throw new UnsupportedOperationException(type.getName());
        }

        ArchivePathImpl archivePath = toArchivePath(path);
        return (A) new Attributes(archivePath, getAttributes(archivePath));
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        String names = attributes;
        int colon = names.indexOf(':');
        if (colon >= 0) {
            if (!"basic".equals(names.substring(0, colon))) {
                throw new UnsupportedOperationException(attributes);
            }
            names = names.substring(colon + 1);
        }

        BasicFileAttributes basic = readAttributes(path, BasicFileAttributes.class, options);

        Map<String, Object> all = new LinkedHashMap<>();
        all.put("lastModifiedTime", basic.lastModifiedTime());
        all.put("lastAccessTime", basic.lastAccessTime());
        all.put("creationTime", basic.creationTime());
        all.put("size", basic.size());
        all.put("isRegularFile", basic.isRegularFile());
        all.put("isDirectory", basic.isDirectory());
        all.put("isSymbolicLink", basic.isSymbolicLink());
        all.put("isOther", basic.isOther());
        all.put("fileKey", basic.fileKey());

        if ("*".equals(names)) {
            return all;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (String name : names.split(",")) {
            if (!all.containsKey(name)) {
                throw new IllegalArgumentException(name);
            }
            result.put(name, all.get(name));
        }
        return result;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) {
        throw new ReadOnlyFileSystemException();
    }

    private record Attributes(ArchivePathImpl path, FileAttributes attributes) implements BasicFileAttributes {
        @Override
        public FileTime lastModifiedTime() {
            return FileTime.fromMillis(Math.max(attributes.lastModified, 0));
        }

        @Override
        public FileTime lastAccessTime() {
            return lastModifiedTime();
        }

        @Override
        public FileTime creationTime() {
            return lastModifiedTime();
        }

        @Override
        public boolean isRegularFile() {
            return !attributes.isDirectory();
        }

        @Override
        public boolean isDirectory() {
            return attributes.isDirectory();
        }

        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public long size() {
            return attributes.isDirectory() ? 0 : attributes.length;
        }

        @Override
        public Object fileKey() {
            return path.toAbsolutePath().normalize();
        }
    }

    private static final class ReadOnlyByteChannel implements SeekableByteChannel {
        private final byte[] myBytes;
        private int myPosition;
        private boolean myOpen = true;

        private ReadOnlyByteChannel(byte[] bytes) {
            myBytes = bytes;
        }

        @Override
        public int read(ByteBuffer dst) {
            if (myPosition >= myBytes.length) {
                return -1;
            }
            int count = Math.min(dst.remaining(), myBytes.length - myPosition);
            dst.put(myBytes, myPosition, count);
            myPosition += count;
            return count;
        }

        @Override
        public int write(ByteBuffer src) {
            throw new NonWritableChannelException();
        }

        @Override
        public long position() {
            return myPosition;
        }

        @Override
        public SeekableByteChannel position(long newPosition) {
            myPosition = (int) Math.min(Math.max(newPosition, 0), Integer.MAX_VALUE);
            return this;
        }

        @Override
        public long size() {
            return myBytes.length;
        }

        @Override
        public SeekableByteChannel truncate(long size) {
            throw new NonWritableChannelException();
        }

        @Override
        public boolean isOpen() {
            return myOpen;
        }

        @Override
        public void close() {
            myOpen = false;
        }
    }
}
