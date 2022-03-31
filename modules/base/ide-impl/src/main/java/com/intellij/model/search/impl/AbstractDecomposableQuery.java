package com.intellij.model.search.impl;

import consulo.application.util.query.AbstractQuery;

// from kotlin
public abstract class AbstractDecomposableQuery<T> extends AbstractQuery<T> implements DecomposableQuery<T> {
  public AbstractDecomposableQuery() {
  }
}
