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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Path implementation for remote filesystem.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemotePath implements Path {
    private final RemoteNioFileSystem myFileSystem;
    private final String myPath;
    private final boolean myAbsolute;

    RemotePath(RemoteNioFileSystem fileSystem, String path) {
        myFileSystem = fileSystem;
        // normalize separators to /
        myPath = path.replace('\\', '/');
        myAbsolute = myPath.startsWith("/") || (myPath.length() >= 2 && myPath.charAt(1) == ':');
    }

    @Override
    public FileSystem getFileSystem() {
        return myFileSystem;
    }

    @Override
    public boolean isAbsolute() {
        return myAbsolute;
    }

    @Override
    public Path getRoot() {
        if (!myAbsolute) {
            return null;
        }
        // Windows-style: C:/
        if (myPath.length() >= 2 && myPath.charAt(1) == ':') {
            return new RemotePath(myFileSystem, myPath.substring(0, 3));
        }
        return new RemotePath(myFileSystem, "/");
    }

    @Override
    public Path getFileName() {
        if (myPath.isEmpty() || myPath.equals("/")) {
            return null;
        }
        String normalized = myPath.endsWith("/") ? myPath.substring(0, myPath.length() - 1) : myPath;
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash < 0) {
            return new RemotePath(myFileSystem, normalized);
        }
        return new RemotePath(myFileSystem, normalized.substring(lastSlash + 1));
    }

    @Override
    public Path getParent() {
        if (myPath.isEmpty() || myPath.equals("/")) {
            return null;
        }
        String normalized = myPath.endsWith("/") ? myPath.substring(0, myPath.length() - 1) : myPath;
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash < 0) {
            return null;
        }
        if (lastSlash == 0) {
            return new RemotePath(myFileSystem, "/");
        }
        return new RemotePath(myFileSystem, normalized.substring(0, lastSlash));
    }

    @Override
    public int getNameCount() {
        if (myPath.isEmpty() || myPath.equals("/")) {
            return 0;
        }
        String[] parts = getNameParts();
        return parts.length;
    }

    @Override
    public Path getName(int index) {
        String[] parts = getNameParts();
        if (index < 0 || index >= parts.length) {
            throw new IllegalArgumentException("Index " + index + " out of range for " + parts.length + " elements");
        }
        return new RemotePath(myFileSystem, parts[index]);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        String[] parts = getNameParts();
        if (beginIndex < 0 || endIndex > parts.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = beginIndex; i < endIndex; i++) {
            if (i > beginIndex) {
                sb.append('/');
            }
            sb.append(parts[i]);
        }
        return new RemotePath(myFileSystem, sb.toString());
    }

    @Override
    public boolean startsWith(Path other) {
        if (!(other instanceof RemotePath rp) || rp.myFileSystem != myFileSystem) {
            return false;
        }
        return myPath.startsWith(rp.myPath);
    }

    @Override
    public boolean endsWith(Path other) {
        if (!(other instanceof RemotePath rp) || rp.myFileSystem != myFileSystem) {
            return false;
        }
        return myPath.endsWith(rp.myPath);
    }

    @Override
    public Path normalize() {
        // Simple normalization: remove . and ..
        String[] parts = myPath.split("/");
        java.util.List<String> normalized = new java.util.ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!normalized.isEmpty()) {
                    normalized.removeLast();
                }
            }
            else {
                normalized.add(part);
            }
        }
        String result = String.join("/", normalized);
        if (myAbsolute) {
            result = "/" + result;
        }
        return new RemotePath(myFileSystem, result);
    }

    @Override
    public Path resolve(Path other) {
        if (other instanceof RemotePath rp) {
            if (rp.isAbsolute()) {
                return rp;
            }
            if (rp.myPath.isEmpty()) {
                return this;
            }
            String combined = myPath.endsWith("/") ? myPath + rp.myPath : myPath + "/" + rp.myPath;
            return new RemotePath(myFileSystem, combined);
        }
        return resolve(other.toString());
    }

    @Override
    public Path resolve(String other) {
        return resolve(new RemotePath(myFileSystem, other));
    }

    @Override
    public Path relativize(Path other) {
        if (!(other instanceof RemotePath rp) || rp.myFileSystem != myFileSystem) {
            throw new IllegalArgumentException("Cannot relativize paths from different file systems");
        }
        if (myAbsolute != rp.myAbsolute) {
            throw new IllegalArgumentException("Cannot relativize absolute and relative paths");
        }
        String[] baseParts = getNameParts();
        String[] otherParts = rp.getNameParts();

        int common = 0;
        while (common < baseParts.length && common < otherParts.length && baseParts[common].equals(otherParts[common])) {
            common++;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = common; i < baseParts.length; i++) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append("..");
        }
        for (int i = common; i < otherParts.length; i++) {
            if (!sb.isEmpty()) {
                sb.append('/');
            }
            sb.append(otherParts[i]);
        }
        return new RemotePath(myFileSystem, sb.toString());
    }

    @Override
    public URI toUri() {
        return URI.create("remote-agent://" + myFileSystem.getPlatformId() + myPath);
    }

    @Override
    public Path toAbsolutePath() {
        if (myAbsolute) {
            return this;
        }
        String workspacePath = myFileSystem.getWorkspacePath();
        return new RemotePath(myFileSystem, workspacePath + "/" + myPath);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return toAbsolutePath().normalize();
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException("Remote paths cannot be converted to local File");
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("File watching is not supported on remote file systems");
    }

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<>() {
            private int index = 0;
            private final int count = getNameCount();

            @Override
            public boolean hasNext() {
                return index < count;
            }

            @Override
            public Path next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return getName(index++);
            }
        };
    }

    @Override
    public int compareTo(Path other) {
        if (other instanceof RemotePath rp) {
            return myPath.compareTo(rp.myPath);
        }
        return myPath.compareTo(other.toString());
    }

    @Override
    public String toString() {
        return myPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemotePath rp)) {
            return false;
        }
        return myFileSystem == rp.myFileSystem && myPath.equals(rp.myPath);
    }

    @Override
    public int hashCode() {
        return myPath.hashCode();
    }

    String getPathString() {
        return myPath;
    }

    private String[] getNameParts() {
        String stripped = myPath;
        // Remove root prefix
        if (stripped.startsWith("/")) {
            stripped = stripped.substring(1);
        }
        // Handle Windows-style: C:/path
        if (stripped.length() >= 2 && stripped.charAt(1) == ':') {
            stripped = stripped.substring(stripped.indexOf('/') + 1);
        }
        if (stripped.isEmpty()) {
            return new String[0];
        }
        if (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        return stripped.split("/");
    }
}
