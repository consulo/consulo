/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl.actions;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInspection.*;
import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ThreeState;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author Roman.Chernyatchik
 * @date Aug 13, 2009
 */
public abstract class AbstractBatchSuppressByNoInspectionCommentFix implements ContainerBasedSuppressQuickFix, InjectionAwareSuppressQuickFix, Iconable {
  @Nonnull
  protected final String myID;
  private final boolean myReplaceOtherSuppressionIds;
  private ThreeState myShouldBeAppliedToInjectionHost = ThreeState.UNSURE;

  @Override
  @Nullable
  public abstract PsiElement getContainer(final PsiElement context);

  /**
   * @param ID                         Inspection ID
   * @param replaceOtherSuppressionIds Merge suppression policy. If false new tool id will be append to the end
   *                                   otherwise replace other ids
   */
  public AbstractBatchSuppressByNoInspectionCommentFix(@Nonnull String ID, final boolean replaceOtherSuppressionIds) {
    myID = ID;
    myReplaceOtherSuppressionIds = replaceOtherSuppressionIds;
  }

  @Override
  public void setShouldBeAppliedToInjectionHost(@Nonnull ThreeState shouldBeAppliedToInjectionHost) {
    myShouldBeAppliedToInjectionHost = shouldBeAppliedToInjectionHost;
  }

  @Nonnull
  @Override
  public ThreeState isShouldBeAppliedToInjectionHost() {
    return myShouldBeAppliedToInjectionHost;
  }

  @Nonnull
  @Override
  public String getName() {
    return getText();
  }

  @Override
  public Image getIcon(int flags) {
    return AllIcons.General.InspectionsTrafficOff;
  }

  private String myText = "";
  @Nonnull
  public String getText() {
    return myText;
  }

  protected void setText(@Nonnull String text) {
    myText = text;
  }

  public boolean startInWriteAction() {
    return true;
  }

  @Override
  public String toString() {
    return getText();
  }

  @Override
  public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getStartElement();
    if (element == null) return;
    invoke(project, element);
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
  public boolean isAvailable(@Nonnull final Project project, @Nonnull final PsiElement context) {
    return context.isValid() && PsiManager.getInstance(project).isInProject(context) && getContainer(context) != null;
  }

  public void invoke(@Nonnull final Project project, @Nonnull final PsiElement element) throws IncorrectOperationException {
    if (!isAvailable(project, element)) return;
    PsiElement container = getContainer(element);
    if (container == null) return;

    if (!FileModificationService.getInstance().preparePsiElementForWrite(container)) return;

    if (replaceSuppressionComments(container)) return;

    createSuppression(project, element, container);
    UndoUtil.markPsiFileForUndo(element.getContainingFile());
  }

  protected boolean replaceSuppressionComments(PsiElement container) {
    final List<? extends PsiElement> comments = getCommentsFor(container);
    if (comments != null) {
      for (PsiElement comment : comments) {
        if (comment instanceof PsiComment && SuppressionUtil.isSuppressionComment(comment)) {
          replaceSuppressionComment(comment);
          return true;
        }
      }
    }
    return false;
  }

  @javax.annotation.Nullable
  protected List<? extends PsiElement> getCommentsFor(@Nonnull final PsiElement container) {
    final PsiElement prev = PsiTreeUtil.skipSiblingsBackward(container, PsiWhiteSpace.class);
    if (prev == null) {
      return null;
    }
    return Collections.singletonList(prev);
  }


  @Override
  @Nonnull
  public String getFamilyName() {
    return InspectionsBundle.message("suppress.inspection.family");
  }
}
