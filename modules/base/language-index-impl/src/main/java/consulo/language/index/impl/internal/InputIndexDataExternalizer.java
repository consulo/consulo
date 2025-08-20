/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.index.impl.internal;

import consulo.index.io.IndexId;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.KeyDescriptor;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InputIndexDataExternalizer<K> implements DataExternalizer<Collection<K>> {
  private final KeyDescriptor<K> myKeyDescriptor;
  private final IndexId<K, ?> myIndexId;

  public InputIndexDataExternalizer(KeyDescriptor<K> keyDescriptor, IndexId<K, ?> indexId) {
    myKeyDescriptor = keyDescriptor;
    myIndexId = indexId;
  }

  @Override
  public void save(@Nonnull DataOutput out, @Nonnull Collection<K> value) throws IOException {
    try {
      DataInputOutputUtil.writeINT(out, value.size());
      for (K key : value) {
        myKeyDescriptor.save(out, key);
      }
    }
    catch (IllegalArgumentException e) {
      throw new IOException("Error saving data for index " + myIndexId, e);
    }
  }

  @Nonnull
  @Override
  public Collection<K> read(@Nonnull DataInput in) throws IOException {
    try {
      final int size = DataInputOutputUtil.readINT(in);
      final List<K> list = new ArrayList<>(size);
      for (int idx = 0; idx < size; idx++) {
        list.add(myKeyDescriptor.read(in));
      }
      return list;
    }
    catch (IllegalArgumentException e) {
      throw new IOException("Error reading data for index " + myIndexId, e);
    }
  }
}
