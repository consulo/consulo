package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.find.FindBundle;
import consulo.find.FindSettings;
import consulo.ide.impl.idea.find.SearchSession;
import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public class ToggleWholeWordsOnlyAction extends EditorHeaderToggleAction implements Embeddable {
  public ToggleWholeWordsOnlyAction() {
    super(FindBundle.message("find.whole.words"), AllIcons.Actions.Words, AllIcons.Actions.WordsHovered, AllIcons.Actions.WordsSelected);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    SearchSession session = e.getData(SearchSession.KEY);
    e.getPresentation().setEnabled(session != null && !session.getFindModel().isRegularExpressions());
    e.getPresentation().setVisible(session != null && !session.getFindModel().isMultiline());

    super.update(e);
  }

  @Override
  protected boolean isSelected(@Nonnull SearchSession session) {
    return session.getFindModel().isWholeWordsOnly();
  }

  @Override
  protected void setSelected(@Nonnull SearchSession session, boolean selected) {
    FindSettings.getInstance().setLocalWholeWordsOnly(selected);
    session.getFindModel().setWholeWordsOnly(selected);
  }
}
