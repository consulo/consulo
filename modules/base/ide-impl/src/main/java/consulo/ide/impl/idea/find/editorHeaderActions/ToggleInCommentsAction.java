package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.find.FindSearchContext;

public class ToggleInCommentsAction extends EditorHeaderSetSearchContextAction {
    public ToggleInCommentsAction() {
        super("In &Comments Only", FindSearchContext.IN_COMMENTS);
    }
}
