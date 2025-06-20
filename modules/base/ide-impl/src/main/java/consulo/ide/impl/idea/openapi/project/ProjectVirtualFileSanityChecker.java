/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.openapi.project;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.container.boot.ContainerPathManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.UIAccess;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.annotation.Nonnull;

import java.io.FileNotFoundException;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ExtensionImpl
public class ProjectVirtualFileSanityChecker implements BackgroundStartupActivity {
    private static final Logger LOG = Logger.getInstance(ProjectVirtualFileSanityChecker.class);

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        try {
            String path = project.getBasePath();
            if (path == null || FileUtil.isAncestor(ContainerPathManager.get().getConfigPath(), path, true)) {
                return;
            }

            PersistentFS fs = (PersistentFS) ManagingFS.getInstance();

            boolean expected = Platform.current().fs().isCaseSensitive();
            boolean actual = fs.isFileSystemCaseSensitive(path);
            LOG.info(path + " case-sensitivity: expected=" + expected + " actual=" + actual);
            if (actual != expected) {
                int prefix = expected ? 1 : 0;  // IDE=true -> FS=false -> prefix='in'
                LocalizeValue title = ApplicationLocalize.fsCaseSensitivityMismatchTitle();
                LocalizeValue text = ApplicationLocalize.fsCaseSensitivityMismatchMessage(prefix);
                Notifications.Bus.notify(
                    new Notification(
                        Notifications.SYSTEM_MESSAGES_GROUP,
                        title.get(),
                        text.get(),
                        NotificationType.WARNING,
                        NotificationListener.URL_OPENING_LISTENER
                    ),
                    project
                );
            }

            //ProjectFsStatsCollector.caseSensitivity(myProject, actual);
        }
        catch (FileNotFoundException e) {
            LOG.warn(e);
        }
    }
}
