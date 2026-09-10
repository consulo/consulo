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
package consulo.task.impl.internal.timeTracking;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ConfigurationException;
import consulo.configurable.NonDefaultProjectConfigurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.task.localize.TaskLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class TimeTrackingConfigurable extends SimpleConfigurableByProperties
    implements SearchableConfigurable, ProjectConfigurable, NonDefaultProjectConfigurable {
    private final Project myProject;

    @Inject
    public TimeTrackingConfigurable(Project project) {
        myProject = project;
    }

    private TimeTrackingManager.Config getConfig() {
        return TimeTrackingManager.getInstance(myProject).getState();
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        TimeTrackingManager.Config config = getConfig();

        VerticalLayout root = VerticalLayout.create();

        CheckBox enableBox = CheckBox.create(TaskLocalize.settingsEnableTimeTracking(), config.enabled);
        propertyBuilder.add(enableBox, () -> config.enabled, value -> config.enabled = value);
        root.add(enableBox);

        IntBox suspendDelayBox = IntBox.create(config.suspendDelayInSeconds).withRange(0, Integer.MAX_VALUE);
        propertyBuilder.add(
            suspendDelayBox,
            () -> config.suspendDelayInSeconds,
            value -> config.suspendDelayInSeconds = value
        );
        suspendDelayBox.setEnabled(config.enabled);
        enableBox.addValueListener(event -> suspendDelayBox.setEnabled(event.getValue()));

        root.add(DockLayout.create()
            .left(Label.create(TaskLocalize.settingsSuspendDelay()))
            .right(HorizontalLayout.create(Space.SMALL).add(suspendDelayBox).add(Label.create(TaskLocalize.settingsSeconds()))));

        return root;
    }

    @RequiredUIAccess
    @Override
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        boolean wasEnabled = getConfig().enabled;

        super.apply(component);

        if (getConfig().enabled != wasEnabled) {
            TimeTrackingManager.getInstance(myProject).updateTimeTrackingToolWindow();
        }
    }

    @Override
    public String getId() {
        return "tasks.timeTracking";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Time Tracking");
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.TASKS_GROUP;
    }
}
