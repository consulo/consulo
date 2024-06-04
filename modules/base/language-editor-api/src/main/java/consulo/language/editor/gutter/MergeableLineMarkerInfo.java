/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * @author Konstantin Bulenkov
 */
public abstract class MergeableLineMarkerInfo<T extends PsiElement> extends LineMarkerInfo<T> {
  public MergeableLineMarkerInfo(@Nonnull T element,
                                 @Nonnull TextRange textRange,
                                 Image icon,
                                 int updatePass,
                                 @Nullable Function<? super T, String> tooltipProvider,
                                 @Nullable GutterIconNavigationHandler<T> navHandler,
                                 @Nonnull GutterIconRenderer.Alignment alignment) {
    super(element, textRange, icon, updatePass, tooltipProvider, navHandler, alignment);
  }

  public abstract boolean canMergeWith(@Nonnull MergeableLineMarkerInfo<?> info);

  @Nonnull
  public abstract Image getCommonIcon(@Nonnull List<MergeableLineMarkerInfo> infos);

  @Nonnull
  public abstract Function<? super PsiElement, String> getCommonTooltip(@Nonnull List<MergeableLineMarkerInfo> infos);

  public GutterIconRenderer.Alignment getCommonIconAlignment(@Nonnull List<MergeableLineMarkerInfo> infos) {
    return GutterIconRenderer.Alignment.LEFT;
  }

  public String getElementPresentation(PsiElement element) {
    return element.getText();
  }

  public int getCommonUpdatePass(@Nonnull List<MergeableLineMarkerInfo> infos) {
    return updatePass;
  }

  @Nonnull
  public static List<LineMarkerInfo<PsiElement>> merge(@Nonnull List<? extends MergeableLineMarkerInfo<PsiElement>> markers) {
    List<LineMarkerInfo<PsiElement>> result = new SmartList<>();
    for (int i = 0; i < markers.size(); i++) {
      MergeableLineMarkerInfo marker = markers.get(i);
      List<MergeableLineMarkerInfo> toMerge = new SmartList<>();
      for (int k = markers.size() - 1; k > i; k--) {
        MergeableLineMarkerInfo current = markers.get(k);
        if (marker.canMergeWith(current)) {
          toMerge.add(0, current);
          markers.remove(k);
        }
      }
      if (toMerge.isEmpty()) {
        result.add(marker);
      }
      else {
        toMerge.add(0, marker);
        result.add(new MyLineMarkerInfo(toMerge));
      }
    }
    return result;
  }

  private static class MyLineMarkerInfo extends LineMarkerInfo<PsiElement> {
    private MyLineMarkerInfo(@Nonnull List<MergeableLineMarkerInfo> markers) {
      this(markers, markers.get(0));
    }

    private MyLineMarkerInfo(@Nonnull List<MergeableLineMarkerInfo> markers, @Nonnull MergeableLineMarkerInfo template) {
      //noinspection ConstantConditions
      super(template.getElement(), getCommonTextRange(markers), template.getCommonIcon(markers), template.getCommonUpdatePass(markers), template.getCommonTooltip(markers),
            getCommonNavigationHandler(markers), template.getCommonIconAlignment(markers));
    }

    private static TextRange getCommonTextRange(List<MergeableLineMarkerInfo> markers) {
      int startOffset = Integer.MAX_VALUE;
      int endOffset = Integer.MIN_VALUE;
      for (MergeableLineMarkerInfo marker : markers) {
        startOffset = Math.min(startOffset, marker.startOffset);
        endOffset = Math.max(endOffset, marker.endOffset);
      }
      return TextRange.create(startOffset, endOffset);
    }

    private static GutterIconNavigationHandler<PsiElement> getCommonNavigationHandler(@Nonnull final List<MergeableLineMarkerInfo> markers) {
      return new GutterIconNavigationHandler<>() {
        @RequiredUIAccess
        @Override
        public void navigate(final MouseEvent e, PsiElement elt) {
          final List<LineMarkerInfo> infos = new ArrayList<>(markers);
          Collections.sort(infos, (o1, o2) -> o1.startOffset - o2.startOffset);
          IPopupChooserBuilder<LineMarkerInfo> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(infos);
          builder.setRenderer(LanguageEditorInternalHelper.getInstance().createMergeableLineMarkerRender(new Function<>() {
            @Nonnull
            @Override
            public Pair<String, Image> apply(LineMarkerInfo dom) {
              Image icon = null;
              final GutterIconRenderer renderer = dom.createGutterRenderer();
              if (renderer != null) {
                icon = renderer.getIcon();
              }
              PsiElement element = dom.getElement();
              assert element != null;
              final String elementPresentation = dom instanceof MergeableLineMarkerInfo ? ((MergeableLineMarkerInfo)dom).getElementPresentation(element) : element.getText();
              String text = StringUtil.first(elementPresentation, 100, true).replace('\n', ' ');

              return Pair.create(text, icon);
            }
          }));
          builder.setItemChosenCallback(value -> {
            if (value != null) {
              final GutterIconNavigationHandler handler = value.getNavigationHandler();
              if (handler != null) {
                //noinspection unchecked
                handler.navigate(e, value.getElement());
              }
            }
          });

          builder.createPopup().show(new RelativePoint(e));
        }
      };
    }
  }
}
