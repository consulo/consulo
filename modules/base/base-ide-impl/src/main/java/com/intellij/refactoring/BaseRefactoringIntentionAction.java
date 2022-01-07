package com.intellij.refactoring;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Iconable;
import consulo.ui.image.Image;

/**
 * User: anna
 * Date: 11/11/11
 */
public abstract class BaseRefactoringIntentionAction extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {

  @Override
  public Image getIcon(int flags) {
    return AllIcons.Actions.RefactoringBulb;
  }
}
