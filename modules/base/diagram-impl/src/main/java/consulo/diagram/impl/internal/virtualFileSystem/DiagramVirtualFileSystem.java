/*
 * Copyright 2013-2025 consulo.io
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
package consulo.diagram.impl.internal.virtualFileSystem;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
@ExtensionImpl
public class DiagramVirtualFileSystem extends BaseVirtualFileSystem {
    public static final String PROTOCOL = "diagram";

    public static DiagramVirtualFileSystem getInstance() {
        return (DiagramVirtualFileSystem) VirtualFileManager.getInstance().getFileSystem(PROTOCOL);
    }

    @Nonnull
    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Nullable
    @Override
    public VirtualFile findFileByPath(@Nonnull String path) {
        List<String> parts = StringUtil.split(path, URLUtil.ARCHIVE_SEPARATOR);
        if (parts.size() < 3) {
            return null;
        }

        return new DiagramVirtualFile(parts.get(1), path, this);
    }

    @Nullable
    @Override
    public VirtualFile refreshAndFindFileByPath(@Nonnull String path) {
        return findFileByPath(path);
    }

    @Override
    public void refresh(boolean asynchronous) {

    }
}
