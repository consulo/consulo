/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import consulo.language.editor.PlatformDataKeys;
import consulo.dataContext.GetDataRule;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class PsiElementFromSelectionsRule implements GetDataRule<PsiElement[]> {
  @Nonnull
  @Override
  public Key<PsiElement[]> getKey() {
    return PsiElement.KEY_OF_ARRAY;
  }

  @Override
  public PsiElement[] getData(@Nonnull DataProvider dataProvider) {
    Object[] objects = dataProvider.getDataUnchecked(PlatformDataKeys.SELECTED_ITEMS);
    if (objects != null) {
      PsiElement[] elements = new PsiElement[objects.length];
      for (int i = 0, objectsLength = objects.length; i < objectsLength; i++) {
        Object object = objects[i];
        if (object instanceof PsiElement element && element.isValid()) {
          elements[i] = element;
        }
        else {
          return null;
        }
      }

      return elements;
    }
    return null;
  }
}
