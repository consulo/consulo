// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.diff.internal;

import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.util.dataholder.Key;

import java.util.HashMap;
import java.util.Map;

public class GenericDataProvider implements UiDataProvider {
  private final Map<Key, Object> myGenericData;

  public GenericDataProvider() {
    myGenericData = new HashMap<>();
  }

  public void putData(Key key, Object value) {
    myGenericData.put(key, value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void uiDataSnapshot(DataSink sink) {
    for (Map.Entry<Key, Object> entry : myGenericData.entrySet()) {
      sink.set(entry.getKey(), entry.getValue());
    }
  }
}
