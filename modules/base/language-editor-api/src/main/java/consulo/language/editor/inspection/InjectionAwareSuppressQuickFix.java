package consulo.language.editor.inspection;

import consulo.language.psi.PsiElement;
import consulo.util.lang.ThreeState;

import jakarta.annotation.Nonnull;

/**
 * This kind of suppression fix allows to clients to specify whether the fix should
 * be invoked on injected elements or on elements of host files.
 * <p/>
 * By default suppression fixes on injected elements are able to make suppression inside injection only.
 * Whereas implementation of this interface will be provided for suppressing inside injection and in injection host.
 * See {@link InspectionTool#getBatchSuppressActions(PsiElement)} for details.
 */
public interface InjectionAwareSuppressQuickFix extends SuppressQuickFix {
  @Nonnull
  ThreeState isShouldBeAppliedToInjectionHost();

  void setShouldBeAppliedToInjectionHost(@Nonnull ThreeState shouldBeAppliedToInjectionHost);
}
