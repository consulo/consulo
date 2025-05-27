/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.application.impl.internal.util;

import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

public class CachedValueImpl<T> extends CachedValueBase<T> implements CachedValue<T> {
    private final CachedValueProvider<T> myProvider;

    public CachedValueImpl(@Nonnull CachedValueProvider<T> provider, CachedValuesFactory factory) {
        this(provider, false, factory);
    }

    CachedValueImpl(@Nonnull CachedValueProvider<T> provider, boolean trackValue, CachedValuesFactory factory) {
        super(trackValue, factory);
        myProvider = provider;
    }

    @Override
    protected <P> CachedValueProvider.Result<T> doCompute(P param) {
        return myProvider.compute();
    }

    @Nonnull
    @Override
    public CachedValueProvider<T> getValueProvider() {
        return myProvider;
    }

    @Override
    public T getValue() {
        return getValueWithLock(null);
    }

    @Override
    public boolean isFromMyProject(@Nonnull Project project) {
        return true;
    }
}
