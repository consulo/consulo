package com.intellij.model.search.impl;

import com.intellij.psi.impl.search.LeafOccurrence;
import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.List;

// from kotlin
public class RequestsKt {
  @Nonnull
  public static <T> Requests<T> decompose(@Nonnull Query<T> query) {
    return query instanceof DecomposableQuery
           ? ((DecomposableQuery<T>)query).decompose()
           : new Requests<T>(List.of(), List.of(new QueryRequest<>(query, TransformationKt.<T>idTransform())), List.of());
  }

  @Nonnull
  public static <B, R> Requests<R> andThen(@Nonnull Requests<? extends B> thisRequest, @Nonnull XTransformation<? super B, ? extends R> transformation) {
    List<ParametersRequest<?, ? extends R>> params = ContainerUtil.map(thisRequest.getParametersRequests(), (it) -> RequestsKt.andThen(it, transformation));
    List<QueryRequest<?, ? extends R>> queries = ContainerUtil.map(thisRequest.getQueryRequests(), (it) -> RequestsKt.andThen(it, transformation));
    List<WordRequest<? extends R>> wordRequests = ContainerUtil.map(thisRequest.getWordRequests(), (it) -> RequestsKt.andThen(it, transformation));
    return new Requests<>(params, queries, wordRequests);
  }

  @Nonnull
  public static <B, R> WordRequest<R> andThen(@Nonnull WordRequest<? extends B> thisWordRequest, @Nonnull XTransformation<? super B, ? extends R> t) {
    return new WordRequest<R>(thisWordRequest.getSearchWordRequest(), thisWordRequest.getInjectionInfo(), TransformationKt.<LeafOccurrence, B, R>karasique(thisWordRequest.getTransformation(), t));
  }

  @Nonnull
  public static <B, R, I> QueryRequest<B, R> andThen(@Nonnull QueryRequest<B, ? extends I> thisQuery, @Nonnull XTransformation<? super I, ? extends R> t) {
    return new QueryRequest<B, R>(thisQuery.getQuery(), TransformationKt.<B, I, R>karasique(thisQuery.getTransformation(), t));
  }

  @Nonnull
  public static <B, R, I> ParametersRequest<B, R> andThen(@Nonnull ParametersRequest<B, ? extends I> thisParameters, @Nonnull XTransformation<? super I, ? extends R> t) {
    return new ParametersRequest<B, R>(thisParameters.getParams(), TransformationKt.<B, I, R>karasique(thisParameters.getTransformation(), t));
  }

}
