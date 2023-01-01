/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.psi.util;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nullable;

public class PsiNavigateUtil {
  private PsiNavigateUtil() {
  }

  public static void navigate(@Nullable final PsiElement psiElement) {
    navigate(psiElement, true);
  }

  public static void navigate(@Nullable final PsiElement psiElement, boolean requestFocus) {
    if (psiElement != null && psiElement.isValid()) {
      final PsiElement navigationElement = psiElement.getNavigationElement();
      final int offset = navigationElement instanceof PsiFile ? -1 : navigationElement.getTextOffset();
      final VirtualFile virtualFile = navigationElement.getContainingFile().getVirtualFile();
      if (virtualFile != null && virtualFile.isValid()) {
        OpenFileDescriptorFactory.getInstance(navigationElement.getProject()).builder(virtualFile).offset(offset).build().navigate(requestFocus);
      }
    }
  }
}