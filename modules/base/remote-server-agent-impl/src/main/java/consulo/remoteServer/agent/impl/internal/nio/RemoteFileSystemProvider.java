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
package consulo.remoteServer.agent.impl.internal.nio;

import consulo.remoteServer.agent.impl.internal.RemoteAgentConnection;
import consulo.remoteServer.agent.impl.internal.RemoteAgentException;
import consulo.remoteServer.agent.impl.internal.RemotePermissionException;
import consulo.remoteServer.agent.impl.internal.protocol.FileInfo;

import java.io.*;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

/**
 * NIO FileSystemProvider for remote agent connections.
 * <p>
 * Not registered via SPI — each {@link RemoteNioFileSystem} holds a reference to its provider.
 * {@code Files.*} operations work because {@code RemotePath.getFileSystem().provider()} returns this.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemoteFileSystemProvider extends FileSystemProvider {
    @Override
    public String getScheme() {
        return "remote-agent";
    }

    public RemoteNioFileSystem newFileSystem(RemoteAgentConnection connection) {
        return new RemoteNioFileSystem(connection, this);
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        RemoteAgentConnection connection = (RemoteAgentConnection) env.get("connection");
        if (connection == null) {
            throw new IllegalArgumentException("'connection' must be provided in env map");
        }
        return newFileSystem(connection);
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        throw new FileSystemNotFoundException("Use RemotePath.getFileSystem() instead");
    }

    @Override
    public Path getPath(URI uri) {
        throw new UnsupportedOperationException("Use RemoteNioFileSystem.getPath() instead");
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        String pathString = remotePath.getPathString();

        boolean writable = options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND);
        boolean append = options.contains(StandardOpenOption.APPEND);
        boolean truncate = writable && options.contains(StandardOpenOption.TRUNCATE_EXISTING);
        boolean create = options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW);
        try {
            boolean exists = fs.getConnection().fileExists(pathString);
            if (exists && options.contains(StandardOpenOption.CREATE_NEW)) {
                throw new FileAlreadyExistsException(pathString);
            }
            if (!exists && !(writable && create)) {
                throw new NoSuchFileException(pathString);
            }

            byte[] content = exists && !truncate ? readContent(fs, pathString) : new byte[0];
            RemoteByteChannel.Flusher flusher = bytes -> writeContent(fs, pathString, bytes);
            RemoteByteChannel channel = new RemoteByteChannel(content, writable, append, flusher);
            if (!exists) {
                writeContent(fs, pathString, new byte[0]);
            }
            return channel;
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        RemotePath remotePath = toRemotePath(dir);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        try {
            List<FileInfo> entries = fs.getConnection().listDirectory(remotePath.getPathString());
            List<Path> children = new ArrayList<>();
            for (FileInfo entry : entries) {
                Path child = new RemotePath(fs, entry.getPath());
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
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        RemotePath remotePath = toRemotePath(dir);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        String pathString = remotePath.getPathString();
        try {
            if (fs.getConnection().fileExists(pathString)) {
                throw new FileAlreadyExistsException(pathString);
            }
            fs.getConnection().createDirectory(pathString, false);
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void delete(Path path) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        try {
            boolean deleted = fs.getConnection().deleteFile(remotePath.getPathString());
            if (!deleted) {
                throw new NoSuchFileException(remotePath.getPathString());
            }
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        RemotePath src = toRemotePath(source);
        RemotePath dst = toRemotePath(target);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) src.getFileSystem();
        try {
            byte[] data = fs.getConnection().readFile(src.getPathString());
            fs.getConnection().writeFile(dst.getPathString(), data);
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        copy(source, target, options);
        delete(source);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return toRemotePath(path).getPathString().equals(toRemotePath(path2).getPathString());
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        try {
            List<FileInfo> parent = fs.getConnection().listDirectory(remotePath.getParent().toString());
            for (FileInfo info : parent) {
                if (info.getPath().equals(remotePath.getPathString())) {
                    return info.getHidden();
                }
            }
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        return new RemoteFileStore((RemoteNioFileSystem) remotePath.getFileSystem());
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        String pathString = remotePath.getPathString();
        try {
            if (modes.length == 0) {
                if (!fs.getConnection().fileExists(pathString)) {
                    throw new NoSuchFileException(pathString);
                }
                return;
            }

            FileInfo info = getFileInfo(fs, remotePath);
            if (info == null) {
                throw new NoSuchFileException(pathString);
            }
            for (AccessMode mode : modes) {
                boolean allowed = switch (mode) {
                    case READ -> info.getReadable();
                    case WRITE -> info.getWritable();
                    case EXECUTE -> info.getExecutable();
                };
                if (!allowed) {
                    throw new AccessDeniedException(pathString, null, mode.name());
                }
            }
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (type != BasicFileAttributes.class) {
            throw new UnsupportedOperationException("Only BasicFileAttributes supported");
        }
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        try {
            FileInfo info = getFileInfo(fs, remotePath);
            if (info == null) {
                throw new NoSuchFileException(remotePath.getPathString());
            }
            boolean followLinks = !Arrays.asList(options).contains(LinkOption.NOFOLLOW_LINKS);
            return (A) new RemoteBasicFileAttributes(info, followLinks);
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        BasicFileAttributes attrs = readAttributes(path, BasicFileAttributes.class, options);
        Map<String, Object> result = new HashMap<>();
        result.put("size", attrs.size());
        result.put("isDirectory", attrs.isDirectory());
        result.put("isRegularFile", attrs.isRegularFile());
        result.put("isSymbolicLink", attrs.isSymbolicLink());
        result.put("lastModifiedTime", attrs.lastModifiedTime());
        result.put("creationTime", attrs.creationTime());
        return result;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("setAttribute not supported on remote file systems");
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        try {
            return new ByteArrayInputStream(readContent(fs, remotePath.getPathString()));
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        String pathString = remotePath.getPathString();
        List<OpenOption> optionList = Arrays.asList(options);
        boolean append = optionList.contains(StandardOpenOption.APPEND);

        ByteArrayOutputStream stream = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    writeContent(fs, pathString, toByteArray());
                }
                catch (RemoteAgentException e) {
                    throw toIOException(e);
                }
            }
        };
        try {
            if (append || optionList.contains(StandardOpenOption.CREATE_NEW)) {
                boolean exists = fs.getConnection().fileExists(pathString);
                if (exists && optionList.contains(StandardOpenOption.CREATE_NEW)) {
                    throw new FileAlreadyExistsException(pathString);
                }
                if (exists && append) {
                    stream.write(readContent(fs, pathString));
                }
            }
        }
        catch (RemoteAgentException e) {
            throw toIOException(e);
        }
        return stream;
    }

    private static byte[] readContent(RemoteNioFileSystem fs, String pathString) {
        return fs.getConnection().readFile(pathString);
    }

    private static void writeContent(RemoteNioFileSystem fs, String pathString, byte[] bytes) {
        fs.getConnection().writeFile(pathString, bytes);
    }

    private FileInfo getFileInfo(RemoteNioFileSystem fs, RemotePath path) {
        Path parent = path.getParent();
        if (parent == null) {
            // Root path
            List<FileInfo> roots = fs.getConnection().listRoots();
            for (FileInfo root : roots) {
                if (root.getPath().equals(path.getPathString())) {
                    return root;
                }
            }
            return null;
        }
        List<FileInfo> children = fs.getConnection().listDirectory(parent.toString());
        for (FileInfo info : children) {
            if (info.getPath().equals(path.getPathString()) || info.getName().equals(path.getFileName().toString())) {
                return info;
            }
        }
        return null;
    }

    private static IOException toIOException(RemoteAgentException e) {
        String message = e.getMessage();
        IOException exception = switch (e.getCode()) {
            case NOT_FOUND -> new NoSuchFileException(message);
            case ALREADY_EXISTS -> new FileAlreadyExistsException(message);
            case PERMISSION_DENIED -> e instanceof RemotePermissionException permission && !"os".equals(permission.getPermission())
                ? new IOException(message)
                : new AccessDeniedException(message);
            default -> new IOException(message);
        };
        exception.initCause(e);
        return exception;
    }

    private static RemotePath toRemotePath(Path path) {
        if (path instanceof RemotePath rp) {
            return rp;
        }
        throw new IllegalArgumentException("Expected RemotePath but got " + path.getClass().getName());
    }
}
