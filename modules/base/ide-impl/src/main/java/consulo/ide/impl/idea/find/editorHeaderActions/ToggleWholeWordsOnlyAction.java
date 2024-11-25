package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.application.AllIcons;
import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.SearchSession;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public class ToggleWholeWordsOnlyAction extends EditorSearchToggleAction implements Embeddable {
    public ToggleWholeWordsOnlyAction() {
        super(
            FindLocalize.findWholeWords(),
            AllIcons.Actions.Words,
            AllIcons.Actions.WordsHovered,
            AllIcons.Actions.WordsSelected
        );
    }

    @Override
    public boolean displayTextInToolbar() {
        return false;
    }
    
    @RequiredUIAccess
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
