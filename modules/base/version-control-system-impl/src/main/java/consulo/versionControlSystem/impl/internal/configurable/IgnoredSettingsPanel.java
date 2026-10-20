// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.configurable.SearchableConfigurable;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.versionControlSystem.VcsApplicationSettings;
import consulo.versionControlSystem.localize.VcsLocalize;

import static consulo.versionControlSystem.impl.internal.change.ignore.IgnoreConfigurationProperty.ASKED_MANAGE_IGNORE_FILES_PROPERTY;
import static consulo.versionControlSystem.impl.internal.change.ignore.IgnoreConfigurationProperty.MANAGE_IGNORE_FILES_PROPERTY;

public class IgnoredSettingsPanel extends SimpleConfigurableByProperties implements SearchableConfigurable {
    private final Project myProject;

    public IgnoredSettingsPanel(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        VcsApplicationSettings settings = VcsApplicationSettings.getInstance();

        VerticalLayout general = VerticalLayout.create();

        ComboBox.Builder<ManageIgnoredOption> builder = ComboBox.builder();
        for (ManageIgnoredOption option : ManageIgnoredOption.values()) {
            builder.add(option, option.getDisplayName());
        }
        ComboBox<ManageIgnoredOption> policyBox = builder.build();
        propertyBuilder.add(policyBox, this::getIgnoredOption, this::updateIgnoredOption);
        general.add(LabeledBuilder.sided(VcsLocalize.ignoredFileManagePolicyLabel(), policyBox));

        CheckBox generateInVcsRootBox =
            CheckBox.create(VcsLocalize.ignoredFileGenerateInVcsRootLabel(), settings.GENERATE_IGNORE_FILES_IN_VCS_ROOTS);
        propertyBuilder.add(
            generateInVcsRootBox,
            () -> settings.GENERATE_IGNORE_FILES_IN_VCS_ROOTS,
            value -> settings.GENERATE_IGNORE_FILES_IN_VCS_ROOTS = value
        );
        general.add(generateInVcsRootBox);

        VerticalLayout root = VerticalLayout.create();
        root.add(LabeledLayout.create(VcsLocalize.ignoredFileGeneralSettingsTitle(), general));
        return root;
    }

    private void updateIgnoredOption(ManageIgnoredOption option) {
        VcsApplicationSettings applicationSettings = VcsApplicationSettings.getInstance();
        ProjectPropertiesComponent propertiesComponent = ProjectPropertiesComponent.getInstance(myProject);
        switch (option) {
            case DoNotManageForAllProject -> {
                propertiesComponent.setValue(MANAGE_IGNORE_FILES_PROPERTY, false);
                propertiesComponent.setValue(ASKED_MANAGE_IGNORE_FILES_PROPERTY, true);
                applicationSettings.MANAGE_IGNORE_FILES = false;
                applicationSettings.DISABLE_MANAGE_IGNORE_FILES = true;
            }
            case AllProjectsManage -> {
                propertiesComponent.setValue(MANAGE_IGNORE_FILES_PROPERTY, true);
                propertiesComponent.setValue(ASKED_MANAGE_IGNORE_FILES_PROPERTY, true);
                applicationSettings.MANAGE_IGNORE_FILES = true;
                applicationSettings.DISABLE_MANAGE_IGNORE_FILES = false;
            }
            case CurrentProjectManage -> {
                propertiesComponent.setValue(MANAGE_IGNORE_FILES_PROPERTY, true);
                propertiesComponent.setValue(ASKED_MANAGE_IGNORE_FILES_PROPERTY, true);
                applicationSettings.MANAGE_IGNORE_FILES = false;
                applicationSettings.DISABLE_MANAGE_IGNORE_FILES = false;
            }
            case DoNotManageForCurrentProject -> {
                propertiesComponent.setValue(MANAGE_IGNORE_FILES_PROPERTY, false);
                propertiesComponent.setValue(ASKED_MANAGE_IGNORE_FILES_PROPERTY, true);
                applicationSettings.MANAGE_IGNORE_FILES = false;
                applicationSettings.DISABLE_MANAGE_IGNORE_FILES = false;
            }
            case AlwaysAsk -> {
                propertiesComponent.setValue(MANAGE_IGNORE_FILES_PROPERTY, false);
                propertiesComponent.setValue(ASKED_MANAGE_IGNORE_FILES_PROPERTY, false);
                applicationSettings.MANAGE_IGNORE_FILES = false;
                applicationSettings.DISABLE_MANAGE_IGNORE_FILES = false;
            }
        }
    }

    private ManageIgnoredOption getIgnoredOption() {
        VcsApplicationSettings applicationSettings = VcsApplicationSettings.getInstance();
        ProjectPropertiesComponent propertiesComponent = ProjectPropertiesComponent.getInstance(myProject);
        if (applicationSettings.DISABLE_MANAGE_IGNORE_FILES) {
            return ManageIgnoredOption.DoNotManageForAllProject;
        }
        if (applicationSettings.MANAGE_IGNORE_FILES) {
            return ManageIgnoredOption.AllProjectsManage;
        }
        if (propertiesComponent.getBoolean(MANAGE_IGNORE_FILES_PROPERTY, false)) {
            return ManageIgnoredOption.CurrentProjectManage;
        }
        if (propertiesComponent.getBoolean(ASKED_MANAGE_IGNORE_FILES_PROPERTY, false)) {
            return ManageIgnoredOption.DoNotManageForCurrentProject;
        }
        return ManageIgnoredOption.AlwaysAsk;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return VcsLocalize.ignoredFileTabTitle();
    }

    @Override
    public String getId() {
        return "project.propVCSSupport.Ignored.Files";
    }
}
