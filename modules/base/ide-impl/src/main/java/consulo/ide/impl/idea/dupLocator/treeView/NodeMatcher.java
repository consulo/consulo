package consulo.ide.impl.idea.dupLocator.treeView;

import consulo.language.psi.PsiElement;

public interface NodeMatcher {
  boolean match(PsiElement node);
}
