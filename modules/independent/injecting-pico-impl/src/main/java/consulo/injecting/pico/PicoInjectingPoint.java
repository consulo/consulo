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
package consulo.injecting.pico;

import consulo.injecting.InjectingPoint;
import consulo.injecting.key.InjectingKey;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public class PicoInjectingPoint<T> implements InjectingPoint<T> {
  private final InjectingKey<T> myKey;
  private final PicoInjectingContainer myContainer;

  private boolean myLocked;

  public PicoInjectingPoint(InjectingKey<T> key, PicoInjectingContainer container) {
    myKey = key;
    myContainer = container;
  }

  @Nonnull
  @Override
  public InjectingPoint<T> to(@Nonnull T value) {
    if (myLocked) {
      throw new IllegalArgumentException("locked");
    }

    myLocked = true;
    myContainer.getContainer().registerComponentInstance(myKey.getTargetClassName(), value);
    return this;
  }

  @Nonnull
  @Override
  public InjectingPoint<T> to(@Nonnull InjectingKey<T> key) {
    if (myLocked) {
      throw new IllegalArgumentException("locked");
    }

    myLocked = true;
    myContainer.getContainer().registerComponent(new LazyComponentAdapter(key));
    return this;
  }

  @Nonnull
  @Override
  public InjectingPoint<T> injectListener(@Nonnull Consumer<T> consumer) {
    throw new UnsupportedOperationException();
  }
}
