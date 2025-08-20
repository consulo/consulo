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

import consulo.index.io.StorageException;
import consulo.index.io.forward.EmptyInputDataDiffBuilder;
import consulo.index.io.forward.InputDataDiffBuilder;
import consulo.index.io.forward.KeyValueUpdateProcessor;
import consulo.index.io.forward.RemovedKeyProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CollectionInputDataDiffBuilder<Key, Value> extends InputDataDiffBuilder<Key, Value> {
  private final Collection<Key> mySeq;

  public CollectionInputDataDiffBuilder(int inputId, @Nullable Collection<Key> seq) {
    super(inputId);
    mySeq = seq == null ? Collections.emptySet() : seq;
  }

  @Override
  public boolean differentiate(@Nonnull Map<Key, Value> newData,
                               @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> addProcessor,
                               @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> updateProcessor,
                               @Nonnull RemovedKeyProcessor<? super Key> removeProcessor) throws StorageException {
    return differentiateWithKeySeq(mySeq, newData, myInputId, addProcessor, removeProcessor);
  }

  public Collection<Key> getSeq() {
    return mySeq;
  }

  static <Key, Value> boolean differentiateWithKeySeq(@Nonnull Collection<? extends Key> currentData,
                                                      @Nonnull Map<Key, Value> newData,
                                                      int inputId,
                                                      @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> addProcessor,
                                                      @Nonnull RemovedKeyProcessor<? super Key> removeProcessor) throws StorageException {
    for (Key key : currentData) {
      removeProcessor.process(key, inputId);
    }
    EmptyInputDataDiffBuilder.processKeys(newData, addProcessor, inputId);
    return true;
  }
}
