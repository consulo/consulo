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
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataProvider;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.dataContext.GetDataRule;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class PsiFileRule implements GetDataRule<PsiFile> {
  @Nonnull
  @Override
  public Key<PsiFile> getKey() {
    return CommonDataKeys.PSI_FILE;
  }

  @Override
  public PsiFile getData(@Nonnull DataProvider dataProvider) {
    final PsiElement element = dataProvider.getDataUnchecked(LangDataKeys.PSI_ELEMENT);
    if (element != null) {
      return element.getContainingFile();
    }
    Project project = dataProvider.getDataUnchecked(CommonDataKeys.PROJECT);
    if (project != null) {
      VirtualFile vFile = dataProvider.getDataUnchecked(PlatformDataKeys.VIRTUAL_FILE);
      if (vFile != null) {
        return PsiManager.getInstance(project).findFile(vFile);
      }
    }
    return null;
  }
}
