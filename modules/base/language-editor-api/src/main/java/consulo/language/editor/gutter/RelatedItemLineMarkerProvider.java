/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.gutter;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * Override this class and register the implementation as {@code codeInsight.lineMarkerProvider} extension to provide both line marker and
 * 'Go to related file' targets
 *
 * @author nik
 */
public abstract class RelatedItemLineMarkerProvider implements LineMarkerProvider {
  @RequiredReadAction
  @Override
  public RelatedItemLineMarkerInfo getLineMarkerInfo(@Nonnull PsiElement element) {
    return null;
  }

  @RequiredReadAction
  @Override
  public final void collectSlowLineMarkers(@Nonnull List<PsiElement> elements, @Nonnull Collection<LineMarkerInfo> result) {
    collectNavigationMarkers(elements, result, false);
  }

  public void collectNavigationMarkers(List<PsiElement> elements,
                                       Collection<? super RelatedItemLineMarkerInfo> result,
                                       boolean forNavigation) {
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0, size = elements.size(); i < size; i++) {
      PsiElement element = elements.get(i);
      collectNavigationMarkers(element, result);
      if (forNavigation && element instanceof PsiNameIdentifierOwner) {
        PsiElement nameIdentifier = ((PsiNameIdentifierOwner)element).getNameIdentifier();
        if (nameIdentifier != null && !elements.contains(nameIdentifier)) {
          collectNavigationMarkers(nameIdentifier, result);
        }
      }
    }
  }

  protected void collectNavigationMarkers(@Nonnull PsiElement element, Collection<? super RelatedItemLineMarkerInfo> result) {
  }
}
