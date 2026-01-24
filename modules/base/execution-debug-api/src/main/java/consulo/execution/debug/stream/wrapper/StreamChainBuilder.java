// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.wrapper;

import consulo.language.psi.PsiElement;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface StreamChainBuilder {
  /**
   * Check that a chain for debugging exists (the method should be allocation-free)
   * Will be called from a background thread in `Action.update`, so should be fast
   *
   * @param startElement a psi element
   * @return true if chain was found, false otherwise
   */
  boolean isChainExists(PsiElement startElement);

  /**
   * Called under readAction
   */
  List<StreamChain> build(PsiElement startElement);
}
