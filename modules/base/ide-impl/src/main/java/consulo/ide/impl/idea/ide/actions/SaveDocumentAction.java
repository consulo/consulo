package consulo.ide.impl.idea.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.PlatformDataKeys;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.FileDocumentManager;

/**
 * @author yole
 */
public class SaveDocumentAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Document doc = getDocument(e);
    if (doc != null) {
      FileDocumentManager.getInstance().saveDocument(doc);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabled(getDocument(e) != null);
  }

  private static Document getDocument(AnActionEvent e) {
    Editor editor = e.getData(PlatformDataKeys.EDITOR);
    return editor != null ? editor.getDocument() : null;
  }
}
