// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.annotation.component.ActionImpl;
import consulo.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

@ActionImpl(id = "ExternalSystem.HideProjectRefreshAction")
public class HideProjectRefreshAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public HideProjectRefreshAction() {
        super(
            ExternalSystemLocalize.externalSystemReloadNotificationActionHideText(),
            ExternalSystemLocalize.externalSystemReloadNotificationActionHideText(),
            PlatformIconGroup.actionsClose()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        ExternalSystemProjectNotificationAware notificationAware = ExternalSystemProjectNotificationAware.getInstance(project);
        notificationAware.notificationExpire();
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        ExternalSystemProjectNotificationAware notificationAware = ExternalSystemProjectNotificationAware.getInstance(project);
        e.getPresentation().setEnabled(notificationAware.isNotificationVisible());
    }
}
