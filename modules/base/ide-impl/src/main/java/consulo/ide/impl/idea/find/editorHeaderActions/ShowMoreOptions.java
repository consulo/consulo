// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionToolbarEx;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class ShowMoreOptions extends AnAction implements DumbAware {
  private final ActionToolbarEx myToolbarComponent;

  //placeholder for keymap
  public ShowMoreOptions() {
    myToolbarComponent = null;
  }

  public ShowMoreOptions(ActionToolbarEx toolbarComponent, JComponent shortcutHolder) {
    this.myToolbarComponent = toolbarComponent;
    KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
    if (keyboardShortcut != null) {
      registerCustomShortcutSet(new CustomShortcutSet(keyboardShortcut), shortcutHolder);
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final ActionButton secondaryActions = myToolbarComponent.getSecondaryActionsButton();
    if (secondaryActions != null) {
      secondaryActions.click();
    }
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(myToolbarComponent != null && myToolbarComponent.getSecondaryActionsButton() != null);
  }
}
