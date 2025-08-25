package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ActionImpl(id = "SaveDocument")
public class SaveDocumentAction extends AnAction {
    public SaveDocumentAction() {
        super(ActionLocalize.actionSavedocumentText(), ActionLocalize.actionSavedocumentDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Document doc = getDocument(e);
        if (doc != null) {
            FileDocumentManager.getInstance().saveDocument(doc);
        }
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(getDocument(e) != null);
    }

    private static Document getDocument(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        return editor != null ? editor.getDocument() : null;
    }
}
