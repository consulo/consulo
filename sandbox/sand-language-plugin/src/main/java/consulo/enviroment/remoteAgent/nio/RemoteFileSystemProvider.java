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
package consulo.enviroment.remoteAgent.nio;

import consulo.enviroment.remoteAgent.RemoteAgentConnection;
import consulo.enviroment.remoteAgent.RemoteAgentException;
import consulo.enviroment.remoteAgent.protocol.FileInfo;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
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
        throw new UnsupportedOperationException("Use newInputStream/newOutputStream instead");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        RemotePath remotePath = toRemotePath(dir);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        try {
            List<FileInfo> entries = fs.getConnection().execute(client -> client.listDirectory(remotePath.getPathString()));
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
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        RemotePath remotePath = toRemotePath(dir);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        try {
            fs.getConnection().executeVoid(client -> client.createDirectory(remotePath.getPathString(), false));
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(Path path) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        try {
            boolean deleted = fs.getConnection().execute(client -> client.deleteFile(remotePath.getPathString()));
            if (!deleted) {
                throw new IOException("Failed to delete: " + path);
            }
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        RemotePath src = toRemotePath(source);
        RemotePath dst = toRemotePath(target);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) src.getFileSystem();
        try {
            ByteBuffer data = fs.getConnection().execute(client -> client.readFile(src.getPathString()));
            fs.getConnection().executeVoid(client -> client.writeFile(dst.getPathString(), data));
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
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
            List<FileInfo> parent = fs.getConnection().execute(client -> client.listDirectory(remotePath.getParent().toString()));
            for (FileInfo info : parent) {
                if (info.getPath().equals(remotePath.getPathString())) {
                    return info.isHidden();
                }
            }
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
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
        try {
            boolean exists = fs.getConnection().execute(client -> client.fileExists(remotePath.getPathString()));
            if (!exists) {
                throw new NoSuchFileException(remotePath.getPathString());
            }
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
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
            return (A) new RemoteBasicFileAttributes(info);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
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
            ByteBuffer data = fs.getConnection().execute(client -> client.readFile(remotePath.getPathString()));
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            return new ByteArrayInputStream(bytes);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        RemotePath remotePath = toRemotePath(path);
        RemoteNioFileSystem fs = (RemoteNioFileSystem) remotePath.getFileSystem();
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    fs.getConnection().executeVoid(client ->
                        client.writeFile(remotePath.getPathString(), ByteBuffer.wrap(toByteArray())));
                }
                catch (RemoteAgentException e) {
                    throw new IOException(e.getMessage(), e);
                }
            }
        };
    }

    private FileInfo getFileInfo(RemoteNioFileSystem fs, RemotePath path) {
        Path parent = path.getParent();
        if (parent == null) {
            // Root path
            List<FileInfo> roots = fs.getConnection().execute(client -> client.listRoots());
            for (FileInfo root : roots) {
                if (root.getPath().equals(path.getPathString())) {
                    return root;
                }
            }
            return null;
        }
        List<FileInfo> children = fs.getConnection().execute(client -> client.listDirectory(parent.toString()));
        for (FileInfo info : children) {
            if (info.getPath().equals(path.getPathString()) || info.getName().equals(path.getFileName().toString())) {
                return info;
            }
        }
        return null;
    }

    private static RemotePath toRemotePath(Path path) {
        if (path instanceof RemotePath rp) {
            return rp;
        }
        throw new IllegalArgumentException("Expected RemotePath but got " + path.getClass().getName());
    }
}
