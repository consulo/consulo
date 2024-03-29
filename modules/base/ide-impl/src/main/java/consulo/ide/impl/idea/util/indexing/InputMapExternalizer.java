// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing;

import consulo.index.io.IndexExtension;
import consulo.util.collection.SmartList;
import consulo.ide.impl.idea.util.indexing.impl.InputIndexDataExternalizer;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;

import jakarta.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class InputMapExternalizer<Key, Value> implements DataExternalizer<Map<Key, Value>> {
  private final DataExternalizer<Value> myValueExternalizer;
  private final DataExternalizer<Collection<Key>> mySnapshotIndexExternalizer;

  public InputMapExternalizer(IndexExtension<Key, Value, ?> extension) {
    myValueExternalizer = extension.getValueExternalizer();
    mySnapshotIndexExternalizer = extension instanceof CustomInputsIndexFileBasedIndexExtension
                                  ? ((CustomInputsIndexFileBasedIndexExtension<Key>)extension).createExternalizer()
                                  : new InputIndexDataExternalizer<>(extension.getKeyDescriptor(), ((IndexExtension<Key, ?, ?>)extension).getName());
  }


  @Nonnull
  public DataExternalizer<Value> getValueExternalizer() {
    return myValueExternalizer;
  }

  @Override
  public void save(@Nonnull DataOutput stream, Map<Key, Value> data) throws IOException {
    int size = data.size();
    DataInputOutputUtil.writeINT(stream, size);

    if (size > 0) {
      Map<Value, List<Key>> values = new HashMap<>();
      List<Key> keysForNullValue = null;
      for (Map.Entry<Key, Value> e : data.entrySet()) {
        Value value = e.getValue();

        List<Key> keys = value != null ? values.get(value) : keysForNullValue;
        if (keys == null) {
          if (value != null) values.put(value, keys = new SmartList<>());
          else keys = keysForNullValue = new SmartList<>();
        }
        keys.add(e.getKey());
      }

      if (keysForNullValue != null) {
        myValueExternalizer.save(stream, null);
        mySnapshotIndexExternalizer.save(stream, keysForNullValue);
      }

      for (Value value : values.keySet()) {
        myValueExternalizer.save(stream, value);
        mySnapshotIndexExternalizer.save(stream, values.get(value));
      }
    }
  }

  @Override
  public Map<Key, Value> read(@Nonnull DataInput in) throws IOException {
    int pairs = DataInputOutputUtil.readINT(in);
    if (pairs == 0) return Collections.emptyMap();
    Map<Key, Value> result = new HashMap<>(pairs);
    while (((InputStream)in).available() > 0) {
      Value value = myValueExternalizer.read(in);
      Collection<Key> keys = mySnapshotIndexExternalizer.read(in);
      for (Key k : keys) result.put(k, value);
    }
    return result;
  }
}
