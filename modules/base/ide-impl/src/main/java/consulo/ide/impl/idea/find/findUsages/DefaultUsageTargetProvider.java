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

package consulo.ide.impl.idea.find.findUsages;

import consulo.annotation.component.ExtensionImpl;
import consulo.find.FindManager;
import consulo.navigation.NavigationItem;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiElement;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.usage.UsageTarget;
import consulo.usage.UsageTargetProvider;

@ExtensionImpl(id = "default", order = "last")
public class DefaultUsageTargetProvider implements UsageTargetProvider {
  @Override
  public UsageTarget[] getTargets(Editor editor, PsiFile file) {
    return null;
  }

  @Override
  public UsageTarget[] getTargets(PsiElement psiElement) {
    if (psiElement instanceof NavigationItem) {
      if (FindManager.getInstance(psiElement.getProject()).canFindUsages(psiElement)) {
        return new UsageTarget[]{new PsiElement2UsageTargetAdapter(psiElement)};
      }
    }
    return null;
  }
}
