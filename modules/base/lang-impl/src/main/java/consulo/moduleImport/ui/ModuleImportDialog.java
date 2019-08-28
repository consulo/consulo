/*
 * Copyright 2013-2019 consulo.io
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
package consulo.moduleImport.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBCardLayout;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import consulo.ui.RequiredUIAccess;
import consulo.ui.wizard.WizardSession;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class ModuleImportDialog<C extends ModuleImportContext> extends DialogWrapper {
  @Nonnull
  private final ModuleImportProvider myModuleImportProvider;
  @Nonnull
  private JPanel myContentPanel;

  private final WizardSession<C> myWizardSession;

  protected ModuleImportDialog(@Nullable Project project, @Nonnull VirtualFile targetFile, @Nonnull ModuleImportProvider<C> moduleImportProvider) {
    super(project);
    myModuleImportProvider = moduleImportProvider;

    C context = moduleImportProvider.createContext();

    String defaultPath = ModuleImportProvider.getDefaultPath(targetFile);
    context.setPath(defaultPath);
    context.setName(new File(defaultPath).getName());
    context.setFileToImport(targetFile.getPath());

    List<WizardStep<C>> steps = new ArrayList<>();
    moduleImportProvider.buildSteps(steps::add, context);

    myWizardSession = new WizardSession<>(context, steps);

    if (!myWizardSession.hasNext()) {
      throw new IllegalArgumentException("no steps for show");
    }

    setTitle("Import from " + moduleImportProvider.getName());

    init();
    pack();
  }

  @Nullable
  @Override
  @RequiredUIAccess
  protected JComponent createCenterPanel() {
    JBCardLayout layout = new JBCardLayout();
    myContentPanel = new JPanel(layout);

    WizardStep<C> first = myWizardSession.next();

    int currentStepIndex = myWizardSession.getCurrentStepIndex();

    String id = "step-" + currentStepIndex;
    myContentPanel.add(first.getSwingComponent(), id);

    layout.show(myContentPanel, id);
    return myContentPanel;
  }

  @Override
  protected void dispose() {
    myWizardSession.dispose();

    super.dispose();
  }
}
