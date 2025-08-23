package consulo.language.duplicateAnalysis.util;

import consulo.language.psi.PsiElement;

/**
 * Base class for tree filtering
 */
public interface NodeFilter {
  boolean accepts(PsiElement element);
}
