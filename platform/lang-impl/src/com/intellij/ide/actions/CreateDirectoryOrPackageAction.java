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
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.consulo.psi.PsiPackage;
import org.consulo.psi.PsiPackageManager;

import javax.swing.*;

public class CreateDirectoryOrPackageAction extends AnAction implements DumbAware {
  private enum CreateType {
    Directory {
      @Override
      public Icon getIcon() {
        return AllIcons.Nodes.TreeClosed;
      }

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
      public Icon getIcon() {
        return AllIcons.Nodes.Package;
      }

      @Override
      public String getName() {
        return IdeBundle.message("action.package");
      }

      @Override
      public String getSeparator() {
        return ".";
      }
    },
    TestPackage {
      @Override
      public Icon getIcon() {
        return AllIcons.Nodes.TestPackage;
      }

      @Override
      public String getName() {
        return IdeBundle.message("action.package");
      }

      @Override
      public String getSeparator() {
        return ".";
      }
    };

    public abstract Icon getIcon();

    public abstract String getName();

    public abstract String getSeparator();

    public static CreateType findCreateType(PsiDirectory... directories) {
      if (directories.length == 0) {
        return Directory;
      }
      Project project = directories[0].getProject();
      PsiPackageManager packageManager = PsiPackageManager.getInstance(project);
      ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);

      for (PsiDirectory directory : directories) {
        if (projectFileIndex.isInResource(directory.getVirtualFile()) || projectFileIndex.isInTestResource(directory.getVirtualFile())) {
          continue;
        }
        PsiPackage anyPackage = packageManager.findAnyPackage(directory);
        if (anyPackage != null) {
          return projectFileIndex.isInTestSourceContent(directory.getVirtualFile()) ? TestPackage : Package;
        }
      }
      return Directory;
    }
  }

  public CreateDirectoryOrPackageAction() {
    super(IdeBundle.message("action.create.new.directory.or.package"), IdeBundle.message("action.create.new.directory.or.package"), null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    Project project = e.getData(CommonDataKeys.PROJECT);

    if (view == null || project == null) {
      return;
    }
    PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(view);

    if (directory == null) {
      return;
    }
    CreateType createType = CreateType.findCreateType(directory);

    CreateDirectoryOrPackageHandler validator =
      new CreateDirectoryOrPackageHandler(project, directory, createType == CreateType.Directory, createType.getSeparator());
    Messages.showInputDialog(project, IdeBundle.message("prompt.enter.new.name"),
                             createType.getName(),
                             Messages.getQuestionIcon(), "", validator);

    final PsiElement result = validator.getCreatedElement();
    if (result != null) {
      view.selectElement(result);
    }
  }

  @Override
  public void update(AnActionEvent event) {
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

    CreateType createType = CreateType.findCreateType(directories);

    presentation.setText(createType.getName());
    presentation.setIcon(createType.getIcon());
  }
}
