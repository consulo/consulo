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

import consulo.index.io.forward.InputDataDiffBuilder;
import consulo.index.io.forward.KeyValueUpdateProcessor;
import consulo.index.io.forward.RemovedKeyProcessor;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.function.ThrowableSupplier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Map;

public class UpdateData<Key, Value> extends AbstractUpdateData<Key, Value> {
  private final Map<Key, Value> myNewData;
  private final ThrowableSupplier<InputDataDiffBuilder<Key, Value>, IOException> myCurrentDataEvaluator;
  private final IndexId<Key, Value> myIndexId;
  private final ThrowableRunnable<? extends IOException> myForwardIndexUpdate;

  public UpdateData(int inputId,
                    @Nonnull Map<Key, Value> newData,
                    @Nonnull ThrowableSupplier<InputDataDiffBuilder<Key, Value>, IOException> currentDataEvaluator,
                    @Nonnull IndexId<Key, Value> indexId,
                    @Nullable ThrowableRunnable<? extends IOException> forwardIndexUpdate) {
    super(inputId);
    myNewData = newData;
    myCurrentDataEvaluator = currentDataEvaluator;
    myIndexId = indexId;
    myForwardIndexUpdate = forwardIndexUpdate;
  }

  @Override
  protected boolean iterateKeys(@Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> addProcessor,
                                @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> updateProcessor,
                                @Nonnull RemovedKeyProcessor<? super Key> removeProcessor) throws StorageException {
    InputDataDiffBuilder<Key, Value> currentData;
    try {
      currentData = getCurrentDataEvaluator().get();
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
    return currentData.differentiate(myNewData, addProcessor, updateProcessor, removeProcessor);
  }

  @Override
  public boolean newDataIsEmpty() {
    return myNewData.isEmpty();
  }

  @Nonnull
  protected ThrowableSupplier<InputDataDiffBuilder<Key, Value>, IOException> getCurrentDataEvaluator() {
    return myCurrentDataEvaluator;
  }

  @Override
  protected void updateForwardIndex() throws IOException {
    if (myForwardIndexUpdate != null) {
      myForwardIndexUpdate.run();
    }
  }

  @Override
  public String toString() {
    return myIndexId + "," + getClass().getName();
  }
}
