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
package consulo.ide.impl.externalSystem.service.module.wizard;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.execution.ExternalSystemSettingsControl;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.ui.awt.ExternalSystemUiUtil;
import consulo.externalSystem.ui.awt.PaintAwarePanel;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.ex.wizard.WizardStepValidationException;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

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
public class SelectExternalProjectStep<C extends AbstractImportFromExternalSystemControl> implements WizardStep<ExternalModuleImportContext<C>> {
    private PaintAwarePanel myComponent;

    @Nonnull
    private AbstractImportFromExternalSystemControl myControl;

    @Nonnull
    private TextFieldWithBrowseButton myLinkedProjectPathField;

    @RequiredUIAccess
    @Nonnull
    @Override
    public Component getComponent(@Nonnull ExternalModuleImportContext<C> context, @Nonnull Disposable uiDisposable) {
        throw new UnsupportedOperationException("desktop only");
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public JComponent getSwingComponent(@Nonnull ExternalModuleImportContext<C> context, @Nonnull Disposable uiDisposable) {
        return createComponent(context, uiDisposable);
    }

    public JComponent createComponent(@Nonnull ExternalModuleImportContext<C> context, @Nonnull Disposable uiDisposable) {
        if (myComponent != null) {
            return myComponent;
        }

        myControl = context.getImportProvider().getControl();

        myComponent = new PaintAwarePanel(new GridBagLayout());
        AbstractExternalModuleImportProvider<C> provider = context.getImportProvider();

        ProjectSystemId externalSystemId = provider.getExternalSystemId();

        JLabel linkedProjectPathLabel =
            new JLabel(ExternalSystemLocalize.settingsLabelSelectProject(externalSystemId.getDisplayName()).get());
        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        assert manager != null;
        FileChooserDescriptor fileChooserDescriptor = manager.getExternalProjectDescriptor();
        myLinkedProjectPathField = new TextFieldWithBrowseButton();
        myLinkedProjectPathField.addBrowseFolderListener(
            LocalizeValue.empty(),
            ExternalSystemLocalize.settingsLabelSelectProject(externalSystemId.getDisplayName()),
            null,
            fileChooserDescriptor,
            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );
        myLinkedProjectPathField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                myControl.onLinkedProjectPathChange(myLinkedProjectPathField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                myControl.onLinkedProjectPathChange(myLinkedProjectPathField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                myControl.onLinkedProjectPathChange(myLinkedProjectPathField.getText());
            }
        });

        myComponent.add(linkedProjectPathLabel, ExternalSystemUiUtil.getLabelConstraints(0));
        myComponent.add(myLinkedProjectPathField, ExternalSystemUiUtil.getFillLineConstraints(0));

        ExternalSystemSettingsControl projectSettings = myControl.getProjectSettingsControl();
        projectSettings.fillUi(uiDisposable, myComponent, 0);

        ExternalSystemSettingsControl systemSettingsControl = myControl.getSystemSettingsControl();
        if (systemSettingsControl != null) {
            systemSettingsControl.fillUi(uiDisposable, myComponent, 0);
        }

        ExternalSystemUiUtil.fillBottom(myComponent);

        String path = context.getPath();

        myControl.getProjectSettings().setExternalProjectPath(path);
        myLinkedProjectPathField.setText(path);

        provider.doPrepare(context);

        projectSettings.reset();
        if (systemSettingsControl != null) {
            systemSettingsControl.reset();
        }
        return myComponent;
    }

    @Override
    public void validateStep(@Nonnull ExternalModuleImportContext<C> context) throws WizardStepValidationException {
        try {
            AbstractExternalModuleImportProvider<C> provider = context.getImportProvider();
            ProjectSystemId externalSystemId = provider.getExternalSystemId();
            Project project = context.getProject();

            String linkedProjectPath = myLinkedProjectPathField.getText();
            if (StringUtil.isEmpty(linkedProjectPath)) {
                throw new ConfigurationException(ExternalSystemLocalize.errorProjectUndefined());
            }
            else if (project != null) {
                ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
                assert manager != null;
                AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().apply(project);
                if (settings.getLinkedProjectSettings(linkedProjectPath) != null) {
                    throw new ConfigurationException(ExternalSystemLocalize.errorProjectAlreadyRegistered());
                }
            }

            myControl.apply(linkedProjectPath, project);
        }
        catch (ConfigurationException e) {
            throw new WizardStepValidationException(e.getMessage());
        }

        AbstractExternalModuleImportProvider<C> provider = context.getImportProvider();

        provider.ensureProjectIsDefined(context);
    }
}
