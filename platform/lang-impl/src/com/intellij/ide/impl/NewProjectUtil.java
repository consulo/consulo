/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
 * @author max
 */
package com.intellij.ide.impl;

import com.intellij.ide.impl.util.NewProjectUtilPlatform;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.util.ui.UIUtil;
import consulo.compiler.CompilerConfiguration;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class NewProjectUtil extends NewProjectUtilPlatform {
  public static final Logger LOGGER = Logger.getInstance(NewProjectUtil.class);

  private NewProjectUtil() {
  }

  public static void createNewProject(Project projectToClose, @Nullable final String defaultPath) {
    final boolean proceed = ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      @Override
      public void run() {
        ProjectManager.getInstance().getDefaultProject(); //warm up components
      }
    }, ProjectBundle.message("project.new.wizard.progress.title"), true, null);
    if (!proceed) return;
    final AddModuleWizard dialog = new AddModuleWizard(null, ModulesProvider.EMPTY_MODULES_PROVIDER, defaultPath);
    dialog.show();
    if (!dialog.isOK()) {
      return;
    }

    createFromWizard(dialog, projectToClose);
  }

  public static Project createFromWizard(AddModuleWizard dialog, Project projectToClose) {
    try {
      return doCreate(dialog, projectToClose);
    }
    catch (final IOException e) {
      UIUtil.invokeLaterIfNeeded(() -> Messages.showErrorDialog(e.getMessage(), "Project Initialization Failed"));
      return null;
    }
  }

  private static Project doCreate(final AddModuleWizard wizard, @Nullable Project projectToClose) throws IOException {
    final ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
    final String projectFilePath = wizard.getNewProjectFilePath();
    final ModuleImportProvider<?> importProvider = wizard.getImportProvider();

    ModuleImportContext importContext = importProvider == null ? null : wizard.getWizardContext().getModuleImportContext(importProvider);

    try {
      File projectDir = new File(projectFilePath).getParentFile();
      LOGGER.assertTrue(projectDir != null, "Cannot create project in '" + projectFilePath + "': no parent file exists");
      FileUtil.ensureExists(projectDir);
      final File ideaDir = new File(projectFilePath, Project.DIRECTORY_STORE_FOLDER);
      FileUtil.ensureExists(ideaDir);

      final Project newProject;
      if (importContext == null || !importContext.isUpdate()) {
        String name = wizard.getProjectName();
        newProject =
                importProvider == null ? projectManager.newProject(name, projectFilePath, true, false) : projectManager.createProject(name, projectFilePath);
      }
      else {
        newProject = projectToClose;
      }

      if (newProject == null) return projectToClose;


      final String compileOutput = wizard.getNewCompileOutput();
      CommandProcessor.getInstance().executeCommand(newProject, new Runnable() {
        @Override
        public void run() {
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
              String canonicalPath = compileOutput;
              try {
                canonicalPath = FileUtil.resolveShortWindowsName(compileOutput);
              }
              catch (IOException e) {
                //file doesn't exist
              }
              canonicalPath = FileUtil.toSystemIndependentName(canonicalPath);
              CompilerConfiguration.getInstance(newProject).setCompilerOutputUrl(VfsUtilCore.pathToUrl(canonicalPath));
            }
          });
        }
      }, null, null);

      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        newProject.save();
      }

      if (importProvider != null && !importProvider.validate(projectToClose, newProject)) {
        return projectToClose;
      }

      if (newProject != projectToClose && !ApplicationManager.getApplication().isUnitTestMode()) {
        closePreviousProject(projectToClose);
      }

      if (importProvider != null) {
        importProvider.commit(newProject, null, ModulesProvider.EMPTY_MODULES_PROVIDER, null);
      }

      final boolean need2OpenProjectStructure = importContext == null || importContext.isOpenProjectSettingsAfter();
      StartupManager.getInstance(newProject).registerPostStartupActivity(new Runnable() {
        @Override
        public void run() {
          // ensure the dialog is shown after all startup activities are done
          //noinspection SSBasedInspection
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              if (newProject.isDisposed() || ApplicationManager.getApplication().isUnitTestMode()) return;
              if (need2OpenProjectStructure) {
                ModulesConfigurator.showDialog(newProject, null, null);
              }
              ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                  if (newProject.isDisposed()) return;
                  final ToolWindow toolWindow = ToolWindowManager.getInstance(newProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
                  if (toolWindow != null) {
                    toolWindow.activate(null);
                  }
                }
              }, ModalityState.NON_MODAL);
            }
          });
        }
      });

      if (newProject != projectToClose) {
        ProjectUtil.updateLastProjectLocation(projectFilePath);

        if (WindowManager.getInstance().isFullScreenSupportedInCurrentOS()) {
          IdeFocusManager instance = IdeFocusManager.findInstance();
          IdeFrame lastFocusedFrame = instance.getLastFocusedFrame();
          if (lastFocusedFrame instanceof IdeFrameEx) {
            boolean fullScreen = ((IdeFrameEx)lastFocusedFrame).isInFullScreen();
            if (fullScreen) {
              newProject.putUserData(IdeFrameImpl.SHOULD_OPEN_IN_FULL_SCREEN, Boolean.TRUE);
            }
          }
        }

        projectManager.openProject(newProject);
      }
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        newProject.save();
      }
      return newProject;
    }
    finally {
      Disposer.dispose(wizard.getWizardContext());
    }
  }
}
