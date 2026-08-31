package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.codeEditor.Editor;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.EditorSearchSession;
import consulo.ide.impl.idea.find.FindUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.internal.ActionUpdateInvoker;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.LegacyDumbAwareAction;

import javax.swing.*;

/**
 * @author zajac
 * @since 2011-03-05
 */
public class SwitchToFind extends LegacyDumbAwareAction {
  public SwitchToFind(JComponent shortcutHolder) {
    AnAction findAction = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND);
    if (findAction != null) {
      registerCustomShortcutSet(findAction.getShortcutSet(), shortcutHolder);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    if (KeymapUtil.isEmacsKeymap()) {
      // Emacs users are accustomed to the editor that executes 'find next' on subsequent pressing of shortcut that
      // activates 'incremental search'. Hence, we do the similar hack here for them.
      ActionUpdateInvoker.updateSync(ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_NEXT), e);
    }
    else {
      e.getPresentation().setEnabledAndVisible(e.hasData(EditorSearchSession.SESSION_KEY));
    }
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    if (KeymapUtil.isEmacsKeymap()) {
      // Emacs users are accustomed to the editor that executes 'find next' on subsequent pressing of shortcut that
      // activates 'incremental search'. Hence, we do the similar hack here for them.
      ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_NEXT).actionPerformed(e);
      return;
    }

    EditorSearchSession search = e.getRequiredData(EditorSearchSession.SESSION_KEY);
    FindModel findModel = search.getFindModel();
    FindUtil.configureFindModel(false, e.getData(Editor.KEY), findModel, false);
    search.getComponent().selectSearchAll();
  }
}
