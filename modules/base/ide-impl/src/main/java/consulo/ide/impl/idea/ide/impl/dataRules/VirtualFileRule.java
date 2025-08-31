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

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataProvider;
import consulo.dataContext.GetDataRule;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class VirtualFileRule implements GetDataRule<VirtualFile> {
  @Nonnull
  @Override
  public Key<VirtualFile> getKey() {
    return VirtualFile.KEY;
  }

  @Override
  public VirtualFile getData(@Nonnull DataProvider dataProvider) {
    // Try to detect multiselection.
    PsiElement[] psiElements = dataProvider.getDataUnchecked(PsiElement.KEY_OF_ARRAY);
    if (psiElements != null) {
      for (PsiElement elem : psiElements) {
        VirtualFile virtualFile = PsiUtilBase.getVirtualFile(elem);
        if (virtualFile != null) return virtualFile;
      }
    }

    VirtualFile[] virtualFiles = dataProvider.getDataUnchecked(VirtualFile.KEY_OF_ARRAY);
    if (virtualFiles != null && virtualFiles.length == 1) {
      return virtualFiles[0];
    }

    PsiFile psiFile = dataProvider.getDataUnchecked(PsiFile.KEY);
    if (psiFile != null) {
      return psiFile.getVirtualFile();
    }
    PsiElement elem = dataProvider.getDataUnchecked(PsiElement.KEY);
    if (elem == null) {
      return null;
    }
    return PsiUtilBase.getVirtualFile(elem);
  }
}
