package consulo.ide.impl.idea.codeInsight.editorActions.fillParagraph;

import consulo.codeEditor.Editor;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.ParagraphFillHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * User : ktisha
 * <p>
 * Action to re-flow paragraph to fit right margin.
 * Glues paragraph and then splits into lines with appropriate length
 * <p>
 * The action came from Emacs users // PY-4775
 */
public class FillParagraphAction extends BaseCodeInsightAction {

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new Handler();
  }

  private static class Handler implements CodeInsightActionHandler {

    @RequiredUIAccess
    @Override
    public void invoke(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull final PsiFile file) {

      ParagraphFillHandler paragraphFillHandler = ParagraphFillHandler.forLanguage(file.getLanguage());

      final int offset = editor.getCaretModel().getOffset();
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

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    final ParagraphFillHandler handler = ParagraphFillHandler.forLanguage(file.getLanguage());
    return handler.isAvailableForFile(file);
  }
}
