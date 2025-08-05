package consulo.ide.impl.idea.ide.actionMacro.actions;

import consulo.annotation.component.ActionImpl;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;

/**
 * @author Evgeny.Zakrevsky
 * @since 2012-08-14
 */
@ActionImpl(id = "PlaySavedMacrosAction")
public class PlaySavedMacros extends AnAction {
    public PlaySavedMacros() {
        super(ActionLocalize.actionPlaysavedmacrosactionText(), ActionLocalize.actionPlaysavedmacrosactionDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            IdeLocalize.popupTitlePlaySavedMacros().get(),
            new MacrosGroup(),
            e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        );
        Project project = e.getData(Project.KEY);
        if (project != null) {
            popup.showCenteredInCurrentWindow(project);
        }
        else {
            popup.showInFocusCenter();
        }
    }
}
