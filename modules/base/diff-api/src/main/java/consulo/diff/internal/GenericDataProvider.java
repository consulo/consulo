// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.diff.internal;

import consulo.dataContext.DataProvider;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GenericDataProvider implements DataProvider {
  private final Map<Key, Object> myGenericData;
  private final DataProvider myParentProvider;

  public GenericDataProvider() {
    this(null);
  }

  public GenericDataProvider(@Nullable DataProvider provider) {
    myParentProvider = provider;
    myGenericData = new HashMap<>();
  }

  public void putData(Key key, Object value) {
    myGenericData.put(key, value);
  }

  @Override
  public Object getData(Key dataId) {
    Object data = myGenericData.get(dataId);
    if (data != null) return data;
    return myParentProvider != null ? myParentProvider.getData(dataId) : null;
  }
}
