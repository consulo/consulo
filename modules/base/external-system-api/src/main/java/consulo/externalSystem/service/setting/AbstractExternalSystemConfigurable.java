/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.service.setting;

import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.setting.ExternalSystemSettingsListener;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class that simplifies external system settings management.
 * <p/>
 * The general idea is to provide a control which looks like below:
 * <pre>
 *    ----------------------------------------------
 *   |   linked external projects list              |
 *   |----------------------------------------------
 *   |   linked project-specific settings           |
 *   |----------------------------------------------
 *   |   external system-wide settings (optional)   |
 * ----------------------------------------------
 * </pre>
 *
 * @author Denis Zhdanov
 * @since 2013-04-30
 */
public abstract class AbstractExternalSystemConfigurable<
    ProjectSettings extends ExternalProjectSettings,
    L extends ExternalSystemSettingsListener<ProjectSettings>,
    SystemSettings extends AbstractExternalSystemSettings<SystemSettings, ProjectSettings, L>
> implements SearchableConfigurable {
    private final List<ProjectSettings> myProjectSettings = new ArrayList<>();
    private final Map<ProjectSettings, ProjectSettingsEntry> myProjectEntries = new LinkedHashMap<>();

    private final ProjectSystemId myExternalSystemId;
    private final Project myProject;

    private @Nullable ExternalSystemSettingsConfigurable<SystemSettings> mySystemSettingsConfigurable;

    private @Nullable VerticalLayout myRoot;
    private @Nullable DockLayout myProjectSettingsHolder;
    private @Nullable Disposable myUiDisposable;
    private @Nullable Disposable myContentDisposable;

    private record ProjectSettingsEntry(Object configurable, Component component) {
    }

    protected AbstractExternalSystemConfigurable(Project project, ProjectSystemId externalSystemId) {
        myProject = project;
        myExternalSystemId = externalSystemId;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return myExternalSystemId.getDisplayName();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable uiDisposable) {
        if (myRoot == null) {
            myUiDisposable = uiDisposable;
            myRoot = VerticalLayout.create();
            buildContent();
        }
        return myRoot;
    }

    @SuppressWarnings("unchecked")
    private SystemSettings getSettings() {
        ExternalSystemManager<ProjectSettings, L, SystemSettings, ?, ?> manager =
            (ExternalSystemManager<ProjectSettings, L, SystemSettings, ?, ?>)ExternalSystemApiUtil.getManager(myExternalSystemId);
        assert manager != null;
        return manager.getSettingsProvider().apply(myProject);
    }

    @RequiredUIAccess
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildContent() {
        VerticalLayout root = myRoot;
        if (root == null) {
            return;
        }

        if (myContentDisposable != null) {
            Disposer.dispose(myContentDisposable);
        }
        myContentDisposable = Disposable.newDisposable("external system settings content");
        Disposer.register(myUiDisposable, myContentDisposable);

        root.removeAll();
        myProjectSettings.clear();
        myProjectEntries.clear();

        SystemSettings settings = getSettings();

        myProjectSettings.addAll(settings.getLinkedProjectsSettings());
        myProjectSettings.sort(Comparator.comparing(setting -> getProjectName(setting.getExternalProjectPath())));

        DockLayout projectSettingsHolder = DockLayout.create();
        myProjectSettingsHolder = projectSettingsHolder;

        ListBox<ProjectSettings> projectsBox = ListBox.create(myProjectSettings);
        projectsBox.setRender((presentation, item) -> {
            ProjectSettings value = item.getValue();
            presentation.append(value == null ? "" : getProjectName(value.getExternalProjectPath()));
        });
        projectsBox.addValueListener(event -> showProjectSettings(event.getValue()));

        root.add(LabeledLayout.create(
            ExternalSystemLocalize.settingsTitleLinkedProjects(myExternalSystemId.getReadableName()),
            ScrollableLayout.create(projectsBox)
        ));
        root.add(LabeledLayout.create(ExternalSystemLocalize.settingsTitleProjectSettings(), projectSettingsHolder));

        mySystemSettingsConfigurable = getConfigurableFactory().createSystemSettingsConfigurable(
            settings,
            ExternalSystemSettingsPlace.SETTINGS
        );
        if (mySystemSettingsConfigurable != null) {
            root.add(LabeledLayout.create(
                ExternalSystemLocalize.settingsTitleSystemSettings(myExternalSystemId.getDisplayName()),
                mySystemSettingsConfigurable.createUIComponent(myContentDisposable)
            ));
        }

        if (!myProjectSettings.isEmpty()) {
            projectsBox.setValueByIndex(0);
        }
    }

    @SuppressWarnings("rawtypes")
    private ExternalSystemSettingsConfigurableFactory getConfigurableFactory() {
        return ExternalSystemApiUtil.getSettingsConfigurableFactoryStrict(myExternalSystemId);
    }

    @RequiredUIAccess
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void showProjectSettings(@Nullable ProjectSettings settings) {
        DockLayout holder = myProjectSettingsHolder;
        if (holder == null) {
            return;
        }

        holder.removeAll();

        if (settings == null) {
            return;
        }

        ProjectSettingsEntry entry = myProjectEntries.get(settings);
        if (entry == null) {
            AbstractExternalProjectSettingsConfigurable configurable = getConfigurableFactory()
                .createProjectSettingsConfigurable(cloneSettings(settings), ExternalSystemSettingsPlace.SETTINGS);
            entry = new ProjectSettingsEntry(configurable, configurable.createUIComponent(myContentDisposable));
            myProjectEntries.put(settings, entry);
        }

        holder.center(entry.component());
    }

    @SuppressWarnings("unchecked")
    private AbstractExternalProjectSettingsConfigurable<ProjectSettings> configurableOf(ProjectSettingsEntry entry) {
        return (AbstractExternalProjectSettingsConfigurable<ProjectSettings>)entry.configurable();
    }

    @SuppressWarnings("unchecked")
    private ProjectSettings cloneSettings(ProjectSettings settings) {
        return (ProjectSettings)settings.clone();
    }

    @SuppressWarnings("MethodMayBeStatic")
    protected String getProjectName(String path) {
        File file = new File(path);
        return file.isDirectory() || file.getParentFile() == null ? file.getName() : file.getParentFile().getName();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        for (ProjectSettingsEntry entry : myProjectEntries.values()) {
            if (configurableOf(entry).isModified()) {
                return true;
            }
        }
        return mySystemSettingsConfigurable != null && mySystemSettingsConfigurable.isModified();
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        SystemSettings systemSettings = getSettings();
        L publisher = systemSettings.getPublisher();
        publisher.onBulkChangeStart();
        try {
            List<ProjectSettings> projectSettings = new ArrayList<>();
            for (ProjectSettings setting : myProjectSettings) {
                ProjectSettingsEntry entry = myProjectEntries.get(setting);
                if (entry == null) {
                    projectSettings.add(cloneSettings(setting));
                    continue;
                }

                AbstractExternalProjectSettingsConfigurable<ProjectSettings> configurable = configurableOf(entry);
                configurable.apply();
                projectSettings.add(cloneSettings(configurable.getSettings()));
            }
            systemSettings.setLinkedProjectsSettings(projectSettings);

            if (mySystemSettingsConfigurable != null) {
                mySystemSettingsConfigurable.apply();
            }
        }
        finally {
            publisher.onBulkChangeEnd();
        }
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        buildContent();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myProjectSettings.clear();
        myProjectEntries.clear();
        myProjectSettingsHolder = null;
        mySystemSettingsConfigurable = null;
        myContentDisposable = null;
        myUiDisposable = null;
        myRoot = null;
    }
}
