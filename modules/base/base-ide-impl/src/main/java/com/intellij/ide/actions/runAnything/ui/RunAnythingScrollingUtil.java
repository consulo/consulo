// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.ui.ListActions;
import com.intellij.ui.ScrollingUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class RunAnythingScrollingUtil {

  /**
   * @deprecated unused
   */
  @NonNls
  @Deprecated
  protected static final String SELECT_PREVIOUS_ROW_ACTION_ID = ListActions.Up.ID;

  /**
   * @deprecated unused
   */
  @NonNls
  @Deprecated
  protected static final String SELECT_NEXT_ROW_ACTION_ID = ListActions.Down.ID;

  public static void installActions(@Nonnull JList list, @Nonnull JTextField focusParent, @Nonnull Runnable handleFocusParent, boolean isCycleScrolling) {
    ActionMap actionMap = list.getActionMap();
    actionMap.put(ListActions.Up.ID, new MoveAction(ListActions.Up.ID, list, handleFocusParent, isCycleScrolling));
    actionMap.put(ListActions.Down.ID, new MoveAction(ListActions.Down.ID, list, handleFocusParent, isCycleScrolling));

    maybeInstallDefaultShortcuts(list);

    installMoveUpAction(list, focusParent, handleFocusParent, isCycleScrolling);
    installMoveDownAction(list, focusParent, handleFocusParent, isCycleScrolling);
  }

  private static void maybeInstallDefaultShortcuts(@Nonnull JComponent component) {
    InputMap map = component.getInputMap(JComponent.WHEN_FOCUSED);
    UIUtil.maybeInstall(map, ListActions.Up.ID, KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
    UIUtil.maybeInstall(map, ListActions.Down.ID, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
  }

  private static void installMoveDownAction(@Nonnull JList list, @Nonnull JComponent focusParent, @Nonnull Runnable handleFocusParent, final boolean isCycleScrolling) {
    new ScrollingUtil.ListScrollAction(CommonShortcuts.getMoveDown(), focusParent) {
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        moveDown(list, handleFocusParent, isCycleScrolling);
      }
    };
  }

  private static void installMoveUpAction(@Nonnull JList list, @Nonnull JComponent focusParent, @Nonnull Runnable handleFocusParent, final boolean isCycleScrolling) {
    new ScrollingUtil.ListScrollAction(CommonShortcuts.getMoveUp(), focusParent) {
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        moveUp(list, handleFocusParent, isCycleScrolling);
      }
    };
  }

  private static void moveDown(@Nonnull JList list, @Nonnull Runnable handleFocusParent, boolean isCycleScrolling) {
    move(list, list.getSelectionModel(), list.getModel().getSize(), +1, handleFocusParent, isCycleScrolling);
  }

  private static void moveUp(@Nonnull JList list, @Nonnull Runnable handleFocusParent, boolean isCycleScrolling) {
    move(list, list.getSelectionModel(), list.getModel().getSize(), -1, handleFocusParent, isCycleScrolling);
  }

  private static void move(@Nonnull JList c, @Nonnull ListSelectionModel selectionModel, int size, int direction, @Nonnull Runnable handleFocusParent, boolean isCycleScrolling) {
    if (size == 0) return;
    int index = selectionModel.getMaxSelectionIndex();
    int indexToSelect = index + direction;

    if ((indexToSelect == -2 || indexToSelect >= size) && !isCycleScrolling) {
      return;
    }

    if (indexToSelect == -2) {
      indexToSelect = size - 1;
    }
    else if (indexToSelect == -1 || indexToSelect >= size) {
      handleFocusParent.run();
      return;
    }

    ScrollingUtil.ensureIndexIsVisible(c, indexToSelect, -1);
    selectionModel.setSelectionInterval(indexToSelect, indexToSelect);
  }

  private static class MoveAction extends AbstractAction {
    @Nonnull
    private final String myId;
    @Nonnull
    private final JList myComponent;
    @Nonnull
    private final Runnable myHandleFocusParent;
    private final boolean myIsCycleScrolling;

    MoveAction(@Nonnull String id, @Nonnull JList component, @Nonnull Runnable handleFocusParent, boolean isCycleScrolling) {
      myId = id;
      myComponent = component;
      myHandleFocusParent = handleFocusParent;
      myIsCycleScrolling = isCycleScrolling;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (ListActions.Up.ID.equals(myId)) {
        moveUp(myComponent, myHandleFocusParent, myIsCycleScrolling);
      }
      else if (ListActions.Down.ID.equals(myId)) {
        moveDown(myComponent, myHandleFocusParent, myIsCycleScrolling);
      }
    }
  }
}