// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util;

import com.intellij.openapi.application.Application;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.Function;

/**
 * This class is intentionally package local.
 */
public abstract class Queries {
  @Nonnull
  static Queries getInstance() {
    return Application.get().getInstance(Queries.class);
  }

  @Nonnull
  protected abstract <I, O> Query<O> transforming(@Nonnull Query<? extends I> base, @Nonnull Function<? super I, ? extends Collection<? extends O>> transformation);

  @Nonnull
  protected abstract <I, O> Query<O> flatMapping(@Nonnull Query<? extends I> base, @Nonnull Function<? super I, ? extends Query<? extends O>> mapper);
}
