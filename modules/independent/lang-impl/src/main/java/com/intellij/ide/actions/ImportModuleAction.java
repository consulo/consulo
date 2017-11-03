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
package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotations.RequiredDispatchThread;
import consulo.annotations.RequiredReadAction;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import consulo.moduleImport.ModuleImportProviders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 *         Date: 10/31/12
 */
public class ImportModuleAction extends AnAction {

  private static final String LAST_IMPORTED_LOCATION = "last.imported.location";

  @RequiredDispatchThread
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    doImport(canCreateNewProject() ? null : e.getProject());
  }

  @RequiredReadAction
  public static List<Module> doImport(Project project) {
    AddModuleWizard wizard = selectFileAndCreateWizard(project, null);
    if (wizard == null) {
      return Collections.emptyList();
    }
    if (wizard.getStepCount() > 0 && !wizard.showAndGet()) {
      NewProjectUtil.disposeContext(wizard);
      return Collections.emptyList();
    }

    return createFromWizard(project, wizard);
  }

  @NotNull
  @RequiredReadAction
  @SuppressWarnings("unchecked")
  public static List<Module> createFromWizard(Project project, @NotNull AddModuleWizard wizard) {
    if (project == null && wizard.getStepCount() > 0) {
      Project newProject = NewProjectUtil.createFromWizard(wizard, null);
      return newProject == null ? Collections.emptyList() : Arrays.asList(ModuleManager.getInstance(newProject).getModules());
    }

    WizardContext wizardContext = wizard.getWizardContext();

    ModuleImportProvider importProvider = wizard.getImportProvider();

    try {
      if (wizard.getStepCount() > 0) {
        Module module = NewModuleAction.createModuleFromWizard(project, null, wizard);
        return Collections.singletonList(module);
      }
      else {
        ModuleImportContext moduleImportContext = wizardContext.getModuleImportContext(importProvider);
        return importProvider.commit(moduleImportContext, project);
      }
    }
    finally {
      NewProjectUtil.disposeContext(wizard);
    }
  }

  @Nullable
  public static AddModuleWizard selectFileAndCreateWizard(final Project project, Component dialogParent) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, true, false, false) {
      FileChooserDescriptor myDelegate = new OpenProjectFileChooserDescriptor(true);

      @Override
      public Icon getIcon(VirtualFile file) {
        for (ModuleImportProvider importProvider : ModuleImportProviders.getExtensions(true)) {
          if (importProvider.canImport(VfsUtilCore.virtualToIoFile(file))) {
            return importProvider.getIcon();
          }
        }
        Icon icon = myDelegate.getIcon(file);
        return icon == null ? super.getIcon(file) : icon;
      }
    };
    descriptor.setHideIgnored(false);
    descriptor.setTitle("Select File or Directory to Import");
    List<ModuleImportProvider> providers = ModuleImportProviders.getExtensions(true);
    String description = getFileChooserDescription(project);
    descriptor.setDescription(description);

    return selectFileAndCreateWizard(project, dialogParent, descriptor, providers);
  }

  @Nullable
  public static AddModuleWizard selectFileAndCreateWizard(final Project project,
                                                          @Nullable Component dialogParent,
                                                          @NotNull FileChooserDescriptor descriptor,
                                                          @NotNull List<ModuleImportProvider> providers) {
    FileChooserDialog chooser = FileChooserFactory.getInstance().createFileChooser(descriptor, project, dialogParent);
    VirtualFile toSelect = null;
    String lastLocation = PropertiesComponent.getInstance().getValue(LAST_IMPORTED_LOCATION);
    if (lastLocation != null) {
      toSelect = LocalFileSystem.getInstance().refreshAndFindFileByPath(lastLocation);
    }
    VirtualFile[] files = chooser.choose(project, toSelect == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{toSelect});
    if (files.length == 0) {
      return null;
    }

    final VirtualFile file = files[0];
    PropertiesComponent.getInstance().setValue(LAST_IMPORTED_LOCATION, file.getPath());
    return createImportWizard(project, dialogParent, file, providers);
  }

  public static String getFileChooserDescription(final Project project) {
    List<ModuleImportProvider> providers = ModuleImportProviders.getExtensions(true);

    return IdeBundle.message("import.project.chooser.header", StringUtil.join(providers, ModuleImportProvider::getFileSample, ", <br>"));
  }

  @Nullable
  public static AddModuleWizard createImportWizard(final Project project,
                                                   @Nullable Component dialogParent,
                                                   final VirtualFile file,
                                                   List<ModuleImportProvider> providers) {
    File ioFile = VfsUtilCore.virtualToIoFile(file);
    List<ModuleImportProvider> available = ContainerUtil.filter(providers, provider -> provider.canImport(ioFile));
    if (available.isEmpty()) {
      Messages.showErrorDialog(project, "Cannot import anything from '" + FileUtil.toSystemDependentName(file.getPath()) + "'", "Cannot Import");
      return null;
    }

    String path;
    if (available.size() == 1) {
      path = available.get(0).getPathToBeImported(file);
    }
    else {
      path = ModuleImportProvider.getDefaultPath(file);
    }

    ModuleImportProvider[] availableProviders = available.toArray(new ModuleImportProvider[available.size()]);

    return dialogParent == null ? new AddModuleWizard(project, path, availableProviders) : new AddModuleWizard(project, dialogParent, path, availableProviders);
  }

  @RequiredDispatchThread
  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();

    if (!canCreateNewProject() && e.getProject() == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    presentation.setEnabledAndVisible(!ModuleImportProviders.getExtensions(true).isEmpty());
  }

  public boolean canCreateNewProject() {
    return false;
  }

  @Override
  public boolean isDumbAware() {
    return true;
  }
}
