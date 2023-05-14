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
package consulo.language.editor.gutter;

import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.document.util.TextRange;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import consulo.util.lang.lazy.LazyValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author nik
 */
public class RelatedItemLineMarkerInfo<T extends PsiElement> extends LineMarkerInfo<T> {
  private final Supplier<Collection<? extends GotoRelatedItem>> myTargets;

  public RelatedItemLineMarkerInfo(@Nonnull T element,
                                   @Nonnull TextRange range,
                                   Image icon,
                                   int updatePass,
                                   @Nullable Function<? super T, String> tooltipProvider,
                                   @Nullable GutterIconNavigationHandler<T> navHandler,
                                   GutterIconRenderer.Alignment alignment,
                                   @Nonnull Supplier<Collection<? extends GotoRelatedItem>> targets) {
    super(element, range, icon, updatePass, tooltipProvider, navHandler, alignment);
    myTargets = LazyValue.notNull(targets);
  }

  public RelatedItemLineMarkerInfo(@Nonnull T element,
                                   @Nonnull TextRange range,
                                   Image icon,
                                   int updatePass,
                                   @Nullable Function<? super T, String> tooltipProvider,
                                   @Nullable GutterIconNavigationHandler<T> navHandler,
                                   GutterIconRenderer.Alignment alignment,
                                   @Nonnull final Collection<? extends GotoRelatedItem> targets) {
    this(element, range, icon, updatePass, tooltipProvider, navHandler, alignment, () -> targets);
  }

  @Nonnull
  public Collection<? extends GotoRelatedItem> createGotoRelatedItems() {
    return myTargets.get();
  }

  @Override
  public GutterIconRenderer createGutterRenderer() {
    if (myIcon == null) return null;
    return new RelatedItemLineMarkerGutterIconRenderer<T>(this);
  }

  private static class RelatedItemLineMarkerGutterIconRenderer<T extends PsiElement> extends LineMarkerGutterIconRenderer<T> {
    public RelatedItemLineMarkerGutterIconRenderer(final RelatedItemLineMarkerInfo<T> markerInfo) {
      super(markerInfo);
    }

    @Override
    protected boolean looksTheSameAs(@Nonnull LineMarkerGutterIconRenderer renderer) {
      if (!(renderer instanceof RelatedItemLineMarkerGutterIconRenderer) || !super.looksTheSameAs(renderer)) {
        return false;
      }

      final RelatedItemLineMarkerInfo<?> markerInfo = (RelatedItemLineMarkerInfo<?>)getLineMarkerInfo();
      final RelatedItemLineMarkerInfo<?> otherInfo = (RelatedItemLineMarkerInfo<?>)renderer.getLineMarkerInfo();
      return markerInfo.myTargets.equals(otherInfo.myTargets);
    }
  }
}
