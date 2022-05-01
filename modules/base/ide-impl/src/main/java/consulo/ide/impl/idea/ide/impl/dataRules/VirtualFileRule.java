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

import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataProvider;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.dataContext.GetDataRule;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.internal.PsiUtilBase;
import javax.annotation.Nonnull;

public class VirtualFileRule implements GetDataRule<VirtualFile> {
  @Nonnull
  @Override
  public Key<VirtualFile> getKey() {
    return CommonDataKeys.VIRTUAL_FILE;
  }

  @Override
  public VirtualFile getData(@Nonnull final DataProvider dataProvider) {
    // Try to detect multiselection.
    PsiElement[] psiElements = dataProvider.getDataUnchecked(LangDataKeys.PSI_ELEMENT_ARRAY);
    if (psiElements != null) {
      for (PsiElement elem : psiElements) {
        VirtualFile virtualFile = PsiUtilBase.getVirtualFile(elem);
        if (virtualFile != null) return virtualFile;
      }
    }

    VirtualFile[] virtualFiles = dataProvider.getDataUnchecked(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
    if (virtualFiles != null && virtualFiles.length == 1) {
      return virtualFiles[0];
    }

    PsiFile psiFile = dataProvider.getDataUnchecked(LangDataKeys.PSI_FILE);
    if (psiFile != null) {
      return psiFile.getVirtualFile();
    }
    PsiElement elem = dataProvider.getDataUnchecked(LangDataKeys.PSI_ELEMENT);
    if (elem == null) {
      return null;
    }
    return PsiUtilBase.getVirtualFile(elem);
  }
}
