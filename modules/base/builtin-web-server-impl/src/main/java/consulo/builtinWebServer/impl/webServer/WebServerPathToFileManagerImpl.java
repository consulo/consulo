// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.webServer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import consulo.annotation.component.ServiceImpl;
import consulo.builtinWebServer.impl.BuiltInWebServerKt;
import consulo.builtinWebServer.webServer.FileResolver;
import consulo.builtinWebServer.webServer.PathInfo;
import consulo.builtinWebServer.webServer.PathQuery;
import consulo.builtinWebServer.webServer.WebServerPathToFileManager;
import consulo.builtinWebServer.webServer.WebServerRootsProvider;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
@ServiceImpl
public final class WebServerPathToFileManagerImpl extends WebServerPathToFileManager {
    private static final long CACHE_SIZE = 4096 * 4;

    private static final FileResolver RELATIVE_PATH_RESOLVER = new FileResolver() {
        @Override
        public @Nullable PathInfo resolve(String path,
                                          VirtualFile root,
                                          @Nullable String moduleName,
                                          boolean isLibrary,
                                          PathQuery pathQuery) {
            try {
                Path relativePath = Path.of(path);
                if (relativePath.isAbsolute() || relativePath.getRoot() != null) {
                    return null;
                }

                if (pathQuery.isUseVfs() || root.getFileSystem() != LocalFileSystem.getInstance() || path.equals(".htaccess")) {
                    VirtualFile file = root.findFileByRelativePath(path);
                    return file == null ? null : new PathInfo(null, file, root, moduleName, isLibrary);
                }

                Path rootPath = root.toNioPath();
                Path file = rootPath.resolve(relativePath).normalize();
                if (file.startsWith(rootPath) && Files.exists(file)) {
                    return new PathInfo(file, null, root, moduleName, isLibrary);
                }
                return null;
            }
            catch (InvalidPathException e) {
                return null;
            }
        }
    };

    private static final FileResolver EMPTY_PATH_RESOLVER = new FileResolver() {
        @Override
        public @Nullable PathInfo resolve(String path,
                                          VirtualFile root,
                                          @Nullable String moduleName,
                                          boolean isLibrary,
                                          PathQuery pathQuery) {
            VirtualFile file = BuiltInWebServerKt.findIndexFile(root);
            return file == null ? null : new PathInfo(null, file, root, moduleName, isLibrary);
        }
    };

    private final Project myProject;

    private final Cache<String, PathInfo> myPathToInfoCache =
        CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).expireAfterAccess(10, TimeUnit.MINUTES).build();
    private final Cache<String, Boolean> myPathToExistShortTermCache =
        CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).expireAfterAccess(5, TimeUnit.SECONDS).build();
    private final Cache<VirtualFile, PathInfo> myVirtualFileToPathInfo =
        CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).expireAfterAccess(11, TimeUnit.MINUTES).build();
    private final LoadingCache<String, List<SuitableRoot>> myParentToSuitableRoot;

    @Inject
    public WebServerPathToFileManagerImpl(Project project) {
        myProject = project;
        myParentToSuitableRoot = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::computeSuitableRoots));

        project.getApplication().getMessageBus().connect(project).subscribe(BulkFileListener.class, new BulkFileListener() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event instanceof VFileContentChangeEvent contentChangeEvent) {
                        VirtualFile file = contentChangeEvent.getFile();
                        if (myProject.getExtensionPoint(WebServerRootsProvider.class)
                            .anyMatchSafe(provider -> provider.isClearCacheOnFileContentChanged(file))) {
                            clearCache();
                            break;
                        }
                    }
                    else {
                        clearCache();
                        break;
                    }
                }
            }
        });
        project.getMessageBus().connect().subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                clearCache();
            }
        });
    }

    private List<SuitableRoot> computeSuitableRoots(String path) {
        List<SuitableRoot> suitableRoots = new ArrayList<>();
        for (VirtualFile root : RootProvider.getAllRoots(myProject)) {
            if (root.isValid() && root.findChild(path) != null) {
                suitableRoots.add(new SuitableRoot(root, null));
            }
        }
        return suitableRoots;
    }

    private void clearCache() {
        myPathToInfoCache.invalidateAll();
        myVirtualFileToPathInfo.invalidateAll();
        myPathToExistShortTermCache.invalidateAll();
        myParentToSuitableRoot.invalidateAll();
    }

    Cache<String, PathInfo> getPathToInfoCache() {
        return myPathToInfoCache;
    }

    Cache<String, Boolean> getPathToExistShortTermCache() {
        return myPathToExistShortTermCache;
    }

    LoadingCache<String, List<SuitableRoot>> getParentToSuitableRoot() {
        return myParentToSuitableRoot;
    }

    @Override
    public @Nullable PathInfo getPathInfo(String path, boolean cacheResult, PathQuery pathQuery) {
        PathInfo pathInfo = myPathToInfoCache.getIfPresent(path);
        if (pathInfo == null || !pathInfo.isValid()) {
            if (Boolean.FALSE.equals(myPathToExistShortTermCache.getIfPresent(path))) {
                return null;
            }

            pathInfo = doFindByRelativePath(path, pathQuery);
            if (cacheResult) {
                if (pathInfo != null && pathInfo.isValid()) {
                    myPathToInfoCache.put(path, pathInfo);
                }
                else {
                    myPathToExistShortTermCache.put(path, Boolean.FALSE);
                }
            }
        }
        return pathInfo;
    }

    @Override
    public @Nullable PathInfo getPathInfo(VirtualFile child) {
        PathInfo result = myVirtualFileToPathInfo.getIfPresent(child);
        if (result == null) {
            result = myProject.getExtensionPoint(WebServerRootsProvider.class).computeSafeIfAny(provider -> provider.getPathInfo(child));
            if (result != null) {
                myVirtualFileToPathInfo.put(child, result);
            }
        }
        return result;
    }

    @Nullable PathInfo doFindByRelativePath(String path, PathQuery pathQuery) {
        PathInfo result = myProject.getExtensionPoint(WebServerRootsProvider.class)
            .computeSafeIfAny(provider -> provider.resolve(path, pathQuery));
        if (result == null) {
            return null;
        }

        VirtualFile file = result.getFile();
        if (file != null) {
            myVirtualFileToPathInfo.put(file, result);
        }
        return result;
    }

    @Override
    public FileResolver getResolver(String path) {
        return path.isEmpty() ? EMPTY_PATH_RESOLVER : RELATIVE_PATH_RESOLVER;
    }
}
