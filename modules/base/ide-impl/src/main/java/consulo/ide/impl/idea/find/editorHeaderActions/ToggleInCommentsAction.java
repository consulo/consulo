package consulo.ide.impl.idea.find.editorHeaderActions;

import consulo.find.FindModel;

public class ToggleInCommentsAction extends EditorHeaderSetSearchContextAction {
  public ToggleInCommentsAction() {
    super("In &Comments Only", FindModel.SearchContext.IN_COMMENTS);
  }
}
