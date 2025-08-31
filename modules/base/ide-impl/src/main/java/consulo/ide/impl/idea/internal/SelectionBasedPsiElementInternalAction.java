package consulo.ide.impl.idea.internal;

import consulo.language.editor.refactoring.introduce.IntroduceTargetChooser;
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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
  @RequiredUIAccess
  public final void actionPerformed(@Nonnull AnActionEvent e) {
    Editor editor = e.getRequiredData(Editor.KEY);
    PsiFile file = e.getData(PsiFile.KEY);
    if (editor == null || file == null) return;

    List<T> expressions = getElement(editor, file);
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

  protected void showError(@Nonnull Editor editor) {
    ApplicationManager.getApplication().invokeLater(() -> {
      String errorHint = "Cannot find element of class " + myClass.getSimpleName() + " at selection/offset";
      HintManager.getInstance().showErrorHint(editor, errorHint);
    });
  }

  private void performOnElement(@Nonnull Editor editor, @Nonnull T first) {
    TextRange textRange = first.getTextRange();
    editor.getSelectionModel().setSelection(textRange.getStartOffset(), textRange.getEndOffset());
    String informationHint = getInformationHint(first);
    if (informationHint != null) {
      ApplicationManager.getApplication().invokeLater(() -> HintManager.getInstance().showInformationHint(editor, informationHint));
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> HintManager.getInstance().showErrorHint(editor, getErrorHint()));
    }
  }

  @Nullable
  protected abstract String getInformationHint(@Nonnull T element);

  @Nonnull
  protected abstract String getErrorHint();

  @Nonnull
  protected List<T> getElement(@Nonnull Editor editor, @Nonnull PsiFile file) {
    SelectionModel selectionModel = editor.getSelectionModel();
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
    int selectionStart = selectionModel.getSelectionStart();
    int selectionEnd = selectionModel.getSelectionEnd();
    return PsiTreeUtil.findElementOfClassAtRange(file, selectionStart, selectionEnd, myClass);
  }

  @Override
  public final void update(@Nonnull AnActionEvent e) {
    boolean enabled = Application.get().isInternal() && e.hasData(Editor.KEY) && myFileClass.isInstance(e.getData(PsiFile.KEY));
    e.getPresentation().setEnabledAndVisible(enabled);
  }
}
