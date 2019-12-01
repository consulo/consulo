package com.intellij.dupLocator;

import javax.annotation.Nonnull;

/**
 * @author Eugene.Kudelevsky
 */
public interface ExternalizableDuplocatorState extends DuplocatorState {
  boolean distinguishRole(@Nonnull PsiElementRole role);

  boolean distinguishLiterals();
}
