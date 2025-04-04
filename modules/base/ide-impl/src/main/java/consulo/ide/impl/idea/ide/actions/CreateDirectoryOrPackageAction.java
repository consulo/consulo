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

package consulo.ide.impl.idea.ide.actions;

import consulo.ide.IdeView;
import consulo.ide.action.ui.NewItemPopupUtil;
import consulo.ide.action.ui.NewItemSimplePopupPanel;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.content.ContentFolderTypeProvider;
import consulo.ide.impl.actions.CreateDirectoryOrPackageType;
import consulo.language.LangBundle;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPackageSupportProvider;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.ui.TextBox;
import consulo.ui.HasValidator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.util.lang.Trinity;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class CreateDirectoryOrPackageAction extends AnAction implements DumbAware {
  public CreateDirectoryOrPackageAction() {
    super(IdeLocalize.actionCreateNewDirectoryOrPackage(), IdeLocalize.actionCreateNewDirectoryOrPackage(), null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    IdeView view = e.getData(IdeView.KEY);
    Project project = e.getData(Project.KEY);

    if (view == null || project == null) {
      return;
    }

    PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(view);

    if (directory == null) {
      return;
    }

    Trinity<ContentFolderTypeProvider, PsiDirectory, CreateDirectoryOrPackageType> info = getInfo(directory);

    boolean isDirectory = info.getThird() == CreateDirectoryOrPackageType.Directory;

    CreateDirectoryOrPackageHandler validator = new CreateDirectoryOrPackageHandler(project, directory, info.getThird(), info.getThird().getSeparator());

    LocalizeValue title = isDirectory ? IdeLocalize.titleNewDirectory() : IdeLocalize.titleNewPackage();

    String defaultValue = info.getThird().getDefaultValue(directory);

    createLightWeightPopup(
      validator,
      title.get(),
      defaultValue,
      element -> {
        if (element != null) {
          view.selectElement(element);
        }
      })
      .showCenteredInCurrentWindow(project);
  }

  @Nonnull
  @RequiredUIAccess
  private JBPopup createLightWeightPopup(
    CreateDirectoryOrPackageHandler validator,
    String title,
    String defaultValue,
    Consumer<PsiElement> consumer
  ) {
    NewItemSimplePopupPanel contentPanel = new NewItemSimplePopupPanel();
    TextBox nameField = contentPanel.getTextField();
    if (!StringUtil.isEmptyOrSpaces(defaultValue)) {
        nameField.setValue(defaultValue);
    }
    JBPopup popup = NewItemPopupUtil.createNewItemPopup(title, contentPanel, (JComponent)TargetAWT.to(nameField));
    contentPanel.addValidator(value -> {
      if (!validator.checkInput(value)) {
        String message = InputValidatorEx.getErrorText(validator, value, LangBundle.message("incorrect.name"));
        return new HasValidator.ValidationInfo(message);
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

    Project project = event.getData(Project.KEY);
    if (project == null) {
      presentation.setVisible(false);
      presentation.setEnabled(false);
      return;
    }

    IdeView view = event.getData(IdeView.KEY);
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
      presentation.setText(CreateDirectoryOrPackageType.Directory.getName());
      presentation.setIcon(AllIcons.Nodes.TreeClosed);
    }
    else {
      Trinity<ContentFolderTypeProvider, PsiDirectory, CreateDirectoryOrPackageType> info = getInfo(directories[0]);

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
  private static Trinity<ContentFolderTypeProvider, PsiDirectory, CreateDirectoryOrPackageType> getInfo(PsiDirectory d) {
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
          return Trinity.create(contentFolderTypeForFile, d, childPackageIcon != null ? CreateDirectoryOrPackageType.Package : CreateDirectoryOrPackageType.Directory);
        }
      }
    }

    return Trinity.create(null, d, CreateDirectoryOrPackageType.Directory);
  }
}
