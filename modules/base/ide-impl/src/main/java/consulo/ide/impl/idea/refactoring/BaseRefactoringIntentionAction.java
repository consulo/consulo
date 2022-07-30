package consulo.ide.impl.idea.refactoring;

import consulo.language.editor.intention.HighPriorityAction;
import consulo.language.editor.intention.PsiElementBaseIntentionAction;
import consulo.application.AllIcons;
import consulo.component.util.Iconable;
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
