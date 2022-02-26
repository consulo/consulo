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
package com.intellij.codeInsight.navigation;

import consulo.language.editor.gutter.GutterIconNavigationHandler;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.ide.util.PsiElementListCellRenderer;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopup;
import consulo.ide.ui.popup.JBPopupFactory;
import consulo.application.util.function.Computable;
import com.intellij.openapi.util.NotNullLazyValue;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.PsiUtilCore;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.RelativePoint;
import com.intellij.util.NullableFunction;
import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author peter
 */
public abstract class NavigationGutterIconRenderer extends GutterIconRenderer implements GutterIconNavigationHandler<PsiElement>{
  private final String myPopupTitle;
  private final String myEmptyText;
  private final Computable<PsiElementListCellRenderer> myCellRenderer;
  private final NotNullLazyValue<List<SmartPsiElementPointer>> myPointers;

  protected NavigationGutterIconRenderer(final String popupTitle, final String emptyText, @Nonnull Computable<PsiElementListCellRenderer> cellRenderer,
    @Nonnull NotNullLazyValue<List<SmartPsiElementPointer>> pointers) {
    myPopupTitle = popupTitle;
    myEmptyText = emptyText;
    myCellRenderer = cellRenderer;
    myPointers = pointers;
  }

  public boolean isNavigateAction() {
    return true;
  }

  public List<PsiElement> getTargetElements() {
    return ContainerUtil.mapNotNull(myPointers.getValue(), new NullableFunction<SmartPsiElementPointer, PsiElement>() {
      public PsiElement fun(final SmartPsiElementPointer smartPsiElementPointer) {
        return smartPsiElementPointer.getElement();
      }
    });
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final NavigationGutterIconRenderer renderer = (NavigationGutterIconRenderer)o;

    if (myEmptyText != null ? !myEmptyText.equals(renderer.myEmptyText) : renderer.myEmptyText != null) return false;
    if (!myPointers.getValue().equals(renderer.myPointers.getValue())) return false;
    if (myPopupTitle != null ? !myPopupTitle.equals(renderer.myPopupTitle) : renderer.myPopupTitle != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myPopupTitle != null ? myPopupTitle.hashCode() : 0);
    result = 31 * result + (myEmptyText != null ? myEmptyText.hashCode() : 0);
    result = 31 * result + myPointers.getValue().hashCode();
    return result;
  }

  @Nullable
  public AnAction getClickAction() {
    return new AnAction() {
      public void actionPerformed(AnActionEvent e) {
        navigate(e == null ? null : (MouseEvent)e.getInputEvent(), null);
      }
    };
  }

  @RequiredUIAccess
  public void navigate(@Nullable final MouseEvent event, @Nullable final PsiElement elt) {
    final List<PsiElement> list = getTargetElements();
    if (list.isEmpty()) {
      if (myEmptyText != null) {
        if (event != null) {
          final JComponent label = HintUtil.createErrorLabel(myEmptyText);
          label.setBorder(IdeBorderFactory.createEmptyBorder(2, 7, 2, 7));
          JBPopupFactory.getInstance().createBalloonBuilder(label)
            .setFadeoutTime(3000)
            .setFillColor(HintUtil.ERROR_COLOR)
            .createBalloon()
            .show(new RelativePoint(event), Balloon.Position.above);
        }
      }
      return;
    }
    if (list.size() == 1) {
      PsiNavigateUtil.navigate(list.iterator().next());
    }
    else {
      if (event != null) {
        final JBPopup popup = NavigationUtil.getPsiElementPopup(PsiUtilCore.toPsiElementArray(list), myCellRenderer.compute(), myPopupTitle);
        popup.show(new RelativePoint(event));
      }
    }
  }
}
