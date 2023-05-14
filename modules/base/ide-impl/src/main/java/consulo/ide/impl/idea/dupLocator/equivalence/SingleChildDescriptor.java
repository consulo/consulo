package consulo.ide.impl.idea.dupLocator.equivalence;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public class SingleChildDescriptor {
  private final MyType myType;
  private final PsiElement myElement;

  public SingleChildDescriptor(@Nonnull MyType type, @Nullable PsiElement element) {
    myType = type;
    myElement = element;
  }

  @Nonnull
  public MyType getType() {
    return myType;
  }

  @Nullable
  public PsiElement getElement() {
    return myElement;
  }

  public enum MyType {
    DEFAULT,
    OPTIONALLY,
    OPTIONALLY_IN_PATTERN,
    CHILDREN,
    CHILDREN_OPTIONALLY,
    CHILDREN_OPTIONALLY_IN_PATTERN,
    CHILDREN_IN_ANY_ORDER
  }
}
