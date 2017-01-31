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

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractImportFromExternalSystemWizardStep;
import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
public class SelectExternalProjectStep extends AbstractImportFromExternalSystemWizardStep {

  private final JPanel myComponent = new JPanel(new BorderLayout());

  @NotNull
  private AbstractImportFromExternalSystemControl myControl;

  private boolean myGradleSettingsInitialised;

  public SelectExternalProjectStep(@NotNull WizardContext context) {
    super(context);
  }

  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  @Override
  public void updateStep(WizardContext wizardContext) {
    if (!myGradleSettingsInitialised) {
      initExternalProjectSettingsControl();
    }
  }

  @Override
  public void updateDataModel() {
  }

  // TODO den uncomment
  //@Override
  //public String getHelpId() {
  //  return GradleConstants.HELP_TOPIC_IMPORT_SELECT_PROJECT_STEP;
  //}

  @Override
  public boolean validate(@NotNull WizardContext wizardContext) throws ConfigurationException {
    myControl.apply();
    AbstractExternalModuleImportProvider<?> provider = (AbstractExternalModuleImportProvider<?>)getImportProvider();
    if (provider == null) {
      return false;
    }

    provider.ensureProjectIsDefined(getWizardContext());
    return true;
  }

  private void initExternalProjectSettingsControl() {
    AbstractExternalModuleImportProvider<?> provider = (AbstractExternalModuleImportProvider<?>)getImportProvider();
    if (provider == null) {
      return;
    }
    provider.prepare(getWizardContext());
    myControl = provider.getControl(getWizardContext().getProject());
    myComponent.add(myControl.getComponent());
    myGradleSettingsInitialised = true;
  }
}
