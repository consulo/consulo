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
import consulo.enviroment.remoteAgent.protocol.FileInfo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

/**
 * NIO FileSystem implementation for remote agent.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemoteNioFileSystem extends FileSystem {
    private final RemoteAgentConnection myConnection;
    private final RemoteFileSystemProvider myProvider;
    private final String myWorkspacePath;
    private volatile boolean myOpen = true;

    RemoteNioFileSystem(RemoteAgentConnection connection, RemoteFileSystemProvider provider) {
        myConnection = connection;
        myProvider = provider;
        myWorkspacePath = connection.execute(client -> client.getWorkspacePath());
    }

    @Override
    public FileSystemProvider provider() {
        return myProvider;
    }

    @Override
    public void close() throws IOException {
        myOpen = false;
    }

    @Override
    public boolean isOpen() {
        return myOpen;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        List<FileInfo> roots = myConnection.execute(client -> client.listRoots());
        List<Path> result = new ArrayList<>();
        for (FileInfo root : roots) {
            result.add(new RemotePath(this, root.getPath()));
        }
        return result;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return List.of(new RemoteFileStore(this));
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Set.of("basic");
    }

    @Override
    public Path getPath(String first, String... more) {
        if (more.length == 0) {
            return new RemotePath(this, first);
        }
        StringBuilder sb = new StringBuilder(first);
        for (String part : more) {
            if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != '/') {
                sb.append('/');
            }
            sb.append(part);
        }
        return new RemotePath(this, sb.toString());
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException("PathMatcher not supported on remote file systems");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("UserPrincipalLookupService not supported on remote file systems");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("File watching not supported on remote file systems");
    }

    public RemoteAgentConnection getConnection() {
        return myConnection;
    }

    String getPlatformId() {
        return myConnection.getPlatformId();
    }

    String getWorkspacePath() {
        return myWorkspacePath;
    }
}
