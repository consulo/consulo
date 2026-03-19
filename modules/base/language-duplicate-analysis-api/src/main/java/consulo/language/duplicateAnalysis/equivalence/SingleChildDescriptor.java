package consulo.language.duplicateAnalysis.equivalence;

import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public class SingleChildDescriptor {
  private final MyType myType;
  private final PsiElement myElement;

  public SingleChildDescriptor(MyType type, @Nullable PsiElement element) {
    myType = type;
    myElement = element;
  }

  
  public MyType getType() {
    return myType;
  }

  public @Nullable PsiElement getElement() {
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
