package consulo.ide.impl.idea.internal;

import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.refactoring.IntroduceTargetChooser;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.document.util.TextRange;
import consulo.language.editor.hint.HintManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Nikolay Matveev
 */
public abstract class SelectionBasedPsiElementInternalAction<T extends PsiElement> extends AnAction {
  @Nonnull
  protected final Class<T> myClass;
  @Nonnull
  protected final Class<? extends PsiFile> myFileClass;

  protected SelectionBasedPsiElementInternalAction(@Nonnull Class<T> aClass, @Nonnull Class<? extends PsiFile> fileClass) {
    myClass = aClass;
    myFileClass = fileClass;
  }

  @Override
  public final void actionPerformed(AnActionEvent e) {
    final Editor editor = getEditor(e);
    final PsiFile file = getPsiFile(e);
    if (editor == null || file == null) return;

    final List<T> expressions = getElement(editor, file);
    T first = ContainerUtil.getFirstItem(expressions);

    if (expressions.size() > 1) {
      IntroduceTargetChooser.showChooser(editor, expressions, expression -> performOnElement(editor, expression), PsiElement::getText);
    }
    else if (expressions.size() == 1 && first != null) {
      performOnElement(editor, first);
    }
    else if (expressions.isEmpty()) {
      showError(editor);
    }
  }

  protected void showError(@Nonnull final Editor editor) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        final String errorHint = "Cannot find element of class " + myClass.getSimpleName() + " at selection/offset";
        HintManager.getInstance().showErrorHint(editor, errorHint);
      }
    });
  }

  private void performOnElement(@Nonnull final Editor editor, @Nonnull T first) {
    final TextRange textRange = first.getTextRange();
    editor.getSelectionModel().setSelection(textRange.getStartOffset(), textRange.getEndOffset());
    final String informationHint = getInformationHint(first);
    if (informationHint != null) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          HintManager.getInstance().showInformationHint(editor, informationHint);
        }
      });
    }
    else {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          HintManager.getInstance().showErrorHint(editor, getErrorHint());
        }
      });
    }
  }

  @Nullable
  protected abstract String getInformationHint(@Nonnull T element);

  @Nonnull
  protected abstract String getErrorHint();

  @Nonnull
  protected List<T> getElement(@Nonnull Editor editor, @Nonnull PsiFile file) {
    final SelectionModel selectionModel = editor.getSelectionModel();
    if (selectionModel.hasSelection()) {
      return ContainerUtil.list(getElementFromSelection(file, selectionModel));
    }
    return getElementAtOffset(editor, file);
  }

  @Nonnull
  protected List<T> getElementAtOffset(@Nonnull Editor editor, @Nonnull PsiFile file) {
    return ContainerUtil.list(PsiTreeUtil.findElementOfClassAtOffset(file, editor.getCaretModel().getOffset(), myClass, false));
  }

  @Nullable
  protected T getElementFromSelection(@Nonnull PsiFile file, @Nonnull SelectionModel selectionModel) {
    final int selectionStart = selectionModel.getSelectionStart();
    final int selectionEnd = selectionModel.getSelectionEnd();
    return PsiTreeUtil.findElementOfClassAtRange(file, selectionStart, selectionEnd, myClass);
  }

  @Override
  public final void update(AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    boolean enabled = Application.get().isInternal() && getEditor(e) != null && myFileClass.isInstance(getPsiFile(e));
    presentation.setVisible(enabled);
    presentation.setEnabled(enabled);
  }

  @Nullable
  private static Editor getEditor(@Nonnull AnActionEvent e) {
    return e.getDataContext().getData(PlatformDataKeys.EDITOR);
  }

  @Nullable
  private static PsiFile getPsiFile(@Nonnull AnActionEvent e) {
    return e.getDataContext().getData(LangDataKeys.PSI_FILE);
  }
}
