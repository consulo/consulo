// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.autoimport;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.project.Project;

import java.util.Set;

/**
 * Bridge between auto-reload backend and notification view about that project is needed to reload.
 * Notifications can be shown in editor floating toolbar, editor banner, etc.
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ExternalSystemProjectNotificationAware {
    static ExternalSystemProjectNotificationAware getInstance(Project project) {
        return project.getInstance(ExternalSystemProjectNotificationAware.class);
    }

    /**
     * Requests to show notifications for reload project that defined by {@code projectAware}
     */
    void notificationNotify(ExternalSystemProjectAware projectAware);

    /**
     * Requests to hide all notifications for all projects.
     */
    void notificationExpire();

    /**
     * Requests to hide all notifications for project that defined by {@code projectId}
     *
     * @see ExternalSystemProjectAware#getProjectId()
     */
    void notificationExpire(ExternalSystemProjectId projectId);

    /**
     * Checks that notifications should be shown.
     */
    boolean isNotificationVisible();

    /**
     * Checks that notifications should be shown with defined {@code systemId}.
     */
    boolean isNotificationVisible(ProjectSystemId systemId);

    /**
     * Gets list of project ids which should be reloaded.
     */
    Set<ProjectSystemId> getSystemIds();
}
