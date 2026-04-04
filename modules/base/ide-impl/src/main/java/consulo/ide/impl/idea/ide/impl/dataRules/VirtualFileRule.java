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

package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.dataContext.DataSnapshot;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;


public final class VirtualFileRule {
  static VirtualFile getData(DataSnapshot dataProvider) {
    // Try to detect multiselection.
    PsiElement[] psiElements = dataProvider.get(PsiElement.KEY_OF_ARRAY);
    if (psiElements != null) {
      for (PsiElement elem : psiElements) {
        VirtualFile virtualFile = PsiUtilBase.getVirtualFile(elem);
        if (virtualFile != null) return virtualFile;
      }
    }

    VirtualFile[] virtualFiles = dataProvider.get(VirtualFile.KEY_OF_ARRAY);
    if (virtualFiles != null && virtualFiles.length == 1) {
      return virtualFiles[0];
    }

    PsiFile psiFile = dataProvider.get(PsiFile.KEY);
    if (psiFile != null) {
      return psiFile.getVirtualFile();
    }
    PsiElement elem = dataProvider.get(PsiElement.KEY);
    if (elem == null) {
      return null;
    }
    return PsiUtilBase.getVirtualFile(elem);
  }
}
