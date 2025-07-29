package consulo.ide.impl.idea.codeInsight.editorActions.fillParagraph;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.ParagraphFillHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

/**
 * Action to re-flow paragraph to fit right margin.
 * Glues paragraph and then splits into lines with appropriate length
 * <p>
 * The action came from Emacs users // PY-4775
 *
 * @author ktisha
 */
@ActionImpl(id = "FillParagraph")
public class FillParagraphAction extends BaseCodeInsightAction {
    private static class Handler implements CodeInsightActionHandler {
        @Override
        @RequiredUIAccess
        public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
            ParagraphFillHandler paragraphFillHandler = ParagraphFillHandler.forLanguage(file.getLanguage());

            int offset = editor.getCaretModel().getOffset();
            PsiElement element = file.findElementAt(offset);
            if (element != null && paragraphFillHandler.isAvailableForFile(file) && paragraphFillHandler.isAvailableForElement(element)) {
                paragraphFillHandler.performOnElement(element, editor);
            }
        }

        @Override
        public boolean startInWriteAction() {
            return true;
        }
    }

    public FillParagraphAction() {
        super(ActionLocalize.actionFillparagraphText(), ActionLocalize.actionFillparagraphDescription());
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new Handler();
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        ParagraphFillHandler handler = ParagraphFillHandler.forLanguage(file.getLanguage());
        return handler.isAvailableForFile(file);
    }
}
