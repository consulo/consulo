// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.annotation.DeprecationInfo;
import consulo.dataContext.DataContext;
import consulo.dataContext.internal.BuilderDataContext;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Deprecated
@DeprecationInfo("Use DataContext#builder()")
public final class SimpleDataContext extends BuilderDataContext implements DataContext {
  private SimpleDataContext(Map<Key, Object> dataId2data, @Nullable DataContext parent) {
    super(dataId2data, parent);
  }

  
  public static <T> DataContext getSimpleContext(Key<? super T> dataKey, T data, @Nullable DataContext parent) {
    return new SimpleDataContext(Map.of(dataKey, data), parent);
  }

  /**
   * @see DataContext#builder()
   * @deprecated prefer type-safe {@link DataContext#builder()} where possible.
   */
  @Deprecated
  
  public static DataContext getSimpleContext(Map<Key, Object> dataId2data, @Nullable DataContext parent) {
    return new SimpleDataContext(dataId2data, parent);
  }

  
  public static <T> DataContext getSimpleContext(Key<? super T> dataKey, T data) {
    return getSimpleContext(dataKey, data, null);
  }

  
  public static DataContext getProjectContext(Project project) {
    return getSimpleContext(Project.KEY, project);
  }

  
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

    
    public <T> Builder add(Key<? super T> dataKey, @Nullable T value) {
      if (value != null) {
        if (myMap == null) myMap = new HashMap<>();
        myMap.put(dataKey, value);
      }
      return this;
    }

    
    public Builder addAll(DataContext dataContext, Key<?>... keys) {
      for (Key<?> key : keys) {
        //noinspection unchecked
        add((Key<Object>)key, dataContext.getData(key));
      }
      return this;
    }

    
    public DataContext build() {
      if (myMap == null && myParent == null) return EMPTY_CONTEXT;
      return new SimpleDataContext(myMap != null ? myMap : Map.of(), myParent);
    }
  }
}
