package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.codeEditor.Editor;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: zajac
 * Date: 05.03.11
 * Time: 10:57
 * To change this template use File | Settings | File Templates.
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
    Editor editor = e.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE);
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
    e.getPresentation().setEnabled(editor != null && search != null && !ConsoleViewUtil.isConsoleViewEditor(editor));
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    EditorSearchSession search = e.getRequiredData(EditorSearchSession.SESSION_KEY);
    FindModel findModel = search.getFindModel();
    FindUtil.configureFindModel(true, e.getData(CommonDataKeys.EDITOR_EVEN_IF_INACTIVE), findModel, false);
    search.getComponent().getSearchTextComponent().selectAll();
  }
}
