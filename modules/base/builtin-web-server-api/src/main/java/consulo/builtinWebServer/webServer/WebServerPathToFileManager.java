// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.webServer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * Implement {@link WebServerRootsProvider} to add your provider
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class WebServerPathToFileManager {
    private static final PathQuery DEFAULT_PATH_QUERY = new PathQuery();

    public static WebServerPathToFileManager getInstance(Project project) {
        return project.getInstance(WebServerPathToFileManager.class);
    }

    public @Nullable VirtualFile findVirtualFile(String path) {
        return findVirtualFile(path, true, DEFAULT_PATH_QUERY);
    }

    public @Nullable VirtualFile findVirtualFile(String path, boolean cacheResult) {
        return findVirtualFile(path, cacheResult, DEFAULT_PATH_QUERY);
    }

    public @Nullable VirtualFile findVirtualFile(String path, boolean cacheResult, PathQuery pathQuery) {
        PathInfo pathInfo = getPathInfo(path, cacheResult, pathQuery);
        return pathInfo == null ? null : pathInfo.getOrResolveVirtualFile();
    }

    public @Nullable PathInfo getPathInfo(String path) {
        return getPathInfo(path, true, DEFAULT_PATH_QUERY);
    }

    public @Nullable PathInfo getPathInfo(String path, boolean cacheResult) {
        return getPathInfo(path, cacheResult, DEFAULT_PATH_QUERY);
    }

    public abstract @Nullable PathInfo getPathInfo(String path, boolean cacheResult, PathQuery pathQuery);

    public @Nullable String getPath(VirtualFile file) {
        PathInfo pathInfo = getPathInfo(file);
        return pathInfo == null ? null : pathInfo.getPath();
    }

    public abstract @Nullable PathInfo getPathInfo(VirtualFile child);

    public abstract FileResolver getResolver(String path);
}
