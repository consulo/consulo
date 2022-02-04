package com.intellij.dupLocator.util;

import consulo.language.psi.PsiElement;

/**
 * Base class for tree filtering
 */
public interface NodeFilter {
  boolean accepts(PsiElement element);
}
