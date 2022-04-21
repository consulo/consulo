package com.intellij.codeInsight.editorActions.fillParagraph;

import consulo.language.editor.action.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * User : ktisha
 *
 * Action to re-flow paragraph to fit right margin.
 * Glues paragraph and then splits into lines with appropriate length
 *
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

      ParagraphFillHandler paragraphFillHandler = LanguageFillParagraphExtension.INSTANCE.forLanguage(file.getLanguage());

      final int offset = editor.getCaretModel().getOffset();
      PsiElement element = file.findElementAt(offset);
      if (element != null && paragraphFillHandler != null && paragraphFillHandler.isAvailableForFile(file)
          && paragraphFillHandler.isAvailableForElement(element)) {

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
    final ParagraphFillHandler handler =
      LanguageFillParagraphExtension.INSTANCE.forLanguage(file.getLanguage());
    return handler != null && handler.isAvailableForFile(file);
  }

}
