/*
 * Copyright 2013-2016 consulo.io
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
package consulo.virtualFileSystem.impl.internal;

import consulo.platform.Platform;
import consulo.util.collection.Sets;
import consulo.util.io.FileUtil;
import consulo.util.io.URLUtil;
import consulo.util.lang.SystemProperties;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.VirtualFilePointerCapableFileSystem;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.internal.ArchiveHandler;
import consulo.virtualFileSystem.internal.BaseArchiveFileSystem;
import consulo.virtualFileSystem.impl.internal.zip.JarHandler;
import consulo.virtualFileSystem.impl.internal.zip.PlatformArchiveHandler;
import consulo.virtualFileSystem.internal.PlatformArchives;
import consulo.virtualFileSystem.internal.VfsImplUtil;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * @author VISTALL
 * @since 06-Aug-16
 */
public abstract class ArchiveFileSystemViaArchiveFile extends BaseArchiveFileSystem implements ArchiveFileSystem, VirtualFilePointerCapableFileSystem {
    private final @Nullable Set<String> myNoCopyJarPaths;
    private final String myProtocol;
    private final boolean myPlatformHosted;

    protected ArchiveFileSystemViaArchiveFile(String protocol) {
        this(protocol, false);
    }

    /**
     * @param platformHosted the archives are files of other platforms: the protocol is {@code platform-<protocol>}, the host
     *                       file system is {@code platform}, the archives are read through their paths and never copied
     */
    protected ArchiveFileSystemViaArchiveFile(String protocol, boolean platformHosted) {
        myPlatformHosted = platformHosted;
        myProtocol = platformHosted ? PlatformArchives.getPlatformProtocol(protocol) : protocol;
        boolean noCopy = platformHosted || SystemProperties.getBooleanProperty("idea.jars.nocopy", !Platform.current().os().isWindows());
        myNoCopyJarPaths = noCopy ? null : Sets.newConcurrentHashSet(FileUtil.PATH_HASHING_STRATEGY);
    }

    protected final boolean isPlatformHosted() {
        return myPlatformHosted;
    }

    /**
     * The same file system for the archives of other platforms - {@code null} if they are not supported.
     */
    protected abstract @Nullable ArchiveFileSystemViaArchiveFile newPlatformFileSystem();

    /**
     * @param path the archive - a file of the local machine, or of another platform, read through its path
     */
    public abstract ArchiveFile createArchiveFile(Path path) throws IOException;

    @Override
    public @Nullable BaseArchiveFileSystem createPlatformFileSystem() {
        return myPlatformHosted ? null : newPlatformFileSystem();
    }

    @Override
    public VirtualFileSystem getHostFileSystem() {
        return myPlatformHosted
            ? VirtualFileManager.getInstance().getRequiredFileSystem(StandardFileSystems.PLATFORM_PROTOCOL)
            : super.getHostFileSystem();
    }

    /**
     * The NIO view of the archives is of the local ones only.
     */
    @Override
    public @Nullable Path getNioPath(VirtualFile file) {
        return myPlatformHosted ? null : super.getNioPath(file);
    }

    /**
     * An archive of another platform as it is on its machine, the platform id is internal: {@code /path/to/lib.jar!/entry}.
     */
    @Override
    public String extractPresentableUrl(String path) {
        if (!myPlatformHosted) {
            return super.extractPresentableUrl(path);
        }
        int separator = path.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        String hostPath = separator < 0 ? path : path.substring(0, separator);
        String entryPath = separator < 0 ? "" : path.substring(separator + URLUtil.ARCHIVE_SEPARATOR.length());
        String host = getHostFileSystem().extractPresentableUrl(hostPath);
        return entryPath.isEmpty() ? host : host + URLUtil.ARCHIVE_SEPARATOR + entryPath;
    }

    
    @Override
    public final String getProtocol() {
        return myProtocol;
    }

    @Override
    public boolean isMakeCopyOfJar(File originalJar) {
        if (myPlatformHosted) {
            // a file of another platform never comes to this machine
            return false;
        }
        return !(myNoCopyJarPaths == null || myNoCopyJarPaths.contains(originalJar.getPath()));
    }

    @Override
    public void setNoCopyJarForPath(String pathInJar) {
        if (myNoCopyJarPaths == null || pathInJar == null) {
            return;
        }
        int index = pathInJar.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        if (index < 0) {
            return;
        }
        String path = FileUtil.toSystemIndependentName(pathInJar.substring(0, index));
        myNoCopyJarPaths.add(path);
    }

    
    @Override
    public ArchiveHandler getHandler(VirtualFile entryFile) {
        if (myPlatformHosted) {
            VirtualFileSystem hostFileSystem = getHostFileSystem();
            return VfsImplUtil.getHandler(this, entryFile, (hostPath, fileSystem) -> new PlatformArchiveHandler(hostPath, hostFileSystem, fileSystem));
        }
        return VfsImplUtil.getHandler(this, entryFile, JarHandler::new);
    }
}
