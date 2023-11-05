/*
 * Copyright 2013-2023 consulo.io
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
package consulo.test.light.impl;

import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2023-11-05
 */
public class LightCachedValue<T> implements CachedValue<T> {
  private final CachedValueProvider<T> myProvider;

  public LightCachedValue(CachedValueProvider<T> provider) {
    myProvider = provider;
  }

  @Override
  public T getValue() {
    CachedValueProvider.Result<T> compute = myProvider.compute();
    return compute == null ? null : compute.getValue();
  }

  @Nonnull
  @Override
  public CachedValueProvider<T> getValueProvider() {
    return myProvider;
  }

  @Override
  public boolean hasUpToDateValue() {
    return true;
  }

  @Override
  public Supplier<T> getUpToDateOrNull() {
    return this::getValue;
  }
}
