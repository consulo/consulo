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
import consulo.application.Application;
import consulo.application.localize.ApplicationLocalize;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.Notifications;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.UIAccess;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.internal.PersistentFS;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.FileNotFoundException;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ExtensionImpl
public class ProjectVirtualFileSanityChecker implements BackgroundStartupActivity {
    private static final Logger LOG = Logger.getInstance(ProjectVirtualFileSanityChecker.class);

    @Nonnull
    private final NotificationService myNotificationService;

    @Inject
    public ProjectVirtualFileSanityChecker(@Nonnull NotificationService notificationService) {
        myNotificationService = notificationService;
    }

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
                myNotificationService.newWarn(Notifications.SYSTEM_MESSAGES_GROUP)
                    .title(ApplicationLocalize.fsCaseSensitivityMismatchTitle())
                    .content(ApplicationLocalize.fsCaseSensitivityMismatchMessage(prefix))
                    .hyperlinkListener(NotificationListener.URL_OPENING_LISTENER)
                    .notify(project);
            }

            //ProjectFsStatsCollector.caseSensitivity(myProject, actual);
        }
        catch (FileNotFoundException e) {
            LOG.warn(e);
        }
    }
}
