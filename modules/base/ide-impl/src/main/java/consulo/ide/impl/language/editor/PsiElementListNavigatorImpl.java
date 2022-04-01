/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.language.editor;

import com.intellij.find.FindUtil;
import com.intellij.ide.PsiCopyPasteManager;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import consulo.ide.ui.impl.PopupChooserBuilder;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.editor.ui.PsiElementListNavigator;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListComponentUpdater;
import consulo.usage.UsageView;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ref.Ref;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 01-Apr-22
 */
@Singleton
public class PsiElementListNavigatorImpl implements PsiElementListNavigator {
  private static class NavigateOrPopupBuilderImpl extends NavigateOrPopupBuilder {
    public NavigateOrPopupBuilderImpl(@Nonnull NavigatablePsiElement[] targets, String title) {
      super(targets, title);
    }

    @Override
    @Nullable
    public final JBPopup build() {
      if (myTargets.length == 0) {
        if (!allowEmptyTargets()) return null; // empty initial targets are not allowed
        if (myListUpdaterTask == null || myListUpdaterTask.isFinished()) return null; // there will be no targets.
      }
      if (myTargets.length == 1 && (myListUpdaterTask == null || myListUpdaterTask.isFinished())) {
        myTargetsConsumer.accept(myTargets);
        return null;
      }
      List<NavigatablePsiElement> initialTargetsList = Arrays.asList(myTargets);
      Ref<NavigatablePsiElement[]> updatedTargetsList = Ref.create(myTargets);

      final IPopupChooserBuilder<NavigatablePsiElement> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(initialTargetsList);
      afterPopupBuilderCreated(builder);
      if (myListRenderer instanceof PsiElementListCellRenderer) {
        ((PsiElementListCellRenderer)myListRenderer).installSpeedSearch(builder);
      }

      IPopupChooserBuilder<NavigatablePsiElement> popupChooserBuilder = builder.
              setTitle(myTitle).
              setMovable(true).
              setFont(EditorUtil.getEditorFont()).
              setRenderer(myListRenderer).
              withHintUpdateSupply().
              setResizable(true).
              setItemsChosenCallback(selectedValues -> myTargetsConsumer.accept(ArrayUtil.toObjectArray(selectedValues))).
              setCancelCallback(() -> {
                if (myListUpdaterTask != null) {
                  myListUpdaterTask.cancelTask();
                }
                return true;
              });
      final Ref<UsageView> usageView = new Ref<>();
      if (myFindUsagesTitle != null) {
        popupChooserBuilder = popupChooserBuilder.setCouldPin(popup -> {
          usageView.set(FindUtil.showInUsageView(null, updatedTargetsList.get(), myFindUsagesTitle, getProject()));
          popup.cancel();
          return false;
        });
      }

      final JBPopup popup = popupChooserBuilder.createPopup();
      if (builder instanceof PopupChooserBuilder) {
        JBList<NavigatablePsiElement> list = (JBList)((PopupChooserBuilder)builder).getChooserComponent();
        list.setTransferHandler(new TransferHandler() {
          @Override
          protected Transferable createTransferable(JComponent c) {
            final Object[] selectedValues = list.getSelectedValues();
            final PsiElement[] copy = new PsiElement[selectedValues.length];
            for (int i = 0; i < selectedValues.length; i++) {
              copy[i] = (PsiElement)selectedValues[i];
            }
            return new PsiCopyPasteManager.MyTransferable(copy);
          }

          @Override
          public int getSourceActions(JComponent c) {
            return COPY;
          }
        });

        JScrollPane pane = ((PopupChooserBuilder)builder).getScrollPane();
        pane.setBorder(null);
        pane.setViewportBorder(null);
      }

      if (myListUpdaterTask != null) {
        ListComponentUpdater popupUpdater = builder.getBackgroundUpdater();
        myListUpdaterTask.init(popup, new ListComponentUpdater<>() {
          @Override
          public void replaceModel(@Nonnull List<? extends PsiElement> data) {
            updatedTargetsList.set(data.toArray(NavigatablePsiElement.EMPTY_ARRAY));
            popupUpdater.replaceModel(data);
          }

          @Override
          public void paintBusy(boolean paintBusy) {
            popupUpdater.paintBusy(paintBusy);
          }
        }, usageView);
      }
      return popup;
    }
  }

  @Override
  public NavigateOrPopupBuilder builder(@Nonnull NavigatablePsiElement[] targets, String title) {
    return new NavigateOrPopupBuilderImpl(targets, title);
  }
}
