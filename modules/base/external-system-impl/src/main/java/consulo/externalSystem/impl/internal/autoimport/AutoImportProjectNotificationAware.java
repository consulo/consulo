// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.externalSystem.autoimport.ExternalSystemProjectAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectId;
import consulo.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectNotificationAwareListener;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@ServiceImpl
public class AutoImportProjectNotificationAware implements ExternalSystemProjectNotificationAware, Disposable {
    private static final Logger LOG = Logger.getInstance("#consulo.externalSystem.autoimport");

    private final Project myProject;

    private final Set<ExternalSystemProjectId> myProjectsWithNotification = ConcurrentHashMap.newKeySet();

    @Inject
    public AutoImportProjectNotificationAware(Project project) {
        myProject = project;
    }

    public static AutoImportProjectNotificationAware getInstance(Project project) {
        return (AutoImportProjectNotificationAware) ExternalSystemProjectNotificationAware.getInstance(project);
    }

    @Override
    public void notificationNotify(ExternalSystemProjectAware projectAware) {
        ExternalSystemProjectId projectId = projectAware.getProjectId();
        LOG.debug(projectId + ": Notify notification");
        myProjectsWithNotification.add(projectId);
        fireNotificationUpdated();
    }

    @Override
    public void notificationExpire(ExternalSystemProjectId projectId) {
        LOG.debug(projectId + ": Expire notification");
        myProjectsWithNotification.remove(projectId);
        fireNotificationUpdated();
    }

    @Override
    public void notificationExpire() {
        LOG.debug("Expire notification");
        myProjectsWithNotification.clear();
        fireNotificationUpdated();
    }

    @Override
    public void dispose() {
        notificationExpire();
    }

    private void fireNotificationUpdated() {
        if (myProject.isDisposed()) {
            return;
        }
        myProject.getMessageBus().syncPublisher(ExternalSystemProjectNotificationAwareListener.class).onNotificationChanged();
    }

    @Override
    public boolean isNotificationVisible() {
        return !myProjectsWithNotification.isEmpty();
    }

    @Override
    public boolean isNotificationVisible(ProjectSystemId systemId) {
        return myProjectsWithNotification.stream().anyMatch(it -> it.getSystemId().equals(systemId));
    }

    @Override
    public Set<ProjectSystemId> getSystemIds() {
        Set<ProjectSystemId> result = new HashSet<>();
        for (ExternalSystemProjectId projectId : myProjectsWithNotification) {
            result.add(projectId.getSystemId());
        }
        return result;
    }

    public Set<ExternalSystemProjectId> getProjectsWithNotification() {
        return Set.copyOf(myProjectsWithNotification);
    }
}
