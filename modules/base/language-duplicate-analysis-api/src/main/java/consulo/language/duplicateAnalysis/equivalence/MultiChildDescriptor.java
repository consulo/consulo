package consulo.language.duplicateAnalysis.equivalence;

import consulo.language.psi.PsiElement;

/**
 * @author Eugene.Kudelevsky
 */
public class MultiChildDescriptor {
  private final MyType myType;
  private final PsiElement[] myElements;

  public MultiChildDescriptor(MyType type, PsiElement[] elements) {
    myType = type;
    myElements = elements;
  }

  
  public MyType getType() {
    return myType;
  }

  
  public PsiElement[] getElements() {
    return myElements;
  }

  public enum MyType {
    DEFAULT,
    OPTIONALLY,
    OPTIONALLY_IN_PATTERN,
    IN_ANY_ORDER
  }
}
