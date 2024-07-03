/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * created at Nov 26, 2001
 *
 * @author Jeka
 */
package consulo.language.editor.refactoring.move;

import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesUtil;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class MoveHandler implements RefactoringActionHandler {
  public static final LocalizeValue REFACTORING_NAME = RefactoringLocalize.moveTitle();

  /**
   * called by an Action in AtomicAction when refactoring is invoked from Editor
   */
  @RequiredUIAccess
  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    int offset = editor.getCaretModel().getOffset();
    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
    PsiElement element = file.findElementAt(offset);
    while (true) {
      if (element == null) {
        String message = RefactoringBundle.getCannotRefactorMessage(
          RefactoringLocalize.theCaretShouldBePositionedAtTheClassMethodOrFieldToBeRefactored().get()
        );
        CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME.get(), null);
        return;
      }

      if (tryToMoveElement(element, project, dataContext, null, editor)) {
        return;
      }
      final TextRange range = element.getTextRange();
      if (range != null) {
        int relative = offset - range.getStartOffset();
        final PsiReference reference = element.findReferenceAt(relative);
        if (reference != null) {
          final PsiElement refElement = reference.resolve();
          if (refElement != null && tryToMoveElement(refElement, project, dataContext, reference, editor)) return;
        }
      }

      element = element.getParent();
    }
  }

  private static boolean tryToMoveElement(
    final PsiElement element,
    final Project project,
    final DataContext dataContext,
    final PsiReference reference,
    final Editor editor
  ) {
    for (MoveHandlerDelegate delegate : MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.tryToMove(element, project, dataContext, reference, editor)) {
        return true;
      }
    }

    return false;
  }

  /**
   * called by an Action in AtomicAction
   */
  @Override
  @RequiredUIAccess
  public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    final PsiElement targetContainer = dataContext == null ? null : dataContext.getData(LangDataKeys.TARGET_PSI_ELEMENT);
    final Set<PsiElement> filesOrDirs = new HashSet<>();
    for (MoveHandlerDelegate delegate : MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(dataContext) && delegate.isValidTarget(targetContainer, elements)) {
        delegate.collectFilesOrDirsFromContext(dataContext, filesOrDirs);
      }
    }
    if (!filesOrDirs.isEmpty()) {
      for (PsiElement element : elements) {
        if (element instanceof PsiDirectory) {
          filesOrDirs.add(element);
        }
        else {
          final PsiFile containingFile = element.getContainingFile();
          if (containingFile != null) {
            filesOrDirs.add(containingFile);
          }
        }
      }
      MoveFilesOrDirectoriesUtil.doMove(project, PsiUtilBase.toPsiElementArray(filesOrDirs), new PsiElement[]{targetContainer}, null);
      return;
    }
    doMove(project, elements, targetContainer, dataContext, null);
  }

  /**
   * must be invoked in AtomicAction
   */
  public static void doMove(Project project, @Nonnull PsiElement[] elements, PsiElement targetContainer, DataContext dataContext, MoveCallback callback) {
    if (elements.length == 0) return;

    for (MoveHandlerDelegate delegate : MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(elements, targetContainer)) {
        delegate.doMove(project, elements, delegate.adjustTargetForMove(dataContext, targetContainer), callback);
        break;
      }
    }
  }

  /**
   * Performs some extra checks (that canMove does not)
   * May replace some elements with others which actulaly shall be moved (e.g. directory->package)
   */
  @Nullable
  public static PsiElement[] adjustForMove(Project project, final PsiElement[] sourceElements, final PsiElement targetElement) {
    for (MoveHandlerDelegate delegate : MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(sourceElements, targetElement)) {
        return delegate.adjustForMove(project, sourceElements, targetElement);
      }
    }
    return sourceElements;
  }

  /**
   * Must be invoked in AtomicAction
   * target container can be null => means that container is not determined yet and must be spacify by the user
   */
  public static boolean canMove(@Nonnull PsiElement[] elements, PsiElement targetContainer) {
    for (MoveHandlerDelegate delegate : MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(elements, targetContainer)) return true;
    }

    return false;
  }

  public static boolean isValidTarget(final PsiElement psiElement, PsiElement[] elements) {
    if (psiElement != null) {
      for (MoveHandlerDelegate delegate : MoveHandlerDelegate.EP_NAME.getExtensionList()) {
        if (delegate.isValidTarget(psiElement, elements)) {
          return true;
        }
      }
    }

    return false;
  }

  public static boolean canMove(DataContext dataContext) {
    for (MoveHandlerDelegate delegate : MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.canMove(dataContext)) return true;
    }

    return false;
  }

  public static boolean isMoveRedundant(PsiElement source, PsiElement target) {
    for (MoveHandlerDelegate delegate : MoveHandlerDelegate.EP_NAME.getExtensionList()) {
      if (delegate.isMoveRedundant(source, target)) return true;
    }
    return false;
  }
}
