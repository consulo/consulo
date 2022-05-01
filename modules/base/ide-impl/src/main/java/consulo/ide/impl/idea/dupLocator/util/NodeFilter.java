package consulo.ide.impl.idea.dupLocator.util;

import consulo.language.psi.PsiElement;

/**
 * Base class for tree filtering
 */
public interface NodeFilter {
  boolean accepts(PsiElement element);
}
