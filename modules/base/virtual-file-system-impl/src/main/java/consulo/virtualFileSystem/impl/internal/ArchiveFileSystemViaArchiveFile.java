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
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePointerCapableFileSystem;
import consulo.virtualFileSystem.archive.ArchiveFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveHandler;
import consulo.virtualFileSystem.archive.BaseArchiveFileSystem;
import consulo.virtualFileSystem.impl.internal.zip.JarHandler;
import consulo.virtualFileSystem.internal.VfsImplUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * @author VISTALL
 * @since 06-Aug-16
 */
public abstract class ArchiveFileSystemViaArchiveFile extends BaseArchiveFileSystem implements ArchiveFileSystem, VirtualFilePointerCapableFileSystem {
    private final Set<String> myNoCopyJarPaths;
    private final String myProtocol;

    protected ArchiveFileSystemViaArchiveFile(@Nonnull String protocol) {
        myProtocol = protocol;
        boolean noCopy = SystemProperties.getBooleanProperty("idea.jars.nocopy", !Platform.current().os().isWindows());
        myNoCopyJarPaths = noCopy ? null : Sets.newConcurrentHashSet(FileUtil.PATH_HASHING_STRATEGY);
    }

    @Nonnull
    public abstract ArchiveFile createArchiveFile(@Nonnull String filePath) throws IOException;

    @Nonnull
    @Override
    public final String getProtocol() {
        return myProtocol;
    }

    @Override
    public boolean isMakeCopyOfJar(@Nonnull File originalJar) {
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

    @Nonnull
    @Override
    public ArchiveHandler getHandler(@Nonnull VirtualFile entryFile) {
        return VfsImplUtil.getHandler(this, entryFile, JarHandler::new);
    }
}
