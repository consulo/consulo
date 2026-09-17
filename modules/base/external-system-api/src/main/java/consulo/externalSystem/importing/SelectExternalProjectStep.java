/*
 * Copyright 2013-2017 consulo.io
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
package consulo.externalSystem.importing;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.setting.AbstractExternalProjectSettingsConfigurable;
import consulo.externalSystem.service.setting.ExternalSystemSettingsConfigurable;
import consulo.externalSystem.service.setting.ExternalSystemSettingsConfigurableFactory;
import consulo.externalSystem.service.setting.ExternalSystemSettingsPlace;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.ex.wizard.WizardStepValidationException;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * Handles the following responsibilities:
 * <pre>
 * <ul>
 *   <li>allows end user to define external system config file to import from;</li>
 *   <li>processes the input and reacts accordingly - shows error message if the project is invalid or proceeds to the next screen;</li>
 * </ul>
 * </pre>
 *
 * @author Denis Zhdanov
 * @author VISTALL
 * @since 2011-08-01
 */
public class SelectExternalProjectStep implements WizardStep<ExternalModuleImportContext> {
    private @Nullable VerticalLayout myComponent;

    private FileChooserTextBoxBuilder.@Nullable Controller myLinkedProjectPathBox;

    private @Nullable AbstractExternalProjectSettingsConfigurable<?> myProjectSettingsConfigurable;

    private @Nullable ExternalSystemSettingsConfigurable<?> mySystemSettingsConfigurable;

    @RequiredUIAccess
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Component getComponent(ExternalModuleImportContext context, Disposable uiDisposable) {
        if (myComponent != null) {
            return myComponent;
        }

        AbstractExternalModuleImportProvider provider = context.getImportProvider();
        ExternalSystemSettingsConfigurableFactory factory = context.getConfigurableFactory();

        ProjectSystemId externalSystemId = provider.getExternalSystemId();
        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        assert manager != null;

        LocalizeValue selectProjectTitle = ExternalSystemLocalize.settingsLabelSelectProject(externalSystemId.getDisplayName());

        FileChooserTextBoxBuilder.Controller linkedProjectPathBox = FileChooserTextBoxBuilder.create(context.getProject())
            .uiDisposable(uiDisposable)
            .dialogTitle(selectProjectTitle)
            .fileChooserDescriptor(manager.getExternalProjectDescriptor())
            .build();
        myLinkedProjectPathBox = linkedProjectPathBox;

        String path = context.getPath();
        context.getProjectSettings().setExternalProjectPath(path);
        linkedProjectPathBox.setValue(StringUtil.notNullize(path));

        provider.doPrepare(context);

        AbstractExternalProjectSettingsConfigurable<?> projectSettingsConfigurable =
            factory.createProjectSettingsConfigurable(context.getProjectSettings(), ExternalSystemSettingsPlace.IMPORT);
        myProjectSettingsConfigurable = projectSettingsConfigurable;

        linkedProjectPathBox.getComponent().addValueListener(
            event -> projectSettingsConfigurable.onLinkedProjectPathChange(StringUtil.notNullize(event.getValue()))
        );

        VerticalLayout layout = VerticalLayout.create();
        layout.add(LabeledBuilder.filled(selectProjectTitle, linkedProjectPathBox));
        layout.add(projectSettingsConfigurable.createUIComponent(uiDisposable));

        ExternalSystemSettingsConfigurable<?> systemSettingsConfigurable =
            factory.createSystemSettingsConfigurable(context.getSystemSettings(), ExternalSystemSettingsPlace.IMPORT);
        mySystemSettingsConfigurable = systemSettingsConfigurable;
        if (systemSettingsConfigurable != null) {
            layout.add(systemSettingsConfigurable.createUIComponent(uiDisposable));
        }

        myComponent = layout;
        return myComponent;
    }

    @RequiredUIAccess
    @Override
    public void validateStep(ExternalModuleImportContext context) throws WizardStepValidationException {
        FileChooserTextBoxBuilder.Controller linkedProjectPathBox = myLinkedProjectPathBox;
        if (linkedProjectPathBox == null) {
            return;
        }

        try {
            context.applyLinkedProjectPath(linkedProjectPathBox.getValue());

            if (myProjectSettingsConfigurable != null) {
                myProjectSettingsConfigurable.apply();
            }
            if (mySystemSettingsConfigurable != null) {
                mySystemSettingsConfigurable.apply();
            }
        }
        catch (ConfigurationException e) {
            throw new WizardStepValidationException(e.getMessage());
        }

        context.getImportProvider().ensureProjectIsDefined(context);
    }
}
