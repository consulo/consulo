/*
 * Copyright 2013 must-be.org
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
package org.mustbe.consulo.roots.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.actions.MarkRootAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.psi.PsiDirectory;
import org.consulo.psi.PsiPackage;
import org.consulo.psi.PsiPackageManager;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 22:48/31.10.13
 */
public abstract class BaseContentFolderTypeProvider extends ContentFolderTypeProvider {
  protected BaseContentFolderTypeProvider(String id) {
    super(id);
  }

  @NotNull
  public String getMarkActionDescription() {
    return ProjectBundle.message("module.toggle.0.action.description", getName());
  }

  @Override
  public final Icon getChildDirectoryIcon(PsiDirectory psiDirectory) {
    if(psiDirectory != null) {
      PsiPackage anyPackage = PsiPackageManager.getInstance(psiDirectory.getProject()).findAnyPackage(psiDirectory);
      if(anyPackage != null) {
        return getChildPackageIcon();
      }
      else {
        return super.getChildDirectoryIcon(psiDirectory);
      }
    }
    else {
      return getChildPackageIcon();
    }
  }

  public Icon getChildPackageIcon() {
    return AllIcons.Nodes.TreeOpen;
  }

  @NotNull
  @Override
  public AnAction createMarkAction() {
    return new MarkRootAction(getName(), getMarkActionDescription(), getIcon(), this);
  }
}
