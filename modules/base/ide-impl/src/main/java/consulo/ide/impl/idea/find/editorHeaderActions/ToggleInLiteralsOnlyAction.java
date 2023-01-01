package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.find.FindModel;

public class ToggleInLiteralsOnlyAction extends EditorHeaderSetSearchContextAction {
  public ToggleInLiteralsOnlyAction() {
    super("In &Literals Only", FindModel.SearchContext.IN_STRING_LITERALS);
  }
}
