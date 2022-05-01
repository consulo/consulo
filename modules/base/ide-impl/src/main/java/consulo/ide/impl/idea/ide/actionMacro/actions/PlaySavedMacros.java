package consulo.ide.impl.idea.ide.actionMacro.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
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
