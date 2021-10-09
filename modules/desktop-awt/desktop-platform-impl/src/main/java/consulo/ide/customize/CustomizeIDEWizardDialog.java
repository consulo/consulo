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
package consulo.ide.customize;

import consulo.desktop.startup.customize.CustomizeAuthOrScratchStep;
import com.intellij.ide.customize.CustomizeKeyboardSchemeStepPanel;
import com.intellij.ide.customize.CustomizeUIThemeStepPanel;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ui.JBUI;
import consulo.container.plugin.PluginDescriptor;
import consulo.desktop.startup.customize.CustomizeDownloadStep;
import consulo.desktop.startup.customize.CustomizeFinishedLastStep;
import consulo.desktop.startup.customize.CustomizePluginTemplatesStepPanel;
import consulo.desktop.startup.customize.PluginTemplate;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.wizard.WizardBasedDialog;
import consulo.ui.wizard.WizardSession;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 22/09/2021
 */
public class CustomizeIDEWizardDialog extends WizardBasedDialog<CustomizeWizardContext> {

  public CustomizeIDEWizardDialog(List<PluginDescriptor> pluginDescriptors, Map<String, PluginTemplate> predefinedTemplateSets) {
    super(null);

    myWizardContext = new CustomizeWizardContext(pluginDescriptors, predefinedTemplateSets);

    List<WizardStep<CustomizeWizardContext>> steps = new ArrayList<>();
    steps.add(new CustomizeAuthOrScratchStep(this::doOKAction, myWizardContext));
    steps.add(new CustomizeUIThemeStepPanel());

    if (SystemInfo.isMac) {
      steps.add(new CustomizeKeyboardSchemeStepPanel());
    }

    steps.add(new CustomizePluginTemplatesStepPanel(myWizardContext));
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
