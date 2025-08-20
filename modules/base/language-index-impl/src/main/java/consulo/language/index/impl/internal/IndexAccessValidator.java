/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.application.ApplicationManager;
import consulo.index.io.ID;
import consulo.logging.Logger;
import consulo.application.util.function.ThrowableComputable;
import jakarta.annotation.Nonnull;

import java.text.MessageFormat;

public class IndexAccessValidator {
  private final ThreadLocal<ID<?, ?>> ourAlreadyProcessingIndices = new ThreadLocal<>();

  private void checkAccessingIndexDuringOtherIndexProcessing(@Nonnull ID<?, ?> indexKey) {
    final ID<?, ?> alreadyProcessingIndex = ourAlreadyProcessingIndices.get();
    if (alreadyProcessingIndex != null && alreadyProcessingIndex != indexKey) {
      final String message = MessageFormat.format("Accessing ''{0}'' during processing ''{1}''. Nested different indices processing may cause deadlock", indexKey.getName(), alreadyProcessingIndex.getName());
      if (ApplicationManager.getApplication().isUnitTestMode()) throw new RuntimeException(message);
      Logger.getInstance(FileBasedIndexImpl.class).error(message); // RuntimeException to skip rebuild
    }
  }

  public <T, E extends Throwable> T validate(@Nonnull ID<?, ?> indexKey, @Nonnull ThrowableComputable<T, E> runnable) throws E {
    checkAccessingIndexDuringOtherIndexProcessing(indexKey);
    ourAlreadyProcessingIndices.set(indexKey);
    try {
      return runnable.compute();
    }
    finally {
      ourAlreadyProcessingIndices.set(null);
    }
  }
}
