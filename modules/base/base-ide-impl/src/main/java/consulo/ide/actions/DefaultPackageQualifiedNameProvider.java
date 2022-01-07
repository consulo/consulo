/*
 * Copyright 2013-2016 consulo.io
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

import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 22:51/08.10.13
 */
public class DefaultPackageQualifiedNameProvider implements QualifiedNameProvider {
  @Nullable
  @Override
  public PsiElement adjustElementToCopy(PsiElement element) {
    if (element instanceof PsiPackage) {
      return element;
    }
    if (element instanceof PsiDirectory) {
      final PsiPackage psiPackage = PsiPackageManager.getInstance(element.getProject()).findAnyPackage((PsiDirectory)element);
      if (psiPackage != null) {
        return psiPackage;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public String getQualifiedName(PsiElement element) {
    if (element instanceof PsiDirectory) {
      PsiPackage anyPackage = PsiPackageManager.getInstance(element.getProject()).findAnyPackage((PsiDirectory)element);
      if (anyPackage != null) {
        return anyPackage.getQualifiedName();
      }
    }
    else if (element instanceof PsiPackage) {
      return ((PsiPackage)element).getQualifiedName();
    }
    return null;
  }

  @Nullable
  @Override
  public PsiElement qualifiedNameToElement(String fqn, Project project) {
    return PsiPackageManager.getInstance(project).findAnyPackage(fqn);
  }
}
