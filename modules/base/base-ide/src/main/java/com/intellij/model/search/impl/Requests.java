package com.intellij.model.search.impl;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// from kotlin
public final class Requests<R> {
  private static Requests empty = new Requests<>();

  private Collection<ParametersRequest<?, ? extends R>> parametersRequests;

  private Collection<QueryRequest<?, ? extends R>> queryRequests;

  private Collection<WordRequest<? extends R>> wordRequests;

  public Requests(@Nonnull Collection<ParametersRequest<?, ? extends R>> parametersRequests,
                  @Nonnull Collection<QueryRequest<?, ? extends R>> queryRequests,
                  @Nonnull Collection<WordRequest<? extends R>> wordRequests) {
    this.parametersRequests = parametersRequests;
    this.queryRequests = queryRequests;
    this.wordRequests = wordRequests;
  }

  public Requests() {
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T> Requests<T> empty() {
    return (Requests<T>)empty;
  }

  @Nonnull
  public static <T> Requests<T> plus(@Nonnull Requests<T> thisPlus, @Nonnull Requests<T> other) {
    if (thisPlus == empty) {
      return other;
    }
    else {
      if (other == empty) {
        return thisPlus;
      }
      else {
        List<ParametersRequest<?, ? extends T>> params = mergeAll(thisPlus.getParametersRequests(), other.getParametersRequests());
        List<QueryRequest<?, ? extends T>> requests = mergeAll(thisPlus.getQueryRequests(), other.getQueryRequests());
        List<WordRequest<? extends T>> wordRequests = mergeAll(thisPlus.getWordRequests(), other.getWordRequests());
        return new Requests<>(params, requests, wordRequests);
      }
    }
  }

  @Nonnull
  private static <T> List<T> mergeAll(Collection<T> p1, Collection<T> p2) {
    List<T> list = new ArrayList<>(p1.size() + p2.size());
    list.addAll(p1);
    list.addAll(p2);
    return list;
  }

  @Nonnull
  public final Collection<ParametersRequest<?, ? extends R>> getParametersRequests() {
    return parametersRequests;
  }

  @Nonnull
  public final Collection<QueryRequest<?, ? extends R>> getQueryRequests() {
    return queryRequests;
  }

  @Nonnull
  public final Collection<WordRequest<? extends R>> getWordRequests() {
    return wordRequests;
  }
}