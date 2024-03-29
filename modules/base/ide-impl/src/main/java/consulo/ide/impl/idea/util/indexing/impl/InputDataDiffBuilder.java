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
package consulo.ide.impl.idea.util.indexing.impl;

import consulo.index.io.StorageException;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * A class intended to make a diff between existing forward index data and new one.
 */
//@ApiStatus.Experimental
public abstract class InputDataDiffBuilder<Key, Value> {
  protected final int myInputId;

  protected InputDataDiffBuilder(int id) {
    myInputId = id;
  }

  /**
   * produce a diff between existing data and newData and consume result to addProcessor, updateProcessor and removeProcessor.
   *
   * @return false if there is no difference and true otherwise
   */
  public abstract boolean differentiate(@Nonnull Map<Key, Value> newData,
                                        @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> addProcessor,
                                        @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> updateProcessor,
                                        @Nonnull RemovedKeyProcessor<? super Key> removeProcessor) throws StorageException;
}
