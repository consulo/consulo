package consulo.ide.impl.idea.ide.actionMacro.actions;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;

/**
 * @author Evgeny.Zakrevsky
 * @since 2012-08-14
 */
public class PlaySavedMacros extends AnAction {
  @Override
  @RequiredUIAccess
  public void actionPerformed(final AnActionEvent e) {
    final ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
      "Play Saved Macros",
      new MacrosGroup(),
      e.getDataContext(),
      JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
      false
    );
    final Project project = e.getData(Project.KEY);
    if (project != null ) {
      popup.showCenteredInCurrentWindow(project);
    } else {
      popup.showInFocusCenter();
    }
  }
}
