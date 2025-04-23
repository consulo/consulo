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

import consulo.annotation.access.RequiredReadAction;
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
import consulo.localize.LocalizeValue;
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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author peter
 */
public abstract class NavigationGutterIconRenderer extends GutterIconRenderer implements GutterIconNavigationHandler<PsiElement> {
    @Nonnull
    private final LocalizeValue myPopupTitle;
    @Nonnull
    private final LocalizeValue myEmptyText;
    private final Supplier<PsiElementListCellRenderer> myCellRenderer;
    private final Supplier<List<SmartPsiElementPointer>> myPointers;

    protected NavigationGutterIconRenderer(
        @Nonnull LocalizeValue popupTitle,
        @Nonnull LocalizeValue emptyText,
        @Nonnull Supplier<PsiElementListCellRenderer> cellRenderer,
        @Nonnull Supplier<List<SmartPsiElementPointer>> pointers
    ) {
        myPopupTitle = popupTitle;
        myEmptyText = emptyText;
        myCellRenderer = cellRenderer;
        myPointers = pointers;
    }

    @Override
    public boolean isNavigateAction() {
        return true;
    }

    @RequiredReadAction
    public List<PsiElement> getTargetElements() {
        return ContainerUtil.mapNotNull(myPointers.get(), SmartPsiElementPointer::getElement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NavigationGutterIconRenderer renderer = (NavigationGutterIconRenderer)o;

        return myEmptyText.equals(renderer.myEmptyText)
            && myPointers.get().equals(renderer.myPointers.get())
            && myPopupTitle.equals(renderer.myPopupTitle);
    }

    @Override
    public int hashCode() {
        int result;
        result = myPopupTitle.hashCode();
        result = 31 * result + myEmptyText.hashCode();
        result = 31 * result + myPointers.get().hashCode();
        return result;
    }

    @Override
    @Nullable
    public AnAction getClickAction() {
        return new AnAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                navigate((MouseEvent)e.getInputEvent(), null);
            }
        };
    }

    @Override
    @RequiredUIAccess
    public void navigate(@Nullable MouseEvent event, @Nullable PsiElement elt) {
        List<PsiElement> list = getTargetElements();
        if (list.isEmpty()) {
            if (myEmptyText != LocalizeValue.empty() && event != null) {
                JComponent label = HintUtil.createErrorLabel(myEmptyText.get());
                label.setBorder(IdeBorderFactory.createEmptyBorder(2, 7, 2, 7));
                JBPopupFactory.getInstance()
                    .createBalloonBuilder(label)
                    .setFadeoutTime(3000)
                    .setFillColor(TargetAWT.to(HintColorUtil.getErrorColor()))
                    .createBalloon()
                    .show(new RelativePoint(event), Balloon.Position.above);
            }
            return;
        }
        if (list.size() == 1) {
            PsiNavigateUtil.navigate(list.iterator().next());
        }
        else if (event != null) {
            JBPopup popup =
                PopupNavigationUtil.getPsiElementPopup(PsiUtilCore.toPsiElementArray(list), myCellRenderer.get(), myPopupTitle.get());
            popup.show(new RelativePoint(event));
        }
    }
}
