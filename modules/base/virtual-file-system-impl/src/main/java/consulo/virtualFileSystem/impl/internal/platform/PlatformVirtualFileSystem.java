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
package consulo.virtualFileSystem.impl.internal.platform;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.SimpleModificationTracker;
import consulo.platform.PlatformNioFileSystem;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.StandardFileSystems;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.core.local.CoreLocalFileSystem;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Files of other machines, read straight through their {@link PlatformNioFileSystem}. A path is
 * {@code <platform id>/<path>} - a platform id has no slash - so a stored url names its machine and resolves again once
 * that machine is reachable.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ExtensionImpl
public class PlatformVirtualFileSystem extends CoreLocalFileSystem {
    private static final char SEPARATOR = '/';

    private final Map<String, FileSystem> myFileSystems = new ConcurrentHashMap<>();

    @Override
    public String getProtocol() {
        return StandardFileSystems.PLATFORM_PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByNioFile(Path file) {
        if (!(file.getFileSystem() instanceof PlatformNioFileSystem platformFileSystem)) {
            return null;
        }
        if (myFileSystems.put(platformFileSystem.getPlatformId(), file.getFileSystem()) != file.getFileSystem()) {
            if (VirtualFilePointerManager.getInstance() instanceof SimpleModificationTracker tracker) {
                tracker.incModificationCount();
            }
        }
        return super.findFileByNioFile(file);
    }

    @Override
    public @Nullable VirtualFile findFileByPath(String path) {
        int separator = path.indexOf(SEPARATOR);
        if (separator <= 0) {
            return null;
        }
        FileSystem fileSystem = myFileSystems.get(path.substring(0, separator));
        if (fileSystem == null || !fileSystem.isOpen()) {
            return null;
        }
        String filePath = path.substring(separator);
        if (filePath.length() > 2 && filePath.charAt(2) == ':') {
            filePath = filePath.substring(1);
        }
        return super.findFileByNioFile(fileSystem.getPath(filePath));
    }

    @Override
    public String toVfsPath(Path file) {
        String path = FileUtil.toSystemIndependentName(file.toString());
        return file.getFileSystem() instanceof PlatformNioFileSystem platformFileSystem
            ? platformFileSystem.getPlatformId() + (path.startsWith("/") ? path : SEPARATOR + path)
            : path;
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
