package com.intellij.model.search.impl;

import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

// from kotlin
public final class TransformationKt {
  @Nonnull
  @SuppressWarnings("unchecked")
  public static <R> XTransformation<? super R, ? extends R> idTransform() {
    return (XTransformation<R, R>)IdTransformation.INSTANCE;
  }

  @Nonnull
  public static <B, R> XTransformation<? super B, ? extends R> xValueTransform(@Nonnull Function<? super B, ? extends Collection<? extends R>> transformation) {
    return (baseValue) -> ContainerUtil.map(transformation.apply(baseValue), ValueResult::new);
  }

  @Nonnull
  public static <B, R> XTransformation<? super B, ? extends R> xQueryTransform(@Nonnull Function<? super B, ? extends Collection<Query<? extends R>>> subQueries) {
    return (baseValue) -> ContainerUtil.map(subQueries.apply(baseValue), QueryResult::new);
  }

  /**
   * (>=>) :: (b -> x i) -> (i -> x r) -> (b -> x r)
   */
  @Nonnull
  @SuppressWarnings("unchecked")
  public static <B, I, R> XTransformation<? super B, ? extends R> karasique(XTransformation<? super B, ? extends I> thisTrans, XTransformation<? super I, ? extends R> next) {
    if (thisTrans == IdTransformation.INSTANCE) {
      return (XTransformation<B, R>)next;
    }
    else if (next == IdTransformation.INSTANCE) {
      return (XTransformation<B, R>)thisTrans;
    }
    else {
      return baseValue -> {
        Collection<? extends XResult<? extends I>> intermediateResults = thisTrans.apply(baseValue);

        List<? extends XResult<? extends R>> results = new ArrayList<>();

        for (XResult result : intermediateResults) {
          Collection apply = (Collection)((XTransformation) next).apply(result);

          results.addAll(apply);
        }

        return results;
      };
    }
  }
}
