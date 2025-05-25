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

import consulo.application.util.CachedValueProvider;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

public class ParameterizedCachedValueImpl<T, P> extends CachedValueBase<T> implements ParameterizedCachedValue<T, P> {
    @Nonnull
    private final Project myProject;
    private final ParameterizedCachedValueProvider<T, P> myProvider;

    ParameterizedCachedValueImpl(
        @Nonnull Project project,
        @Nonnull ParameterizedCachedValueProvider<T, P> provider,
        boolean trackValue,
        CachedValuesFactory factory
    ) {
        super(trackValue, factory);
        myProject = project;
        myProvider = provider;
    }

    @Override
    public T getValue(P param) {
        return getValueWithLock(param);
    }

    @Override
    public boolean isFromMyProject(@Nonnull Project project) {
        return myProject == project;
    }

    @Override
    @Nonnull
    public ParameterizedCachedValueProvider<T, P> getValueProvider() {
        return myProvider;
    }

    @Override
    protected <X> CachedValueProvider.Result<T> doCompute(X param) {
        return myProvider.compute((P)param);
    }
}
