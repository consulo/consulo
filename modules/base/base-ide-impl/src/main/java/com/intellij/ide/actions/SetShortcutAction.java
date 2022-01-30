// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.ActionShortcutRestrictions;
import com.intellij.openapi.keymap.impl.ui.KeymapPanel;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;
import javax.annotation.Nonnull;

import java.awt.*;

public class SetShortcutAction extends AnAction implements DumbAware {

  public final static Key<AnAction> SELECTED_ACTION = Key.create("SelectedAction");

  public SetShortcutAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    JBPopup seDialog = project == null ? null : project.getUserData(SearchEverywhereManager.SEARCH_EVERYWHERE_POPUP);
    if (seDialog == null) {
      return;
    }

    KeymapManager km = KeymapManager.getInstance();
    Keymap activeKeymap = km != null ? km.getActiveKeymap() : null;
    if (activeKeymap == null) {
      return;
    }

    AnAction action = e.getData(SELECTED_ACTION);
    Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    if (action == null || component == null) {
      return;
    }

    seDialog.cancel();
    String id = ActionManager.getInstance().getId(action);
    KeymapPanel.addKeyboardShortcut(id, ActionShortcutRestrictions.getInstance().getForActionId(id), activeKeymap, component);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();

    Project project = e.getData(CommonDataKeys.PROJECT);
    JBPopup seDialog = project == null ? null : project.getUserData(SearchEverywhereManager.SEARCH_EVERYWHERE_POPUP);
    if (seDialog == null) {
      presentation.setEnabled(false);
      return;
    }

    KeymapManager km = KeymapManager.getInstance();
    Keymap activeKeymap = km != null ? km.getActiveKeymap() : null;
    if (activeKeymap == null) {
      presentation.setEnabled(false);
      return;
    }

    AnAction action = e.getData(SELECTED_ACTION);
    Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    presentation.setEnabled(action != null && component != null);
  }
}
