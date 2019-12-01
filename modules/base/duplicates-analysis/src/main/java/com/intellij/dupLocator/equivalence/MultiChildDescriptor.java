package com.intellij.dupLocator.equivalence;

import com.intellij.psi.PsiElement;
import javax.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
public class MultiChildDescriptor {
  private final MyType myType;
  private final PsiElement[] myElements;

  public MultiChildDescriptor(@Nonnull MyType type, @Nonnull PsiElement[] elements) {
    myType = type;
    myElements = elements;
  }

  @Nonnull
  public MyType getType() {
    return myType;
  }

  @Nonnull
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
