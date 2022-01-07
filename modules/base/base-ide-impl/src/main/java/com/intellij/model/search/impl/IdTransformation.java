package com.intellij.model.search.impl;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

// from kotlin
final class IdTransformation implements XTransformation {
  public static IdTransformation INSTANCE = new IdTransformation();

  private IdTransformation() {
  }

  @Nonnull
  public Collection<XResult<Object>> apply(@Nonnull Object e) {
    return List.of(new ValueResult<>(e));
  }

  @Nonnull
  public String toString() {
    return "ID";
  }
}
