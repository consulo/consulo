// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.psi.impl;

import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.ParameterizedCachedValue;
import com.intellij.psi.util.ParameterizedCachedValueProvider;
import com.intellij.util.CachedValuesFactory;
import javax.annotation.Nonnull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author Dmitry Avdeev
 */
@Singleton
public final class PsiCachedValuesFactory implements CachedValuesFactory {
  private final PsiManager myManager;

  @Inject
  public PsiCachedValuesFactory(@Nonnull PsiManager psiManager) {
    myManager = psiManager;
  }

  @Nonnull
  @Override
  public <T> CachedValue<T> createCachedValue(@Nonnull CachedValueProvider<T> provider, boolean trackValue) {
    return new PsiCachedValueImpl<>(myManager, provider, trackValue);
  }

  @Nonnull
  @Override
  public <T, P> ParameterizedCachedValue<T, P> createParameterizedCachedValue(@Nonnull ParameterizedCachedValueProvider<T, P> provider, boolean trackValue) {
    return new PsiParameterizedCachedValue<>(myManager, provider, trackValue);
  }
}
