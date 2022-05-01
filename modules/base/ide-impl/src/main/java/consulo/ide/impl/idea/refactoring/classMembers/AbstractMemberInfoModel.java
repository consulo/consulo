package consulo.ide.impl.idea.refactoring.classMembers;

import consulo.language.psi.PsiElement;
import javax.annotation.Nonnull;

/**
 * Nikolay.Tropin
 * 8/23/13
 */
public abstract class AbstractMemberInfoModel<T extends PsiElement, M extends MemberInfoBase<T>> implements MemberInfoModel<T, M> {

  @Override
  public boolean isMemberEnabled(M member) {
    return true;
  }

  @Override
  public boolean isCheckedWhenDisabled(M member) {
    return false;
  }

  @Override
  public boolean isAbstractEnabled(M member) {
    return false;
  }

  @Override
  public boolean isAbstractWhenDisabled(M member) {
    return false;
  }

  @Override
  public Boolean isFixedAbstract(M member) {
    return null;
  }

  @Override
  public int checkForProblems(@Nonnull M member) {
    return OK;
  }

  @Override
  public String getTooltipText(M member) {
    return null;
  }

  @Override
  public void memberInfoChanged(MemberInfoChange<T, M> event) {
  }
}
