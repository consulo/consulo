/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 12-Jul-2007
 */
package com.intellij.projectImport;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.impl.util.NewProjectUtilPlatform;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import consulo.annotations.RequiredDispatchThread;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@Deprecated
public abstract class ProjectOpenProcessorBase<T extends ProjectImportBuilder> extends ProjectOpenProcessor {

  private final T myBuilder;

  protected ProjectOpenProcessorBase(@NotNull final T builder) {
    myBuilder = builder;
  }

  @Override
  public String getName() {
    return getBuilder().getName();
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return getBuilder().getIcon();
  }

  @Override
  public boolean canOpenProject(@NotNull final VirtualFile file) {
    final String[] supported = getSupportedExtensions();
    if (supported != null) {
      if (file.isDirectory()) {
        for (VirtualFile child : getFileChildren(file)) {
          if (canOpenFile(child, supported)) return true;
        }
        return false;
      }
      if (canOpenFile(file, supported)) return true;
    }
    return false;
  }

  private static Collection<VirtualFile> getFileChildren(VirtualFile file) {
    if (file instanceof NewVirtualFile) {
      return ((NewVirtualFile)file).getCachedChildren();
    }

    return Arrays.asList(file.getChildren());
  }

  protected static boolean canOpenFile(VirtualFile file, String[] supported) {
    final String fileName = file.getName();
    for (String name : supported) {
      if (fileName.equals(name)) {
        return true;
      }
    }
    return false;
  }

  protected boolean doQuickImport(VirtualFile file, final WizardContext wizardContext) {
    return false;
  }

  @NotNull
  public T getBuilder() {
    return myBuilder;
  }

  @Nullable
  public abstract String[] getSupportedExtensions();

  @RequiredDispatchThread
  @Override
  @Nullable
  public Project doOpenProject(@NotNull VirtualFile virtualFile, Project projectToClose, boolean forceOpenInNewFrame) {
    try {
      getBuilder().setUpdate(false);
      final WizardContext wizardContext = new WizardContext(null);
      if (virtualFile.isDirectory()) {
        final String[] supported = getSupportedExtensions();
        for (VirtualFile file : getFileChildren(virtualFile)) {
          if (canOpenFile(file, supported)) {
            virtualFile = file;
            break;
          }
        }
      }

      wizardContext.setProjectFileDirectory(virtualFile.getParent().getPath());

      if (!doQuickImport(virtualFile, wizardContext)) return null;

      if (wizardContext.getProjectName() == null) {
        wizardContext.setProjectName(IdeBundle.message("project.import.default.name.dotIdea", getName()));
      }

      final String dotIdeaFilePath = wizardContext.getProjectFileDirectory() + File.separator + Project.DIRECTORY_STORE_FOLDER;

      File dotIdeaFile = new File(dotIdeaFilePath);

      String pathToOpen = dotIdeaFile.getParent();

      boolean shouldOpenExisting = false;
      if (!ApplicationManager.getApplication().isHeadlessEnvironment() && dotIdeaFile.exists()) {
        String existingName = "an existing project";

        int result = Messages.showYesNoCancelDialog(projectToClose,
                                                    IdeBundle.message("project.import.open.existing",
                                                                      existingName,
                                                                      pathToOpen,
                                                                      virtualFile.getName()),
                                                    IdeBundle.message("title.open.project"),
                                                    IdeBundle.message("project.import.open.existing.openExisting"),
                                                    IdeBundle.message("project.import.open.existing.reimport"),
                                                    CommonBundle.message("button.cancel"),
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
        projectToOpen = ProjectManagerEx.getInstanceEx().newProject(wizardContext.getProjectName(), pathToOpen, true, false);

        if (projectToOpen == null || !getBuilder().validate(projectToClose, projectToOpen)) {
          return null;
        }

        projectToOpen.save();

        getBuilder().commit(projectToOpen, null, ModulesProvider.EMPTY_MODULES_PROVIDER);
      }

      if (!forceOpenInNewFrame) {
        NewProjectUtilPlatform.closePreviousProject(projectToClose);
      }
      ProjectUtil.updateLastProjectLocation(pathToOpen);
      ProjectManagerEx.getInstanceEx().openProject(projectToOpen);

      return projectToOpen;
    }
    finally {
      getBuilder().cleanup();
    }
  }

  public static String getUrl(@NonNls String path) {
    try {
      path = FileUtil.resolveShortWindowsName(path);
    }
    catch (IOException e) {
      //file doesn't exist
    }
    return VfsUtil.pathToUrl(FileUtil.toSystemIndependentName(path));
  }
}
