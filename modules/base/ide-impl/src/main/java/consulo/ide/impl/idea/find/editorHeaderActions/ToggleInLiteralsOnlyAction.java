package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.find.FindSearchContext;
import consulo.fileEditor.impl.internal.search.EditorHeaderSetSearchContextAction;

public class ToggleInLiteralsOnlyAction extends EditorHeaderSetSearchContextAction {
    public ToggleInLiteralsOnlyAction() {
        super("In &Literals Only", FindSearchContext.IN_STRING_LITERALS);
    }
}
