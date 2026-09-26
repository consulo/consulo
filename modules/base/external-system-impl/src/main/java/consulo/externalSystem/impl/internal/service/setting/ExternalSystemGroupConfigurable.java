// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.service.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.PropertiesComponent;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.externalSystem.autoimport.ExternalSystemProjectTrackerSettings;
import consulo.externalSystem.autoimport.ExternalSystemProjectTrackerSettings.AutoReloadType;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.RadioGroup;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.Indenter;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class ExternalSystemGroupConfigurable extends SimpleConfigurableByProperties implements ProjectConfigurable {
    private static final String PREVIOUS_KEY = "settings.build.tools.auto.reload";

    private final Project myProject;

    @Inject
    public ExternalSystemGroupConfigurable(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        ExternalSystemProjectTrackerSettings settings = ExternalSystemProjectTrackerSettings.getInstance(myProject);
        PropertiesComponent propertiesComponent = ProjectPropertiesComponent.getInstance(myProject);

        VerticalLayout root = VerticalLayout.create();

        CheckBox reloadBox = CheckBox.create(ExternalSystemLocalize.settingsBuildToolsAutoReloadRadioButtonGroupTitle());
        root.add(reloadBox);

        RadioGroup<AutoReloadType> reloadTypeGroup = RadioGroup.create();
        VerticalLayout buttonsLayout = VerticalLayout.create();
        buttonsLayout.add(reloadTypeGroup.newButton(
            ExternalSystemLocalize.settingsBuildToolsAutoReloadRadioButtonAllLabel(),
            AutoReloadType.ALL
        ));
        buttonsLayout.add(reloadTypeGroup.newButton(
            ExternalSystemLocalize.settingsBuildToolsAutoReloadRadioButtonSelectiveLabel(),
            AutoReloadType.SELECTIVE
        ));
        Label selectiveComment = Label.create(ExternalSystemLocalize.settingsBuildToolsAutoReloadRadioButtonSelectiveComment());
        selectiveComment.setForegroundColor(ComponentColors.INFO_FOREGROUND);
        selectiveComment.paddingBuilder().leftSet(Space.XX_LARGE).apply();
        buttonsLayout.add(selectiveComment);
        root.add(Indenter.indent(buttonsLayout));

        reloadBox.addValueListener(event -> buttonsLayout.setEnabledRecursive(event.getValue()));

        propertyBuilder.add(
            () -> new AutoReloadState(reloadBox.getValueOrError(), reloadTypeGroup.getValueOrError()),
            state -> {
                reloadBox.setValue(state.isEnabled());
                reloadTypeGroup.setValue(state.value());
                buttonsLayout.setEnabledRecursive(state.isEnabled());
            },
            () -> {
                AutoReloadType autoReloadType = settings.getAutoReloadType();
                boolean isEnabled = autoReloadType != AutoReloadType.NONE;
                AutoReloadType value = isEnabled ? autoReloadType : getPreviousAutoReloadType(propertiesComponent);
                return new AutoReloadState(isEnabled, value);
            },
            state -> {
                settings.setAutoReloadType(state.isEnabled() ? state.value() : AutoReloadType.NONE);
                propertiesComponent.setValue(PREVIOUS_KEY, state.value().toString());
            }
        );

        return root;
    }

    private static AutoReloadType getPreviousAutoReloadType(PropertiesComponent propertiesComponent) {
        String previous = propertiesComponent.getValue(PREVIOUS_KEY);
        if (previous != null) {
            try {
                return AutoReloadType.valueOf(previous);
            }
            catch (IllegalArgumentException ignored) {
            }
        }
        return AutoReloadType.ALL;
    }

    @Override
    public String getId() {
        return "build.tools";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ExternalSystemLocalize.settingsBuildToolsDisplayName();
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }

    private record AutoReloadState(boolean isEnabled, AutoReloadType value) {
    }
}
