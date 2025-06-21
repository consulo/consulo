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
package consulo.virtualFileSystem.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.util.Iconable;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2025-06-20
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface VirtualFileSystemInternalHelper {
    String CHANGES_NOT_ALLOWED_DURING_HIGHLIGHTING = "PSI/document/model changes are not allowed during highlighting";

    public static VirtualFileSystemInternalHelper getInstance() {
        return Application.get().getInstance(VirtualFileSystemInternalHelper.class);
    }

    boolean isUseSafeWrite();

    boolean isHideKnownExtensionInTabs();

    <T> Future<T> executeIO(Callable<T> callable);

    Image getFileIcon(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags);

    @RequiredReadAction
    @Nonnull
    Image getFileIconNoDefer(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags);

    void notifyAboutSlowFileWatcher(@Nonnull LocalizeValue cause);
}
