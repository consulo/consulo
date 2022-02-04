package com.intellij.ide.actionMacro.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;

/**
 * User: Evgeny.Zakrevsky
 * Date: 8/14/12
 */
public class PlaySavedMacros extends AnAction {
  @Override
  public void actionPerformed(final AnActionEvent e) {
    final ListPopup popup = JBPopupFactory.getInstance()
      .createActionGroupPopup("Play Saved Macros", new MacrosGroup(), e.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                              false);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project != null ) {
      popup.showCenteredInCurrentWindow(project);
    } else {
      popup.showInFocusCenter();
    }
  }
}
