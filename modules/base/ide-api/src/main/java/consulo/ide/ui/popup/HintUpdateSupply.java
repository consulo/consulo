/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.ui.popup;

import consulo.dataContext.DataManager;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.PopupUpdateProcessorBase;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.util.function.Function;

/**
 * Use this class to make various hints like QuickDocumentation, ShowImplementations, etc.
 * respond to the selection change in the original component like ProjectView, various GoTo popups, etc.
 *
 * @author gregsh
 */
public abstract class HintUpdateSupply {
  private static final Key<HintUpdateSupply> HINT_UPDATE_MARKER = Key.create("HINT_UPDATE_MARKER");

  @Nullable
  private JBPopup myHint;

  @Nullable
  public static HintUpdateSupply getSupply(@Nonnull JComponent component) {
    return (HintUpdateSupply)component.getClientProperty(HINT_UPDATE_MARKER);
  }

  public static void hideHint(@Nonnull JComponent component) {
    HintUpdateSupply supply = getSupply(component);
    if (supply != null) supply.hideHint();
  }

  public static void installSimpleHintUpdateSupply(@Nonnull JComponent component) {
    installHintUpdateSupply(component, o -> o instanceof PsiElement ? (PsiElement)o : null);
  }

  public static void installDataContextHintUpdateSupply(@Nonnull JComponent component) {
    installHintUpdateSupply(component, o -> o instanceof PsiElement ? (PsiElement)o : DataManager.getInstance().getDataContext(component).getData(PsiElement.KEY));
  }

  public static void installHintUpdateSupply(@Nonnull final JComponent component, final Function<Object, PsiElement> provider) {
    HintUpdateSupply supply = new HintUpdateSupply(component) {
      @Nullable
      @Override
      protected PsiElement getPsiElementForHint(@Nullable Object selectedValue) {
        return provider.apply(selectedValue);
      }
    };
    if (component instanceof JList) supply.installListListener((JList)component);
    if (component instanceof JTree) supply.installTreeListener((JTree)component);
    if (component instanceof JTable) supply.installTableListener((JTable)component);
  }

  protected HintUpdateSupply(@Nonnull JComponent component) {
    installSupply(component);
  }

  public HintUpdateSupply(@Nonnull JBTable table) {
    installSupply(table);
    installTableListener(table);
  }

  public HintUpdateSupply(@Nonnull Tree tree) {
    installSupply(tree);
    installTreeListener(tree);
  }

  public HintUpdateSupply(@Nonnull JBList list) {
    installSupply(list);
    installListListener(list);
  }

  protected void installTableListener(@Nonnull final JTable table) {
    ListSelectionListener listener = new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!isHintVisible(HintUpdateSupply.this.myHint) || isSelectedByMouse(table)) return;

        int selected = ((ListSelectionModel)e.getSource()).getLeadSelectionIndex();
        int rowCount = table.getRowCount();
        if (selected == -1 || rowCount == 0) return;

        PsiElement element = getPsiElementForHint(table.getValueAt(Math.min(selected, rowCount - 1), 0));
        if (element != null && element.isValid()) {
          updateHint(element);
        }
      }
    };
    table.getSelectionModel().addListSelectionListener(listener);
    table.getColumnModel().getSelectionModel().addListSelectionListener(listener);
  }

  protected void installTreeListener(@Nonnull final JTree tree) {
    tree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        if (!isHintVisible(HintUpdateSupply.this.myHint) || isSelectedByMouse(tree)) return;

        TreePath path = tree.getSelectionPath();
        if (path != null) {
          PsiElement psiElement = getPsiElementForHint(path.getLastPathComponent());
          if (psiElement != null && psiElement.isValid()) {
            updateHint(psiElement);
          }
        }
      }
    });
  }

  protected void installListListener(@Nonnull final JList list) {
    list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!isHintVisible(HintUpdateSupply.this.myHint) || isSelectedByMouse(list)) return;

        Object[] selectedValues = ((JList)e.getSource()).getSelectedValues();
        if (selectedValues.length != 1) return;

        PsiElement element = getPsiElementForHint(selectedValues[0]);
        if (element != null && element.isValid()) {
          updateHint(element);
        }
      }
    });
  }

  @Nullable
  protected abstract PsiElement getPsiElementForHint(@Nullable Object selectedValue);

  private void installSupply(@Nonnull JComponent component) {
    component.putClientProperty(HINT_UPDATE_MARKER, this);
  }

  public void registerHint(JBPopup hint) {
    hideHint();
    myHint = hint;
  }

  public void hideHint() {
    if (isHintVisible(myHint)) {
      myHint.cancel();
    }

    myHint = null;
  }

  public void updateHint(PsiElement element) {
    if (!isHintVisible(myHint)) return;

    PopupUpdateProcessorBase updateProcessor = myHint.getUserData(PopupUpdateProcessorBase.class);
    if (updateProcessor != null) {
      updateProcessor.updatePopup(element);
    }
  }

  @Contract("!null->true")
  private static boolean isHintVisible(JBPopup hint) {
    return hint != null && hint.isVisible();
  }

  private static boolean isSelectedByMouse(@Nonnull JComponent c) {
    return Boolean.TRUE.equals(c.getClientProperty(ListUtil.SELECTED_BY_MOUSE_EVENT));
  }
}
