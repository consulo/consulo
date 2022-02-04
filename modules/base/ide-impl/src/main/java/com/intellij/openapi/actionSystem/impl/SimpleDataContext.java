// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem.impl;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import consulo.annotation.DeprecationInfo;
import consulo.dataContext.DataContext;
import consulo.dataContext.internal.BuilderDataContext;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Deprecated
@DeprecationInfo("Use DataContext#builder()")
public final class SimpleDataContext extends BuilderDataContext implements DataContext {
  private SimpleDataContext(@Nonnull Map<Key, Object> dataId2data, @Nullable DataContext parent) {
    super(dataId2data, parent);
  }

  @Nonnull
  public static <T> DataContext getSimpleContext(@Nonnull Key<? super T> dataKey, @Nonnull T data, @Nullable DataContext parent) {
    return new SimpleDataContext(Map.of(dataKey, data), parent);
  }

  /**
   * @see SimpleDataContext#builder()
   * @deprecated prefer type-safe {@link SimpleDataContext#builder()} where possible.
   */
  @Deprecated
  @Nonnull
  public static DataContext getSimpleContext(@Nonnull Map<Key, Object> dataId2data, @Nullable DataContext parent) {
    return new SimpleDataContext(dataId2data, parent);
  }

  @Nonnull
  public static <T> DataContext getSimpleContext(@Nonnull Key<? super T> dataKey, @Nonnull T data) {
    return getSimpleContext(dataKey, data, null);
  }

  @Nonnull
  public static DataContext getProjectContext(@Nonnull Project project) {
    return getSimpleContext(CommonDataKeys.PROJECT, project);
  }

  @Nonnull
  public static Builder builder() {
    return new Builder(null);
  }

  public final static class Builder {
    private DataContext myParent;
    private Map<Key, Object> myMap;

    Builder(DataContext parent) {
      myParent = parent;
    }

    public Builder setParent(@Nullable DataContext parent) {
      myParent = parent;
      return this;
    }

    @Nonnull
    public <T> Builder add(@Nonnull Key<? super T> dataKey, @Nullable T value) {
      if (value != null) {
        if (myMap == null) myMap = new HashMap<>();
        myMap.put(dataKey, value);
      }
      return this;
    }

    @Nonnull
    public Builder addAll(@Nonnull DataContext dataContext, @Nonnull Key<?>... keys) {
      for (Key<?> key : keys) {
        //noinspection unchecked
        add((Key<Object>)key, dataContext.getData(key));
      }
      return this;
    }

    @Nonnull
    public DataContext build() {
      if (myMap == null && myParent == null) return EMPTY_CONTEXT;
      return new SimpleDataContext(myMap != null ? myMap : Map.of(), myParent);
    }
  }
}
