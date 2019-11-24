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

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeView;
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil;
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Trinity;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import consulo.awt.TargetAWT;
import consulo.module.extension.ModuleExtension;
import consulo.psi.PsiPackageSupportProvider;
import consulo.roots.ContentFolderTypeProvider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.TextBox;
import consulo.ui.ValidableComponent;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class CreateDirectoryOrPackageAction extends AnAction implements DumbAware {
  private enum ChildType {
    Directory {
      @Override
      public String getName() {
        return IdeBundle.message("action.directory");
      }

      @Override
      public String getSeparator() {
        return "\\/";
      }
    },
    Package {
      @Override
      public String getName() {
        return IdeBundle.message("action.package");
      }

      @Override
      public String getSeparator() {
        return ".";
      }
    };

    public abstract String getName();

    public abstract String getSeparator();
  }

  public CreateDirectoryOrPackageAction() {
    super(IdeBundle.message("action.create.new.directory.or.package"), IdeBundle.message("action.create.new.directory.or.package"), null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    Project project = e.getData(CommonDataKeys.PROJECT);

    if (view == null || project == null) {
      return;
    }

    PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(view);

    if (directory == null) {
      return;
    }

    Trinity<ContentFolderTypeProvider, PsiDirectory, ChildType> info = getInfo(directory);

    boolean isDirectory = info.getThird() == ChildType.Directory;

    CreateDirectoryOrPackageHandler validator = new CreateDirectoryOrPackageHandler(project, directory, isDirectory, info.getThird().getSeparator());

    String title = isDirectory ? IdeBundle.message("title.new.directory") : IdeBundle.message("title.new.package");

    createLightWeightPopup(validator, title, element -> {
      if (element != null) {
        view.selectElement(element);
      }
    }).showCenteredInCurrentWindow(project);
  }

  private JBPopup createLightWeightPopup(CreateDirectoryOrPackageHandler validator, String title, Consumer<PsiElement> consumer) {
    NewItemSimplePopupPanel contentPanel = new NewItemSimplePopupPanel();
    TextBox nameField = contentPanel.getTextField();
    JBPopup popup = NewItemPopupUtil.createNewItemPopup(title, contentPanel, (JComponent)TargetAWT.to(nameField));
    contentPanel.addValidator(value -> {
      if (!validator.checkInput(value)) {
        String message = InputValidatorEx.getErrorText(validator, value, LangBundle.message("incorrect.name"));
        return new ValidableComponent.ValidationInfo(message);
      }

      return null;
    });

    contentPanel.setApplyAction(event -> {
      String name = nameField.getValue();
      validator.canClose(name);

      popup.closeOk(event);
      consumer.accept(validator.getCreatedElement());
    });

    return popup;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();

    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setVisible(false);
      presentation.setEnabled(false);
      return;
    }

    IdeView view = event.getData(LangDataKeys.IDE_VIEW);
    if (view == null) {
      presentation.setVisible(false);
      presentation.setEnabled(false);
      return;
    }

    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0) {
      presentation.setVisible(false);
      presentation.setEnabled(false);
      return;
    }

    presentation.setVisible(true);
    presentation.setEnabled(true);

    // is more that one directories not show package support
    if (directories.length > 1) {
      presentation.setText(ChildType.Directory.getName());
      presentation.setIcon(AllIcons.Nodes.TreeClosed);
    }
    else {
      Trinity<ContentFolderTypeProvider, PsiDirectory, ChildType> info = getInfo(directories[0]);

      presentation.setText(info.getThird().getName());

      ContentFolderTypeProvider first = info.getFirst();
      Image childIcon;
      if (first == null) {
        childIcon = AllIcons.Nodes.TreeClosed;
      }
      else {
        childIcon = first.getChildPackageIcon() == null ? first.getChildDirectoryIcon() : first.getChildPackageIcon();
      }
      presentation.setIcon(childIcon);
    }
  }

  @Nonnull
  @RequiredUIAccess
  private static Trinity<ContentFolderTypeProvider, PsiDirectory, ChildType> getInfo(PsiDirectory d) {
    Project project = d.getProject();
    ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);

    Module moduleForPsiElement = ModuleUtilCore.findModuleForPsiElement(d);
    if (moduleForPsiElement != null) {
      boolean isPackageSupported = false;
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleForPsiElement);
      List<PsiPackageSupportProvider> extensions = PsiPackageSupportProvider.EP_NAME.getExtensionList();
      for (ModuleExtension moduleExtension : moduleRootManager.getExtensions()) {
        for (PsiPackageSupportProvider supportProvider : extensions) {
          if (supportProvider.isSupported(moduleExtension)) {
            isPackageSupported = true;
            break;
          }
        }
      }

      if (isPackageSupported) {
        ContentFolderTypeProvider contentFolderTypeForFile = projectFileIndex.getContentFolderTypeForFile(d.getVirtualFile());
        if (contentFolderTypeForFile != null) {
          Image childPackageIcon = contentFolderTypeForFile.getChildPackageIcon();
          return Trinity.create(contentFolderTypeForFile, d, childPackageIcon != null ? ChildType.Package : ChildType.Directory);
        }
      }
    }

    return Trinity.create(null, d, ChildType.Directory);
  }
}
