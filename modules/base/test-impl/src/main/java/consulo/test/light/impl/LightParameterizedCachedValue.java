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

import consulo.application.util.CachedValueProvider;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.util.lang.ref.SimpleReference;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2023-11-05
 */
public class LightParameterizedCachedValue<T, P> implements ParameterizedCachedValue<T, P> {
    private final ParameterizedCachedValueProvider<T, P> myProvider;

    private Map<P, SimpleReference<T>> myCache = new ConcurrentHashMap<>();

    public LightParameterizedCachedValue(ParameterizedCachedValueProvider<T, P> provider) {
        myProvider = provider;
    }

    @Override
    public T getValue(P param) {
        return myCache.computeIfAbsent(
            param,
            p -> {
                CachedValueProvider.Result<T> result = myProvider.compute(param);
                T value = result == null ? null : result.getValue();
                return new SimpleReference<>(value);
            }
        ).get();
    }

    @Override
    public ParameterizedCachedValueProvider<T, P> getValueProvider() {
        return myProvider;
    }

    @Override
    public boolean hasUpToDateValue() {
        return true;
    }
}
