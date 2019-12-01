package com.intellij.dupLocator.equivalence;

import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
