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
import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

/**
 * Consulo VFS implementation backed by the remote agent.
 * Protocol: "remote-file" (mirrors "file" for local FS).
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemoteVirtualFileSystem extends NewVirtualFileSystem {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteVirtualFileSystem.class);

    public static final String PROTOCOL = "remote-file";

    private final RemoteAgentConnection myConnection;
    private final RemoteNioFileSystem myNioFileSystem;
    private final RemoteFileMetadataCache myCache = new RemoteFileMetadataCache();

    public RemoteVirtualFileSystem(RemoteAgentConnection connection, RemoteNioFileSystem nioFileSystem) {
        myConnection = connection;
        myNioFileSystem = nioFileSystem;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isCaseSensitive() {
        // Determined by the remote OS
        try {
            var sysInfo = myConnection.execute(c -> c.getSystemInfo());
            String osName = sysInfo.getOsName().toLowerCase();
            return !osName.startsWith("windows") && !osName.startsWith("mac");
        }
        catch (RemoteAgentException e) {
            return true;
        }
    }

    // FileSystemInterface methods

    @Override
    public boolean exists(VirtualFile file) {
        try {
            String remotePath = toRemotePath(file.getPath());
            return myConnection.execute(c -> c.fileExists(remotePath));
        }
        catch (RemoteAgentException e) {
            LOG.warn("Failed to check existence: {}", file.getPath(), e);
            return false;
        }
    }

    @Override
    public String[] list(VirtualFile file) {
        try {
            String remotePath = toRemotePath(file.getPath());
            List<FileInfo> children = getCachedChildren(remotePath);
            String[] names = new String[children.size()];
            for (int i = 0; i < children.size(); i++) {
                names[i] = children.get(i).getName();
            }
            return names;
        }
        catch (RemoteAgentException e) {
            LOG.warn("Failed to list: {}", file.getPath(), e);
            return new String[0];
        }
    }

    @Override
    public boolean isDirectory(VirtualFile file) {
        String remotePath = toRemotePath(file.getPath());
        FileInfo info = myCache.getFileInfo(remotePath);
        if (info != null) {
            return info.isDirectory();
        }
        try {
            List<FileInfo> children = getCachedChildren(remotePath);
            return !children.isEmpty() || myConnection.execute(c -> c.fileExists(remotePath));
        }
        catch (RemoteAgentException e) {
            return false;
        }
    }

    @Override
    public long getTimeStamp(VirtualFile file) {
        String remotePath = toRemotePath(file.getPath());
        FileInfo info = myCache.getFileInfo(remotePath);
        if (info != null && info.isSetLastModified()) {
            return info.getLastModified();
        }
        return 0;
    }

    @Override
    public void setTimeStamp(VirtualFile file, long timeStamp) throws IOException {
        // Not supported by remote agent
    }

    @Override
    public boolean isWritable(VirtualFile file) {
        String remotePath = toRemotePath(file.getPath());
        FileInfo info = myCache.getFileInfo(remotePath);
        if (info != null) {
            return info.isWritable();
        }
        return true;
    }

    @Override
    public void setWritable(VirtualFile file, boolean writableFlag) throws IOException {
        try {
            String remotePath = toRemotePath(file.getPath());
            int mode = writableFlag ? 0755 : 0555;
            myConnection.execute(c -> c.setPermissions(remotePath, mode));
            myCache.invalidate(remotePath);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isSymLink(VirtualFile file) {
        String remotePath = toRemotePath(file.getPath());
        FileInfo info = myCache.getFileInfo(remotePath);
        return info != null && info.isSymlink();
    }

    @Override
    public byte[] contentsToByteArray(VirtualFile file) throws IOException {
        try {
            String remotePath = toRemotePath(file.getPath());
            ByteBuffer data = myConnection.execute(c -> c.readFile(remotePath));
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            return bytes;
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public InputStream getInputStream(VirtualFile file) throws IOException {
        return new ByteArrayInputStream(contentsToByteArray(file));
    }

    @Override
    public OutputStream getOutputStream(VirtualFile file, Object requestor, long modStamp, long timeStamp) throws IOException {
        String remotePath = toRemotePath(file.getPath());
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    myConnection.executeVoid(c -> c.writeFile(remotePath, ByteBuffer.wrap(toByteArray())));
                    myCache.invalidate(remotePath);
                }
                catch (RemoteAgentException e) {
                    throw new IOException(e.getMessage(), e);
                }
            }
        };
    }

    @Override
    public long getLength(VirtualFile file) {
        String remotePath = toRemotePath(file.getPath());
        FileInfo info = myCache.getFileInfo(remotePath);
        if (info != null) {
            return info.getSize();
        }
        return 0;
    }

    // NewVirtualFileSystem methods

    @Nullable
    @Override
    public VirtualFile findFileByPath(String path) {
        // Delegated to VFS infrastructure
        return null;
    }

    @Nullable
    @Override
    public VirtualFile findFileByPathIfCached(String path) {
        return null;
    }

    @Nullable
    @Override
    public VirtualFile refreshAndFindFileByPath(String path) {
        myCache.invalidate(toRemotePath(path));
        return findFileByPath(path);
    }

    @Override
    public String extractRootPath(String path) {
        // Path format: platformId/remotePath
        // e.g. "remote-a67a9ab9292299b15/home/user/file.txt" -> "remote-a67a9ab9292299b15/"
        // e.g. "remote-a67a9ab9292299b15/C:/Users/file.txt" -> "remote-a67a9ab9292299b15/C:/"
        int firstSlash = path.indexOf('/');
        if (firstSlash < 0) {
            return path + "/";
        }
        String afterPlatformId = path.substring(firstSlash + 1);
        // Windows-style: C:/
        if (afterPlatformId.length() >= 2 && afterPlatformId.charAt(1) == ':') {
            return path.substring(0, firstSlash + 1) + afterPlatformId.substring(0, 3);
        }
        // Unix-style: /
        return path.substring(0, firstSlash + 1);
    }

    @Override
    public int getRank() {
        return 2;
    }

    @Nullable
    @Override
    public FileAttributes getAttributes(VirtualFile file) {
        String remotePath = toRemotePath(file.getPath());
        FileInfo info = myCache.getFileInfo(remotePath);
        if (info == null) {
            try {
                // Try to fetch by listing parent
                VirtualFile parent = file.getParent();
                if (parent != null) {
                    String parentRemotePath = toRemotePath(parent.getPath());
                    List<FileInfo> children = getCachedChildren(parentRemotePath);
                    for (FileInfo child : children) {
                        if (child.getName().equals(file.getName())) {
                            info = child;
                            break;
                        }
                    }
                }
            }
            catch (RemoteAgentException e) {
                return null;
            }
        }
        if (info == null) {
            return null;
        }
        return new FileAttributes(
            info.isDirectory(),
            false,
            info.isSymlink(),
            info.isHidden(),
            info.getSize(),
            info.isSetLastModified() ? info.getLastModified() : 0,
            info.isWritable()
        );
    }

    @Override
    public VirtualFile copyFile(Object requestor, VirtualFile file, VirtualFile newParent, String copyName) throws IOException {
        try {
            String srcRemotePath = toRemotePath(file.getPath());
            String parentRemotePath = toRemotePath(newParent.getPath());
            String targetRemotePath = parentRemotePath + "/" + copyName;
            ByteBuffer data = myConnection.execute(c -> c.readFile(srcRemotePath));
            myConnection.executeVoid(c -> c.writeFile(targetRemotePath, data));
            myCache.invalidate(parentRemotePath);
            return refreshAndFindFileByPath(newParent.getPath() + "/" + copyName);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public VirtualFile createChildDirectory(Object requestor, VirtualFile parent, String dir) throws IOException {
        try {
            String parentRemotePath = toRemotePath(parent.getPath());
            String remotePath = parentRemotePath + "/" + dir;
            myConnection.executeVoid(c -> c.createDirectory(remotePath, false));
            myCache.invalidate(parentRemotePath);
            return refreshAndFindFileByPath(parent.getPath() + "/" + dir);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public VirtualFile createChildFile(Object requestor, VirtualFile parent, String file) throws IOException {
        try {
            String parentRemotePath = toRemotePath(parent.getPath());
            String remotePath = parentRemotePath + "/" + file;
            myConnection.executeVoid(c -> c.writeFile(remotePath, ByteBuffer.wrap(new byte[0])));
            myCache.invalidate(parentRemotePath);
            return refreshAndFindFileByPath(parent.getPath() + "/" + file);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(Object requestor, VirtualFile file) throws IOException {
        try {
            String remotePath = toRemotePath(file.getPath());
            myConnection.execute(c -> c.deleteFile(remotePath));
            myCache.invalidate(remotePath);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void moveFile(Object requestor, VirtualFile file, VirtualFile newParent) throws IOException {
        try {
            String srcRemotePath = toRemotePath(file.getPath());
            String parentRemotePath = toRemotePath(newParent.getPath());
            String targetRemotePath = parentRemotePath + "/" + file.getName();
            ByteBuffer data = myConnection.execute(c -> c.readFile(srcRemotePath));
            myConnection.executeVoid(c -> c.writeFile(targetRemotePath, data));
            myConnection.execute(c -> c.deleteFile(srcRemotePath));
            myCache.invalidate(srcRemotePath);
            myCache.invalidate(parentRemotePath);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void renameFile(Object requestor, VirtualFile file, String newName) throws IOException {
        VirtualFile parent = file.getParent();
        if (parent == null) {
            throw new IOException("Cannot rename root");
        }
        try {
            String srcRemotePath = toRemotePath(file.getPath());
            String parentRemotePath = toRemotePath(parent.getPath());
            String targetRemotePath = parentRemotePath + "/" + newName;
            ByteBuffer data = myConnection.execute(c -> c.readFile(srcRemotePath));
            myConnection.executeVoid(c -> c.writeFile(targetRemotePath, data));
            myConnection.execute(c -> c.deleteFile(srcRemotePath));
            myCache.invalidate(srcRemotePath);
            myCache.invalidate(parentRemotePath);
        }
        catch (RemoteAgentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void refresh(boolean asynchronous) {
        myCache.invalidateAll();
    }

    @Nullable
    @Override
    public Path getNioPath(VirtualFile file) {
        return myNioFileSystem.getPath(toRemotePath(file.getPath()));
    }

    public RemoteAgentConnection getConnection() {
        return myConnection;
    }

    /**
     * Strips the platformId prefix from a VFS path to get the remote filesystem path.
     * e.g. "remote-abc123/home/user/file.txt" -> "/home/user/file.txt"
     * e.g. "remote-abc123/C:/Users/file.txt" -> "C:/Users/file.txt"
     */
    private String toRemotePath(String vfsPath) {
        int firstSlash = vfsPath.indexOf('/');
        if (firstSlash < 0) {
            return "/";
        }
        String remotePath = vfsPath.substring(firstSlash + 1);
        // Unix paths need leading /
        if (!remotePath.isEmpty() && remotePath.charAt(0) != '/' && !(remotePath.length() >= 2 && remotePath.charAt(1) == ':')) {
            remotePath = "/" + remotePath;
        }
        return remotePath.isEmpty() ? "/" : remotePath;
    }

    private List<FileInfo> getCachedChildren(String dirPath) {
        List<FileInfo> cached = myCache.getChildren(dirPath);
        if (cached != null) {
            return cached;
        }
        List<FileInfo> children = myConnection.execute(c -> c.listDirectory(dirPath));
        myCache.putChildren(dirPath, children);
        return children;
    }
}
