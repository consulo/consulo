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
package consulo.index.io;

import consulo.index.io.data.DataExternalizer;
import consulo.index.io.internal.ValueContainerImpl;

import jakarta.annotation.Nonnull;
import java.io.*;
import java.util.function.IntUnaryOperator;

/**
 * @author Dmitry Avdeev
 */
class ValueContainerMap<Key, Value> extends PersistentHashMap<Key, UpdatableValueContainer<Value>> {
  @Nonnull
  private final DataExternalizer<Value> myValueExternalizer;
  private final boolean myKeyIsUniqueForIndexedFile;

  ValueContainerMap(@Nonnull File file,
                    @Nonnull KeyDescriptor<Key> keyKeyDescriptor,
                    @Nonnull DataExternalizer<Value> valueExternalizer,
                    boolean keyIsUniqueForIndexedFile,
                    @Nonnull IntUnaryOperator inputRemapping) throws IOException {
    super(file, keyKeyDescriptor, new ValueContainerExternalizer<>(valueExternalizer, inputRemapping));
    myValueExternalizer = valueExternalizer;
    myKeyIsUniqueForIndexedFile = keyIsUniqueForIndexedFile;
  }

  @Nonnull
  Object getDataAccessLock() {
    return myEnumerator;
  }

  @Override
  protected void doPut(Key key, UpdatableValueContainer<Value> container) throws IOException {
    synchronized (myEnumerator) {
      ChangeTrackingValueContainer<Value> valueContainer = (ChangeTrackingValueContainer<Value>)container;

      // try to accumulate index value calculated for particular key to avoid fragmentation: usually keys are scattered across many files
      // note that keys unique for indexed file have their value calculated at once (e.g. key is file id, index calculates something for particular
      // file) and there is no benefit to accumulate values for particular key because only one value exists
      if (!valueContainer.needsCompacting() && !myKeyIsUniqueForIndexedFile) {
        appendData(key, out -> valueContainer.saveTo(out, myValueExternalizer));
      }
      else {
        // rewrite the value container for defragmentation
        super.doPut(key, valueContainer);
      }
    }
  }

  private static final class ValueContainerExternalizer<T> implements DataExternalizer<UpdatableValueContainer<T>> {
    @Nonnull
    private final DataExternalizer<T> myValueExternalizer;
    @Nonnull
    private final IntUnaryOperator myInputRemapping;

    private ValueContainerExternalizer(@Nonnull DataExternalizer<T> valueExternalizer, @Nonnull IntUnaryOperator inputRemapping) {
      myValueExternalizer = valueExternalizer;
      myInputRemapping = inputRemapping;
    }

    @Override
    public void save(@Nonnull DataOutput out, @Nonnull UpdatableValueContainer<T> container) throws IOException {
      container.saveTo(out, myValueExternalizer);
    }

    @Nonnull
    @Override
    public UpdatableValueContainer<T> read(@Nonnull DataInput in) throws IOException {
      ValueContainerImpl<T> valueContainer = new ValueContainerImpl<>();

      valueContainer.readFrom((DataInputStream)in, myValueExternalizer, myInputRemapping);
      return valueContainer;
    }
  }
}
