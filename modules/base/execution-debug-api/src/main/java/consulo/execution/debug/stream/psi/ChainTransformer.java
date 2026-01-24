// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.psi;

import consulo.execution.debug.stream.wrapper.StreamChain;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface ChainTransformer<T extends PsiElement> {
  @Nonnull
  StreamChain transform(@Nonnull List<T> callChain, @Nonnull PsiElement context);


}
