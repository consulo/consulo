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
package consulo.language.editor.ui.navigation;

import consulo.application.util.function.Computable;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.editor.hint.HintColorUtil;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.util.PsiNavigateUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author peter
 */
public abstract class NavigationGutterIconRenderer extends GutterIconRenderer implements GutterIconNavigationHandler<PsiElement> {
  private final String myPopupTitle;
  private final String myEmptyText;
  private final Computable<PsiElementListCellRenderer> myCellRenderer;
  private final Supplier<List<SmartPsiElementPointer>> myPointers;

  protected NavigationGutterIconRenderer(final String popupTitle,
                                         final String emptyText,
                                         @Nonnull Computable<PsiElementListCellRenderer> cellRenderer,
                                         @Nonnull Supplier<List<SmartPsiElementPointer>> pointers) {
    myPopupTitle = popupTitle;
    myEmptyText = emptyText;
    myCellRenderer = cellRenderer;
    myPointers = pointers;
  }

  @Override
  public boolean isNavigateAction() {
    return true;
  }

  public List<PsiElement> getTargetElements() {
    return ContainerUtil.mapNotNull(myPointers.get(), SmartPsiElementPointer::getElement);
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final NavigationGutterIconRenderer renderer = (NavigationGutterIconRenderer)o;

    if (myEmptyText != null ? !myEmptyText.equals(renderer.myEmptyText) : renderer.myEmptyText != null) return false;
    if (!myPointers.get().equals(renderer.myPointers.get())) return false;
    if (myPopupTitle != null ? !myPopupTitle.equals(renderer.myPopupTitle) : renderer.myPopupTitle != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myPopupTitle != null ? myPopupTitle.hashCode() : 0);
    result = 31 * result + (myEmptyText != null ? myEmptyText.hashCode() : 0);
    result = 31 * result + myPointers.get().hashCode();
    return result;
  }

  @Override
  @Nullable
  public AnAction getClickAction() {
    return new AnAction() {
      @Override
      @RequiredUIAccess
      public void actionPerformed(AnActionEvent e) {
        navigate(e == null ? null : (MouseEvent)e.getInputEvent(), null);
      }
    };
  }

  @Override
  @RequiredUIAccess
  public void navigate(@Nullable final MouseEvent event, @Nullable final PsiElement elt) {
    final List<PsiElement> list = getTargetElements();
    if (list.isEmpty()) {
      if (myEmptyText != null) {
        if (event != null) {
          final JComponent label = HintUtil.createErrorLabel(myEmptyText);
          label.setBorder(IdeBorderFactory.createEmptyBorder(2, 7, 2, 7));
          JBPopupFactory.getInstance().createBalloonBuilder(label).setFadeoutTime(3000).setFillColor(TargetAWT.to(HintColorUtil.getErrorColor())).createBalloon().show(new RelativePoint(event), Balloon.Position.above);
        }
      }
      return;
    }
    if (list.size() == 1) {
      PsiNavigateUtil.navigate(list.iterator().next());
    }
    else {
      if (event != null) {
        final JBPopup popup = PopupNavigationUtil.getPsiElementPopup(PsiUtilCore.toPsiElementArray(list), myCellRenderer.compute(), myPopupTitle);
        popup.show(new RelativePoint(event));
      }
    }
  }
}
