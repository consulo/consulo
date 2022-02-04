package com.intellij.util;


import com.intellij.model.search.impl.TransformationKt;
import com.intellij.model.search.impl.XQuery;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

// from kotlin
@Singleton
public final class QueriesImpl extends Queries {
  @Nonnull
  @Override
  protected <I, O> Query<O> transforming(@Nonnull Query<? extends I> base, @Nonnull Function<? super I, ? extends Collection<? extends O>> transformation) {
    return new XQuery<I, O>(base, TransformationKt.<I, O>xValueTransform(transformation::apply));
  }


  @Nonnull
  @Override
  protected <I, O> Query<O> flatMapping(@Nonnull Query<? extends I> base, @Nonnull Function<? super I, ? extends Query<? extends O>> mapper) {
    return new XQuery<I, O>(base, TransformationKt.<I, O>xQueryTransform((baseValue) -> List.of(mapper.apply(baseValue))));
  }
}



