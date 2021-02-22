package com.intellij.model.search.impl;

import com.intellij.util.Query;
import javax.annotation.Nonnull;

// from kotlin
public interface DecomposableQuery<R> extends Query<R> {
  @Nonnull
  Requests<R> decompose();
}

