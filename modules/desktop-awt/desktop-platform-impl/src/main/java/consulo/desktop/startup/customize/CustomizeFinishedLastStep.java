/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.startup.customize;

import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.disposer.Disposable;
import consulo.ide.customize.CustomizeWizardContext;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 09/10/2021
 */
public class CustomizeFinishedLastStep implements WizardStep<CustomizeWizardContext> {
  private final Runnable myNextAction;

  public CustomizeFinishedLastStep(Runnable nextAction) {
    myNextAction = nextAction;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component getComponent(@Nonnull Disposable uiDisposable) {
    throw new UnsupportedOperationException("unsupported ui");
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public java.awt.Component getSwingComponent(@Nonnull Disposable uiDisposable) {
    JLabel label = new JLabel("All up. Press Start for using Consulo");
    // TODO change button to Start
    return new BorderLayoutPanel().addToCenter(label);
  }
}
