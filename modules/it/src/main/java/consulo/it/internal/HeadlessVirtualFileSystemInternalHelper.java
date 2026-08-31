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
package consulo.it.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.util.Iconable;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.VirtualFileSystemInternalHelper;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Headless {@code VirtualFileSystemInternalHelper}: the production impl lives in {@code ide-impl}.
 * <p>
 * Required by {@link consulo.virtualFileSystem.internal.DiskQueryRelay}, which offloads disk access
 * through {@link #executeIO} whenever a progress indicator is present - so any VFS call made under a
 * progress fails without this binding. Bound only under the {@link ComponentProfiles#INTEGRATION_TEST}
 * profile.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessVirtualFileSystemInternalHelper implements VirtualFileSystemInternalHelper {
    @Override
    public boolean isUseSafeWrite() {
        return false;
    }

    @Override
    public boolean isHideKnownExtensionInTabs() {
        return false;
    }

    @Override
    public <T> Future<T> executeIO(Callable<T> callable) {
        return Application.get().executeOnPooledThread(callable);
    }

    @Override
    public Image getFileIcon(VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return Image.empty(Image.DEFAULT_ICON_SIZE);
    }

    @Override
    @RequiredReadAction
    public Image getFileIconNoDefer(VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return Image.empty(Image.DEFAULT_ICON_SIZE);
    }

    @Override
    public void notifyAboutSlowFileWatcher(LocalizeValue cause) {
    }
}
