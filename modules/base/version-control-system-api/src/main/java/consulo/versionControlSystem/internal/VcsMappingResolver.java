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
package consulo.versionControlSystem.internal;

import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.internal.PlatformPathVirtualFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Finds the directory of a mapping - its path is a path of the machine of the project (a remote project's is of its platform),
 * or a path as a virtual file of a platform answers it.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public final class VcsMappingResolver {
    private VcsMappingResolver() {
    }

    /**
     * The path of a directory, as a mapping keeps it - the path on the machine of the directory.
     */
    public static String toMappingPath(VirtualFile directory) {
        return FileUtil.toSystemIndependentName(directory.getPresentableUrl());
    }

    private static @Nullable PlatformPathVirtualFileSystem getPlatformFileSystem() {
        VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.PLATFORM_PROTOCOL);
        return fileSystem instanceof PlatformPathVirtualFileSystem platformFileSystem ? platformFileSystem : null;
    }

    public static @Nullable VirtualFile findDirectory(Project project, String path, boolean refresh) {
        if (path.isEmpty()) {
            return null;
        }

        PlatformPathVirtualFileSystem platformFileSystem = getPlatformFileSystem();
        if (platformFileSystem != null && path.charAt(0) != '/' && platformFileSystem.isPlatformPath(path)) {
            VirtualFileSystem fileSystem = (VirtualFileSystem) platformFileSystem;
            return refresh ? fileSystem.refreshAndFindFileByPath(path) : fileSystem.findFileByPath(path);
        }

        if (project.isDefault() || project.getPlatform() == Platform.current()) {
            LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
            return refresh ? localFileSystem.refreshAndFindFileByPath(path) : localFileSystem.findFileByPath(path);
        }

        Path nioPath;
        try {
            nioPath = project.getPlatform().fs().getPath(path);
        }
        catch (InvalidPathException e) {
            return null;
        }
        VirtualFileManager manager = VirtualFileManager.getInstance();
        return refresh ? manager.refreshAndFindFileByNioPath(nioPath) : manager.findFileByNioPath(nioPath);
    }

    /**
     * The path of the directory of a mapping on its machine - for watching it.
     */
    public static @Nullable Path toNioPath(Project project, String path) {
        if (path.isEmpty()) {
            return null;
        }
        PlatformPathVirtualFileSystem platformFileSystem = getPlatformFileSystem();
        if (platformFileSystem != null && path.charAt(0) != '/' && platformFileSystem.isPlatformPath(path)) {
            return platformFileSystem.toNioPath(path);
        }
        try {
            return project.isDefault() ? Path.of(path) : project.getPlatform().fs().getPath(path);
        }
        catch (InvalidPathException e) {
            return null;
        }
    }

    /**
     * The url of the directory of a mapping, for a pointer - without resolving it.
     */
    public static String toUrl(Project project, String path) {
        PlatformPathVirtualFileSystem platformFileSystem = getPlatformFileSystem();
        if (platformFileSystem != null && !path.isEmpty() && path.charAt(0) != '/' && platformFileSystem.isPlatformPath(path)) {
            return VirtualFileManager.constructUrl(StandardFileSystems.PLATFORM_PROTOCOL, path);
        }
        if (!project.isDefault() && project.getPlatform() != Platform.current()) {
            try {
                String url = VirtualFileManager.getInstance().getUrl(project.getPlatform().fs().getPath(path));
                if (url != null) {
                    return url;
                }
            }
            catch (InvalidPathException ignored) {
            }
        }
        return VirtualFileUtil.pathToUrl(path);
    }
}
