// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectTracker;
import consulo.externalSystem.impl.internal.util.ExternalSystemTrustUtil;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;

import java.util.Set;

@ActionImpl(id = "ExternalSystem.ProjectRefreshAction")
public class ProjectRefreshAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public ProjectRefreshAction() {
        super(
            ExternalSystemLocalize.externalSystemReloadNotificationActionReloadTextEmpty(),
            ExternalSystemLocalize.externalSystemReloadNotificationActionReloadDescriptionEmpty(Application.get().getName()),
            PlatformIconGroup.actionsBuildloadchanges()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        refreshProject(project);
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        ExternalSystemProjectNotificationAware notificationAware = ExternalSystemProjectNotificationAware.getInstance(project);
        Set<ProjectSystemId> systemIds = notificationAware.getSystemIds();
        if (!systemIds.isEmpty()) {
            e.getPresentation().setText(getNotificationText(systemIds));
            e.getPresentation().setDescription(getNotificationDescription(project, systemIds));
        }
        e.getPresentation().setEnabled(notificationAware.isNotificationVisible());
    }

    private LocalizeValue getNotificationText(Set<ProjectSystemId> systemIds) {
        LocalizeValue systemsPresentation = ExternalSystemTrustUtil.naturalJoinSystemIds(systemIds);
        return ExternalSystemLocalize.externalSystemReloadNotificationActionReloadText(systemsPresentation);
    }

    private LocalizeValue getNotificationDescription(Project project, Set<ProjectSystemId> systemIds) {
        LocalizeValue systemsPresentation = ExternalSystemTrustUtil.naturalJoinSystemIds(systemIds);
        LocalizeValue productName = project.getApplication().getName();
        return ExternalSystemLocalize.externalSystemReloadNotificationActionReloadDescription(systemsPresentation, productName);
    }

    public static void refreshProject(Project project) {
        ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
        projectTracker.scheduleProjectRefresh();
    }
}
