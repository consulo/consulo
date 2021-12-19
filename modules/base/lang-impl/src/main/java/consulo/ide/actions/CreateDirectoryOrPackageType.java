/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public enum CreateDirectoryOrPackageType {
  Directory {
    @Nonnull
    @Override
    public String getName() {
      return IdeBundle.message("action.directory");
    }

    @Nonnull
    @Override
    public String getSeparator() {
      return "\\/";
    }

    @Nonnull
    @Override
    public String getDefaultValue(PsiDirectory directory) {
      return "";
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public PsiDirectory createDirectory(@Nonnull PsiDirectory psiDirectory, @Nonnull String name) {
      return DirectoryUtil.createSubdirectories(name, psiDirectory, "/");
    }
  },
  Package {
    @Nonnull
    @Override
    public String getName() {
      return IdeBundle.message("action.package");
    }

    @Nonnull
    @Override
    public String getSeparator() {
      return ".";
    }

    @Nonnull
    @Override
    public String getDefaultValue(PsiDirectory directory) {
      PsiPackage psiPackage = PsiPackageManager.getInstance(directory.getProject()).findAnyPackage(directory);
      if (psiPackage != null) {
        String qualifiedName = psiPackage.getQualifiedName();
        if (!StringUtil.isEmptyOrSpaces(qualifiedName)) {
          return qualifiedName + ".";
        }
      }
      return "";
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public PsiDirectory createDirectory(@Nonnull PsiDirectory psiDirectory, @Nonnull String name) {
      ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(psiDirectory.getProject());
      ContentFolder contentFolder = projectFileIndex.getContentFolder(psiDirectory.getVirtualFile());
      if (contentFolder == null) {
        throw new UnsupportedOperationException("PsiDirectory not under content folder " + psiDirectory.getVirtualFile().getPath());
      }

      VirtualFile file = contentFolder.getFile();

      PsiDirectory contentPsiFolder = PsiManager.getInstance(psiDirectory.getProject()).findDirectory(file);

      return DirectoryUtil.createSubdirectories(name, contentPsiFolder, getSeparator());
    }
  };

  @Nonnull
  public abstract String getName();

  @Nonnull
  public abstract String getSeparator();

  @Nonnull
  public abstract String getDefaultValue(PsiDirectory directory);

  @Nonnull
  @RequiredUIAccess
  public abstract PsiDirectory createDirectory(@Nonnull PsiDirectory psiDirectory, @Nonnull String name) throws IncorrectOperationException;}
