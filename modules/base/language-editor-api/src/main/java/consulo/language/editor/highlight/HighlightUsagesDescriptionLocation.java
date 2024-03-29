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

package consulo.language.editor.highlight;

import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.meta.PsiPresentableMetaData;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class HighlightUsagesDescriptionLocation extends ElementDescriptionLocation {
  private HighlightUsagesDescriptionLocation() {
  }

  @Override
  public ElementDescriptionProvider getDefaultProvider() {
    return new ElementDescriptionProvider() {
      @Override
      public String getElementDescription(@Nonnull PsiElement element, @Nonnull ElementDescriptionLocation location) {
        if (element instanceof PsiPresentableMetaData) {
          return ((PsiPresentableMetaData)element).getTypeName();
        }
        return null;
      }
    };
  }

  public static HighlightUsagesDescriptionLocation INSTANCE = new HighlightUsagesDescriptionLocation();

}
