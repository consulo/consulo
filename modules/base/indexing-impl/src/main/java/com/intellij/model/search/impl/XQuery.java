package com.intellij.model.search.impl;

import com.intellij.util.Processor;
import com.intellij.util.Query;
import javax.annotation.Nonnull;

// from kotlin
public final class XQuery<B, R> extends AbstractDecomposableQuery<R> {
  private Query<? extends B> baseQuery;

  private XTransformation<? super B, ? extends R> transformation;

  public XQuery(@Nonnull Query<? extends B> baseQuery, @Nonnull XTransformation<? super B, ? extends R> transformation) {
    super();
    this.baseQuery = baseQuery;
    this.transformation = transformation;
  }

  protected boolean processResults(@Nonnull Processor<? super R> consumer) {
    return baseQuery.forEach(baseValue -> {
      for (XResult<? extends R> result : transformation.apply(baseValue)) {
        if (!result.process(consumer)) {
          return false;
        }
      }

      return true;
    });
  }

  @Nonnull
  public Requests<R> decompose() {
    return RequestsKt.andThen(RequestsKt.decompose(baseQuery), transformation);
  }
}
