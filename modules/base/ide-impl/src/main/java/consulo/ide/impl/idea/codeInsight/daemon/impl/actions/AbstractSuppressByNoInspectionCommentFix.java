/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl.actions;

import consulo.ui.ex.awt.Messages;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.SuppressionUtil;
import consulo.language.editor.intention.SuppressIntentionAction;
import consulo.language.editor.util.LanguageUndoUtil;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author Roman.Chernyatchik
 * @date Aug 13, 2009
 */
public abstract class AbstractSuppressByNoInspectionCommentFix extends SuppressIntentionAction {
  @Nonnull
  protected final String myID;
  private final boolean myReplaceOtherSuppressionIds;

  @Nullable
  protected abstract PsiElement getContainer(final PsiElement context);

  /**
   * @param ID                         Inspection ID
   * @param replaceOtherSuppressionIds Merge suppression policy. If false new tool id will be append to the end
   *                                   otherwise replace other ids
   */
  public AbstractSuppressByNoInspectionCommentFix(@Nonnull String ID, final boolean replaceOtherSuppressionIds) {
    myID = ID;
    myReplaceOtherSuppressionIds = replaceOtherSuppressionIds;
  }

  protected final void replaceSuppressionComment(@Nonnull final PsiElement comment) {
    SuppressionUtil.replaceSuppressionComment(comment, myID, myReplaceOtherSuppressionIds, getCommentLanguage(comment));
  }

  protected void createSuppression(@Nonnull Project project,
                                   @Nonnull PsiElement element,
                                   @Nonnull PsiElement container) throws IncorrectOperationException {
    SuppressionUtil.createSuppression(project, container, myID, getCommentLanguage(element));
  }

  /**
   * @param element quickfix target or existing comment element
   * @return language that will be used for comment creating.
   * In common case language will be the same as language of quickfix target
   */
  @Nonnull
  protected Language getCommentLanguage(@Nonnull PsiElement element) {
    return element.getLanguage();
  }

  @Override
  public boolean isAvailable(@Nonnull final Project project, final Editor editor, @Nonnull final PsiElement context) {
    return context.isValid() && context.getManager().isInProject(context) && getContainer(context) != null;
  }

  @Override
  public void invoke(@Nonnull final Project project, @Nullable Editor editor, @Nonnull final PsiElement element) throws IncorrectOperationException {
    PsiElement container = getContainer(element);
    if (container == null) return;

    if (!FileModificationService.getInstance().preparePsiElementForWrite(container)) return;

    final List<? extends PsiElement> comments = getCommentsFor(container);
    if (comments != null) {
      for (PsiElement comment : comments) {
        if (comment instanceof PsiComment && SuppressionUtil.isSuppressionComment(comment)) {
          replaceSuppressionComment(comment);
          return;
        }
      }
    }

    boolean caretWasBeforeStatement = editor != null && editor.getCaretModel().getOffset() == container.getTextRange().getStartOffset();
    try {
      createSuppression(project, element, container);
    }
    catch (IncorrectOperationException e) {
      if (!ApplicationManager.getApplication().isUnitTestMode() && editor != null) {
        Messages.showErrorDialog(editor.getComponent(),
                                 InspectionsBundle.message("suppress.inspection.annotation.syntax.error", e.getMessage()));
      }
    }

    if (caretWasBeforeStatement) {
      editor.getCaretModel().moveToOffset(container.getTextRange().getStartOffset());
    }
    LanguageUndoUtil.markPsiFileForUndo(element.getContainingFile());
  }

  @Nullable
  protected List<? extends PsiElement> getCommentsFor(@Nonnull final PsiElement container) {
    final PsiElement prev = PsiTreeUtil.skipSiblingsBackward(container, PsiWhiteSpace.class);
    if (prev == null) {
      return null;
    }
    return Collections.singletonList(prev);
  }

  @Nonnull
  public String getText() {
    return InspectionsBundle.message("suppress.inspection.family");
  }
}
