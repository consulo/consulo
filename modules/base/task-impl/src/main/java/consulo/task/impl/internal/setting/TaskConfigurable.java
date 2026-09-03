/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.task.impl.internal.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.NonDefaultProjectConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.task.TaskManager;
import consulo.task.TaskSettings;
import consulo.task.impl.internal.TaskManagerImpl;
import consulo.task.localize.TaskLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class TaskConfigurable extends SimpleConfigurableByProperties
    implements Configurable.NoScroll, ProjectConfigurable, NonDefaultProjectConfigurable {
    private final Project myProject;

    @Inject
    public TaskConfigurable(Project project) {
        myProject = project;
    }

    private TaskManagerImpl.Config getConfig() {
        return ((TaskManagerImpl)TaskManager.getManager(myProject)).getState();
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        TaskManagerImpl.Config config = getConfig();
        TaskSettings settings = TaskSettings.getInstance();

        VerticalLayout root = VerticalLayout.create();

        IntBox historyLengthBox = IntBox.create(config.taskHistoryLength).withRange(0, Integer.MAX_VALUE);
        propertyBuilder.add(historyLengthBox, () -> config.taskHistoryLength, value -> config.taskHistoryLength = value);
        root.add(DockLayout.create().left(Label.create(TaskLocalize.settingsTaskHistoryLength())).right(historyLengthBox));

        CheckBox saveContextBox = CheckBox.create(TaskLocalize.settingsSaveContextOnCommit());
        propertyBuilder.add(saveContextBox, () -> config.saveContextOnCommit, value -> config.saveContextOnCommit = value);
        root.add(saveContextBox);

        TextBox changelistFormatBox = TextBox.create();
        propertyBuilder.add(changelistFormatBox, () -> config.changelistNameFormat, value -> config.changelistNameFormat = value);
        root.add(DockLayout.create()
            .left(Label.create(TaskLocalize.settingsChangelistNameFormat()))
            .right(changelistFormatBox));

        CheckBox alwaysDisplayComboBox = CheckBox.create(TaskLocalize.settingsAlwaysDisplayTaskCombo());
        propertyBuilder.add(
            alwaysDisplayComboBox,
            () -> settings.ALWAYS_DISPLAY_COMBO,
            value -> settings.ALWAYS_DISPLAY_COMBO = value
        );
        root.add(alwaysDisplayComboBox);

        IntBox connectionTimeoutBox = IntBox.create(settings.CONNECTION_TIMEOUT).withRange(0, Integer.MAX_VALUE);
        propertyBuilder.add(connectionTimeoutBox, () -> settings.CONNECTION_TIMEOUT, value -> settings.CONNECTION_TIMEOUT = value);
        root.add(DockLayout.create()
            .left(Label.create(TaskLocalize.settingsConnectionTimeout()))
            .right(HorizontalLayout.create(Space.SMALL)
                .add(connectionTimeoutBox)
                .add(Label.create(TaskLocalize.settingsMilliseconds()))));

        CheckBox updateEnabledBox = CheckBox.create(TaskLocalize.settingsEnableIssueCache(), config.updateEnabled);
        propertyBuilder.add(updateEnabledBox, () -> config.updateEnabled, value -> config.updateEnabled = value);
        root.add(updateEnabledBox);

        IntBox updateCountBox = IntBox.create(config.updateIssuesCount).withRange(0, Integer.MAX_VALUE);
        propertyBuilder.add(updateCountBox, () -> config.updateIssuesCount, value -> config.updateIssuesCount = value);

        IntBox updateIntervalBox = IntBox.create(config.updateInterval).withRange(0, Integer.MAX_VALUE);
        propertyBuilder.add(updateIntervalBox, () -> config.updateInterval, value -> config.updateInterval = value);

        VerticalLayout cacheLayout = VerticalLayout.create();
        cacheLayout.add(HorizontalLayout.create(Space.SMALL)
            .add(Label.create(TaskLocalize.settingsUpdate()))
            .add(updateCountBox)
            .add(Label.create(TaskLocalize.settingsIssuesEvery()))
            .add(updateIntervalBox)
            .add(Label.create(TaskLocalize.settingsMinutes())));

        cacheLayout.setEnabledRecursive(config.updateEnabled);
        updateEnabledBox.addValueListener(event -> cacheLayout.setEnabledRecursive(event.getValue()));

        root.add(LabeledLayout.create(TaskLocalize.settingsCacheSettings(), cacheLayout));

        return root;
    }

    @RequiredUIAccess
    @Override
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        boolean wasUpdateEnabled = getConfig().updateEnabled;

        super.apply(component);

        if (getConfig().updateEnabled && !wasUpdateEnabled) {
            TaskManager.getManager(myProject).updateIssues(null);
        }
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Tasks");
    }

    @Override
    public String getId() {
        return StandardConfigurableIds.TASKS_GROUP;
    }
}
