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

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import consulo.start.WelcomeFrameManager;
import consulo.ui.Size;
import consulo.ui.wizard.WizardBasedDialog;
import consulo.ui.wizard.WizardSession;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-08-26
 */
public class ModuleImportDialog<C extends ModuleImportContext> extends WizardBasedDialog<C> {

  protected ModuleImportDialog(@Nullable Project project, @Nonnull VirtualFile targetFile, @Nonnull ModuleImportProvider<C> moduleImportProvider) {
    super(project);

    myWizardContext = moduleImportProvider.createContext(project);

    String pathToImport = moduleImportProvider.getPathToBeImported(targetFile);
    myWizardContext.setPath(pathToImport);
    myWizardContext.setName(new File(pathToImport).getName());
    myWizardContext.setFileToImport(targetFile.getPath());

    List<WizardStep<C>> steps = new ArrayList<>();
    moduleImportProvider.buildSteps(steps::add, myWizardContext);

    myWizardSession = new WizardSession<>(myWizardContext, steps);

    if (!myWizardSession.hasNext()) {
      throw new IllegalArgumentException("no steps for show");
    }

    setTitle("Import from " + moduleImportProvider.getName());
    setOKButtonText(IdeBundle.message("button.create"));

    Size size = WelcomeFrameManager.getDefaultWindowSize();
    setScalableSize(size.getWidth(), size.getHeight());

    init();
  }
}
