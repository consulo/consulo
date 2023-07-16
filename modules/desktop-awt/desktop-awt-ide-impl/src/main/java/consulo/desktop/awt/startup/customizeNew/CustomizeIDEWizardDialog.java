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
package consulo.desktop.awt.startup.customizeNew;

import consulo.application.Application;
import consulo.application.util.SystemInfo;
import consulo.externalService.update.UpdateChannel;
import consulo.ide.impl.idea.ide.startup.StartupActionScriptManager;
import consulo.ide.impl.startup.customize.CustomizeWizardContext;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.wizard.WizardBasedDialog;
import consulo.ui.ex.wizard.WizardSession;
import consulo.ui.ex.wizard.WizardStep;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 22/09/2021
 */
public class CustomizeIDEWizardDialog extends WizardBasedDialog<CustomizeWizardContext> {

  public CustomizeIDEWizardDialog(boolean isDark, @Nullable UpdateChannel updateChannel) {
    super(null, false);

    myWizardContext = new CustomizeWizardContext(isDark, updateChannel);

    List<WizardStep<CustomizeWizardContext>> steps = new ArrayList<>();
    steps.add(new CustomizePreparingDataStep(this::doOKAction));
    steps.add(new CustomizeAuthOrScratchStep(this::doOKAction));
    steps.add(new CustomizeUIThemeStepPanel());

    if (SystemInfo.isMac) {
      steps.add(new CustomizeKeyboardSchemeStep());
    }

    steps.add(new CustomizePluginTemplatesStep());
    steps.add(new CustomizePluginsStepPanel());
    steps.add(new CustomizeDownloadStep(this::doOKAction));
    steps.add(new CustomizeFinishedLastStep(this::doOKAction));

    myWizardSession = new WizardSession<>(myWizardContext, steps);

    setTitle("Customize " + Application.get().getName());
    setScalableSize(800, 550);
    init();
    System.setProperty(StartupActionScriptManager.STARTUP_WIZARD_MODE, "true");
  }

  @RequiredUIAccess
  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JComponent panel = super.createCenterPanel();
    assert panel != null;
    panel.setPreferredSize(JBUI.size(800, 550));
    return panel;
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    return null;
  }

  @Override
  protected void dispose() {
    System.clearProperty(StartupActionScriptManager.STARTUP_WIZARD_MODE);
    super.dispose();
  }

  @Nullable
  @Override
  protected ActionListener createCancelAction() {
    return null;//Prevent closing by <Esc>
  }
}