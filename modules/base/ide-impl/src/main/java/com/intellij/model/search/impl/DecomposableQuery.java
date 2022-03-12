package com.intellij.model.search.impl;

import consulo.application.util.Query;
import javax.annotation.Nonnull;

// from kotlin
public interface DecomposableQuery<R> extends Query<R> {
  @Nonnull
  Requests<R> decompose();
}

