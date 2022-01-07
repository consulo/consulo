package com.intellij.model.search.impl;

import com.intellij.psi.impl.search.LeafOccurrence;
import com.intellij.psi.impl.search.WordRequestInfoImpl;
import javax.annotation.Nonnull;

// from kotlin
public final class WordRequest<R> {
  private WordRequestInfoImpl searchWordRequest;

  private InjectionInfo injectionInfo;

  private XTransformation<? super LeafOccurrence, ? extends R> transformation;

  public WordRequest(@Nonnull WordRequestInfoImpl searchWordRequest, @Nonnull InjectionInfo injectionInfo, @Nonnull XTransformation<? super LeafOccurrence, ? extends R> transformation) {
    this.searchWordRequest = searchWordRequest;
    this.injectionInfo = injectionInfo;
    this.transformation = transformation;
  }

  @Nonnull
  public final WordRequestInfoImpl getSearchWordRequest() {
    return searchWordRequest;
  }

  @Nonnull
  public final InjectionInfo getInjectionInfo() {
    return injectionInfo;
  }

  @Nonnull
  public final XTransformation<? super LeafOccurrence, ? extends R> getTransformation() {
    return transformation;
  }
}
