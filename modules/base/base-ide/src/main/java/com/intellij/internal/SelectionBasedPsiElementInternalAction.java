package com.intellij.internal;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

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
      IntroduceTargetChooser.showChooser(
        editor, expressions,
        new Pass<T>() {
          @Override
          public void pass(@Nonnull T expression) {
            performOnElement(editor, expression);
          }
        },
        new Function<T, String>() {
          public String fun(@Nonnull T expression) {
            return expression.getText();
          }
        }
      );
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
