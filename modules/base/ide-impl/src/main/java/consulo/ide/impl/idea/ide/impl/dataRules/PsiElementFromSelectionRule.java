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
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public final class PsiElementFromSelectionRule {
  static @Nullable PsiElement getData(DataSnapshot dataProvider) {
    Object element = dataProvider.get(PlatformDataKeys.SELECTED_ITEM);
    return element instanceof PsiElement psiElement && psiElement.isValid() ? psiElement : null;
  }
}
