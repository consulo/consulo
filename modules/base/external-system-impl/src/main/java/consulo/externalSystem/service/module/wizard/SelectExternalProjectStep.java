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

import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.options.ConfigurationException;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.wizard.WizardStep;
import consulo.ui.wizard.WizardStepValidationException;

import javax.annotation.Nonnull;
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
public class SelectExternalProjectStep<C extends AbstractImportFromExternalSystemControl> implements WizardStep<ExternalModuleImportContext<C>> {

  private final JPanel myComponent = new JPanel(new BorderLayout());

  @Nonnull
  private AbstractImportFromExternalSystemControl myControl;

  private boolean myGradleSettingsInitialised;

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component getComponent() {
    throw new UnsupportedOperationException("desktop only");
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public JComponent getSwingComponent() {
    return myComponent;
  }

  @Override
  public void onStepEnter(@Nonnull ExternalModuleImportContext<C> context) {
    if (!myGradleSettingsInitialised) {
      initExternalProjectSettingsControl(context);
    }
  }

  @Override
  public void validateStep(@Nonnull ExternalModuleImportContext<C> context) throws WizardStepValidationException {
    try {
      myControl.apply();
    }
    catch (ConfigurationException e) {
      throw new WizardStepValidationException(e.getMessage());
    }

    AbstractExternalModuleImportProvider<C> provider = context.getImportProvider();

    provider.ensureProjectIsDefined(context);
  }

  private void initExternalProjectSettingsControl(ExternalModuleImportContext<C> context) {
    AbstractExternalModuleImportProvider<C> provider = context.getImportProvider();

    provider.prepare(context);
    myControl = provider.getControl(context.getProject());
    myComponent.add(myControl.getComponent());
    myGradleSettingsInitialised = true;
  }
}
