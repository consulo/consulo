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
package consulo.ide.impl.virtualFileSystem;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.ui.UISettings;
import consulo.component.ComponentManager;
import consulo.component.util.Iconable;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.localize.LocalizeValue;
import consulo.process.io.ProcessIOExecutorService;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.VirtualFileSystemInternalHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2025-06-20
 */
@Singleton
@ServiceImpl
public class VirtualFileSystemInternalHelperImpl implements VirtualFileSystemInternalHelper {
    @Override
    public boolean isUseSafeWrite() {
        return GeneralSettings.getInstance().isUseSafeWrite();
    }

    @Override
    public boolean isHideKnownExtensionInTabs() {
        return UISettings.getInstance().getHideKnownExtensionInTabs();
    }

    @Override
    public <T> Future<T> executeIO(Callable<T> callable) {
        return ProcessIOExecutorService.INSTANCE.submit(callable);
    }

    @Override
    public Image getFileIcon(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return VfsIconUtil.getIcon(file, flags, (Project) project);
    }

    @RequiredReadAction
    @Nonnull
    @Override
    public Image getFileIconNoDefer(@Nonnull VirtualFile file, @Nullable ComponentManager project, @Iconable.IconFlags int flags) {
        return VfsIconUtil.getIconNoDefer(file, flags, (Project) project);
    }

    @Override
    public void notifyAboutSlowFileWatcher(@Nonnull LocalizeValue cause) {
        Application application = Application.get();
        application.invokeLater(
            () -> NotificationService.getInstance()
                .newWarn(FileWatcherNotificationGroupContributor.NOTIFICATION_GROUP)
                .title(ApplicationLocalize.watcherSlowSync())
                .content(cause)
                .hyperlinkListener(NotificationListener.URL_OPENING_LISTENER)
                .notify(null),
            application.getNoneModalityState()
        );
    }
}
