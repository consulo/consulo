/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import consulo.module.extension.ModuleExtension;
import consulo.psi.PsiPackageSupportProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author peter
 */
public abstract class CreateTemplateInPackageAction<T extends PsiElement> extends CreateFromTemplateAction<T> {
  private final boolean myInSourceOnly;

  protected CreateTemplateInPackageAction(String text, String description, Icon icon, boolean inSourceOnly) {
    super(text, description, icon);
    myInSourceOnly = inSourceOnly;
  }

  @Nullable
  protected Class<? extends ModuleExtension> getModuleExtensionClass() {
    return null;
  }

  @Override
  @Nullable
  protected T createFile(String name, String templateName, PsiDirectory dir) {
    return checkOrCreate(name, dir, templateName);
  }

  @Nullable
  protected abstract PsiElement getNavigationElement(@NotNull T createdElement);

  @Override
  protected boolean isAvailable(final DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (project == null || view == null || view.getDirectories().length == 0) {
      return false;
    }

    final Module module = LangDataKeys.MODULE.getData(dataContext);
    if (module == null) {
      return false;
    }

    final Class<? extends ModuleExtension> moduleExtensionClass = getModuleExtensionClass();
    if (moduleExtensionClass != null && ModuleUtilCore.getExtension(module, moduleExtensionClass) == null) {
      return false;
    }

    if (!myInSourceOnly) {
      return true;
    }

    PsiPackageSupportProvider[] extensions = PsiPackageSupportProvider.EP_NAME.getExtensions();

    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    for (PsiDirectory dir : view.getDirectories()) {
      boolean accepted = false;
      for (PsiPackageSupportProvider provider : extensions) {
        if (provider.acceptVirtualFile(module, dir.getVirtualFile())) {
          accepted = true;
          break;
        }
      }

      if (accepted && projectFileIndex.isInSourceContent(dir.getVirtualFile()) && checkPackageExists(dir)) {
        return true;
      }
    }

    return false;
  }

  protected abstract boolean checkPackageExists(PsiDirectory directory);

  @Nullable
  private T checkOrCreate(String newName, PsiDirectory directory, String templateName) throws IncorrectOperationException {
    PsiDirectory dir = directory;
    String className = removeExtension(templateName, newName);

    if (className.contains(".")) {
      String[] names = className.split("\\.");

      for (int i = 0; i < names.length - 1; i++) {
        dir = CreateFileAction.findOrCreateSubdirectory(dir, names[i]);
      }

      className = names[names.length - 1];
    }

    DumbService service = DumbService.getInstance(dir.getProject());
    service.setAlternativeResolveEnabled(true);
    try {
      return doCreate(dir, className, templateName);
    }
    finally {
      service.setAlternativeResolveEnabled(false);
    }
  }

  protected String removeExtension(String templateName, String className) {
    final String extension = StringUtil.getShortName(templateName);
    if (StringUtil.isNotEmpty(extension)) {
      className = StringUtil.trimEnd(className, "." + extension);
    }
    return className;
  }

  @Nullable
  protected abstract T doCreate(final PsiDirectory dir, final String className, String templateName) throws IncorrectOperationException;
}
