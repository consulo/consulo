// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.service.project.autoimport;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.autoimport.ExternalSystemProjectId;
import consulo.externalSystem.autoimport.ExternalSystemProjectTracker;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.autoimport.ExternalSystemAutoImportAware;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.setting.ExternalSystemSettingsListenerEx;
import consulo.project.Project;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Set;

@ExtensionImpl
public class ExternalProjectsSettingsWatcher implements ExternalSystemSettingsListenerEx {
    private final Project myProject;

    @Inject
    public ExternalProjectsSettingsWatcher(Project project) {
        myProject = project;
    }

    @Override
    public void onProjectsLoaded(ExternalSystemManager<?, ?, ?, ?, ?> manager, Collection<? extends ExternalProjectSettings> settings) {
        if (!(manager instanceof ExternalSystemAutoImportAware)) {
            return;
        }

        ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(myProject);
        ProjectSystemId systemId = manager.getSystemId();
        for (ExternalProjectSettings projectSettings : settings) {
            projectTracker.activate(new ExternalSystemProjectId(systemId, projectSettings.getExternalProjectPath()));
        }
    }

    @Override
    public void onProjectsLinked(ExternalSystemManager<?, ?, ?, ?, ?> manager, Collection<? extends ExternalProjectSettings> settings) {
        if (!(manager instanceof ExternalSystemAutoImportAware autoImportAware)) {
            return;
        }

        ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(myProject);
        ProjectSystemId systemId = manager.getSystemId();
        for (ExternalProjectSettings projectSettings : settings) {
            ExternalSystemProjectId id = new ExternalSystemProjectId(systemId, projectSettings.getExternalProjectPath());
            projectTracker.register(new ProjectAware(myProject, id, autoImportAware));
        }
    }

    @Override
    public void onProjectsUnlinked(ExternalSystemManager<?, ?, ?, ?, ?> manager, Set<String> linkedProjectPaths) {
        if (!(manager instanceof ExternalSystemAutoImportAware)) {
            return;
        }

        ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(myProject);
        ProjectSystemId systemId = manager.getSystemId();
        for (String linkedProjectPath : linkedProjectPaths) {
            projectTracker.remove(new ExternalSystemProjectId(systemId, linkedProjectPath));
        }
    }
}
