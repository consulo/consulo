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

import consulo.enviroment.remoteAgent.protocol.FileInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe metadata cache for remote file info with TTL-based expiration.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemoteFileMetadataCache {
    private static final long TTL_MS = 5000; // 5 seconds

    private record CacheEntry(List<FileInfo> children, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TTL_MS;
        }
    }

    private final ConcurrentHashMap<String, CacheEntry> myCache = new ConcurrentHashMap<>();

    @Nullable
    public List<FileInfo> getChildren(String dirPath) {
        CacheEntry entry = myCache.get(normalizePath(dirPath));
        if (entry != null && !entry.isExpired()) {
            return entry.children;
        }
        return null;
    }

    public void putChildren(String dirPath, List<FileInfo> children) {
        myCache.put(normalizePath(dirPath), new CacheEntry(children, System.currentTimeMillis()));
    }

    @Nullable
    public FileInfo getFileInfo(String path) {
        String normalizedPath = normalizePath(path);
        // Check if this is a root
        CacheEntry rootEntry = myCache.get(normalizedPath);
        if (rootEntry != null && !rootEntry.isExpired()) {
            for (FileInfo info : rootEntry.children) {
                if (normalizePath(info.getPath()).equals(normalizedPath)) {
                    return info;
                }
            }
        }
        // Check parent directory cache
        String parentPath = getParentPath(normalizedPath);
        if (parentPath != null) {
            CacheEntry parentEntry = myCache.get(parentPath);
            if (parentEntry != null && !parentEntry.isExpired()) {
                for (FileInfo info : parentEntry.children) {
                    if (normalizePath(info.getPath()).equals(normalizedPath)) {
                        return info;
                    }
                }
            }
        }
        return null;
    }

    public void invalidate(String path) {
        String normalizedPath = normalizePath(path);
        myCache.remove(normalizedPath);
        String parentPath = getParentPath(normalizedPath);
        if (parentPath != null) {
            myCache.remove(parentPath);
        }
    }

    public void invalidateAll() {
        myCache.clear();
    }

    private static String normalizePath(String path) {
        String normalized = path.replace('\\', '/');
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Nullable
    private static String getParentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return lastSlash == 0 ? "/" : null;
        }
        return path.substring(0, lastSlash);
    }
}
