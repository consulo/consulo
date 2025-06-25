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
package consulo.ide.impl.idea.find.actions;

import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2025-06-25
 */
public abstract class FindReplaceInPathActionBase extends AnAction implements DumbAware {
    public static final NotificationGroup NOTIFICATION_GROUP =
        NotificationGroup.toolWindowGroup("Find in Path", ToolWindowId.FIND, false);

    @Nonnull
    private final NotificationService myNotificationService;

    public FindReplaceInPathActionBase(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nonnull NotificationService notificationService
    ) {
        super(text, description);
        myNotificationService = notificationService;
    }

    protected void showNotAvailableMessage(AnActionEvent e, Project project) {
        myNotificationService.newWarn(NOTIFICATION_GROUP)
            .content(LocalizeValue.localizeTODO("'" + e.getPresentation().getText() + "' is not available while search is in progress"))
            .notify(project);
    }
}
