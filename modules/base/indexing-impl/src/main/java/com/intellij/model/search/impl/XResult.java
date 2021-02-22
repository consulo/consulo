package com.intellij.model.search.impl;

import com.intellij.util.Processor;
import javax.annotation.Nonnull;

import java.util.Collection;

// from kotlin
public abstract class XResult<X> {

  public abstract boolean process(@Nonnull Processor<? super X> processor);

  @Nonnull
  public abstract <R> Collection<? extends XResult<? extends R>> transform(@Nonnull XTransformation<? super X, ? extends R> transformation);
}

