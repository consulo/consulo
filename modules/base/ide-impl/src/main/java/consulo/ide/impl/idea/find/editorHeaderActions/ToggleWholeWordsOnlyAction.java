package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.fileEditor.impl.internal.search.SearchSession;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

public class ToggleWholeWordsOnlyAction extends EditorSearchToggleAction implements Embeddable {
    public ToggleWholeWordsOnlyAction() {
        super(
            FindLocalize.findWholeWords(),
            PlatformIconGroup.actionsWords(),
            PlatformIconGroup.actionsWordshovered(),
            PlatformIconGroup.actionsWordsselected()
        );
    }

    @Override
    public boolean displayTextInToolbar() {
        return false;
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
