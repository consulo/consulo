package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author zajac
 * @since 2011-03-05
 */
public class SwitchToReplace extends AnAction {
  public SwitchToReplace(@Nonnull JComponent shortcutHolder) {
    AnAction replaceAction = ActionManager.getInstance().getAction("Replace");
    if (replaceAction != null) {
      registerCustomShortcutSet(replaceAction.getShortcutSet(), shortcutHolder);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    Editor editor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
    e.getPresentation().setEnabled(editor != null && search != null && !ConsoleViewUtil.isConsoleViewEditor(editor));
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    EditorSearchSession search = e.getRequiredData(EditorSearchSession.SESSION_KEY);
    FindModel findModel = search.getFindModel();
    FindUtil.configureFindModel(true, e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE), findModel, false);
    search.getComponent().selectSearchAll();
  }
}
