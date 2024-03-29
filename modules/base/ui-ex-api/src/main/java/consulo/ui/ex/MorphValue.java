/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.ex;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-09-28
 */
public class MorphValue<V> {
  private static final UIModificationTracker ourTracker = UIModificationTracker.getInstance();

  @Nonnull
  public static <K> MorphValue<K> of(@Nonnull Supplier<K> func) {
    return new MorphValue<>(func);
  }

  private final Supplier<V> myValueProducer;

  private V myLastComputedValue;

  private long myLastModificationCount;

  private MorphValue(Supplier<V> valueProducer) {
    myValueProducer = valueProducer;
    myLastModificationCount = UIModificationTracker.getInstance().getModificationCount();
    myLastComputedValue = valueProducer.get();
  }

  @Nonnull
  public V getValue() {
    long modificationCount = ourTracker.getModificationCount();
    if (myLastModificationCount == modificationCount) {
      return myLastComputedValue;
    }

    myLastModificationCount = modificationCount;
    return myLastComputedValue = myValueProducer.get();
  }
}
