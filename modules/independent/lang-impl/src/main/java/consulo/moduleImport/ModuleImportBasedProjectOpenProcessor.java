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
package consulo.moduleImport;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.impl.util.NewProjectUtilPlatform;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.annotations.RequiredDispatchThread;
import org.jdom.JDOMException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
public class ModuleImportBasedProjectOpenProcessor<C extends ModuleImportContext> extends ProjectOpenProcessor {
  private ModuleImportProvider<C> myProvider;

  public ModuleImportBasedProjectOpenProcessor(ModuleImportProvider<C> provider) {
    myProvider = provider;
  }

  @Nonnull
  @Override
  public String getFileSample() {
    return myProvider.getFileSample();
  }

  @Nonnull
  @Override
  public Icon getIcon() {
    return myProvider.getIcon();
  }

  @Override
  public boolean canOpenProject(@Nonnull File file) {
    return myProvider.canImport(file);
  }

  @RequiredDispatchThread
  @Nullable
  @Override
  public Project doOpenProject(@Nonnull VirtualFile virtualFile, @Nullable Project projectToClose, boolean forceOpenInNewFrame) {
    String pathToBeImported = myProvider.getPathToBeImported(virtualFile);

    final String dotIdeaFilePath = pathToBeImported + File.separator + Project.DIRECTORY_STORE_FOLDER;

    File dotIdeaFile = new File(dotIdeaFilePath);

    String pathToOpen = dotIdeaFile.getParent();

    boolean shouldOpenExisting = false;
    if (!ApplicationManager.getApplication().isHeadlessEnvironment() && dotIdeaFile.exists()) {
      String existingName = "an existing project";

      int result =
              Messages.showYesNoCancelDialog(projectToClose, IdeBundle.message("project.import.open.existing", existingName, pathToOpen, virtualFile.getName()),
                                             IdeBundle.message("title.open.project"), IdeBundle.message("project.import.open.existing.openExisting"),
                                             IdeBundle.message("project.import.open.existing.reimport"), CommonBundle.message("button.cancel"),
                                             Messages.getQuestionIcon());
      if (result == Messages.CANCEL) return null;
      shouldOpenExisting = result == Messages.OK;
    }

    final Project projectToOpen;
    if (shouldOpenExisting) {
      try {
        projectToOpen = ProjectManagerEx.getInstanceEx().loadProject(pathToOpen);
      }
      catch (IOException e) {
        return null;
      }
      catch (JDOMException e) {
        return null;
      }
      catch (InvalidDataException e) {
        return null;
      }
    }
    else {
      final AddModuleWizard dialog = ImportModuleAction.createImportWizard(null, null, virtualFile, Collections.singletonList(myProvider));
      assert dialog != null;
      dialog.show();
      if (!dialog.isOK()) {
        NewProjectUtil.disposeContext(dialog);
        return null;
      }
      projectToOpen = NewProjectUtil.createFromWizard(dialog, null, false);
    }

    if (!forceOpenInNewFrame) {
      NewProjectUtilPlatform.closePreviousProject(projectToClose);
    }
    ProjectUtil.updateLastProjectLocation(pathToOpen);
    ProjectManagerEx.getInstanceEx().openProject(projectToOpen);

    return projectToOpen;
  }
}