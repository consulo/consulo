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
package consulo.externalSystem.service.module.wizard;

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.*;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.wizard.WizardStep;
import consulo.ui.wizard.WizardStepValidationException;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
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
 * @since 8/1/11 4:15 PM
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

    JLabel linkedProjectPathLabel = new JLabel(ExternalSystemBundle.message("settings.label.select.project", externalSystemId.getReadableName()));
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    FileChooserDescriptor fileChooserDescriptor = manager.getExternalProjectDescriptor();
    myLinkedProjectPathField = new TextFieldWithBrowseButton();
    myLinkedProjectPathField.addBrowseFolderListener("", ExternalSystemBundle.message("settings.label.select.project", externalSystemId.getReadableName()), null, fileChooserDescriptor,
                                                     TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
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
        throw new ConfigurationException(ExternalSystemBundle.message("error.project.undefined"));
      }
      else if (project != null) {
        ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        assert manager != null;
        AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().fun(project);
        if (settings.getLinkedProjectSettings(linkedProjectPath) != null) {
          throw new ConfigurationException(ExternalSystemBundle.message("error.project.already.registered"));
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
