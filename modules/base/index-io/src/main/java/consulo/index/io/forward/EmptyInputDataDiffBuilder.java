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
package consulo.index.io.forward;

import consulo.index.io.StorageException;
import jakarta.annotation.Nonnull;

import java.util.Map;

public class EmptyInputDataDiffBuilder<Key, Value> extends InputDataDiffBuilder<Key, Value> {
    public EmptyInputDataDiffBuilder(int inputId) {
        super(inputId);
    }

    @Override
    public boolean differentiate(@Nonnull Map<Key, Value> newData,
                                 @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> addProcessor,
                                 @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> updateProcessor,
                                 @Nonnull RemovedKeyProcessor<? super Key> removeProcessor) throws StorageException {
        return processKeys(newData, addProcessor, myInputId);
    }

    public static <Key, Value> boolean processKeys(@Nonnull Map<Key, Value> currentData, @Nonnull KeyValueUpdateProcessor<? super Key, ? super Value> processor, int inputId)
        throws StorageException {
        for (Map.Entry<Key, Value> entry : currentData.entrySet()) {
            processor.process(entry.getKey(), entry.getValue(), inputId);
        }
        return true;
    }
}
