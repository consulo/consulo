/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.psi.util;

import consulo.application.internal.util.CachedValueProfiler;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author UNV
 * @since 2025-05-25
 */
public class FileDependencyAddingCachedValueProvider<T, E extends PsiElement> implements ParameterizedCachedValueProvider<T, E> {
    @Nonnull
    private final ParameterizedCachedValueProvider<T, E> myProvider;

    public FileDependencyAddingCachedValueProvider(@Nonnull ParameterizedCachedValueProvider<T, E> provider) {
        myProvider = provider;
    }

    @Nullable
    @Override
    public CachedValueProvider.Result<T> compute(E context) {
        CachedValueProvider.Result<T> result = myProvider.compute(context);
        if (result != null && !context.isPhysical()) {
            PsiFile file = context.getContainingFile();
            if (file != null) {
                CachedValueProvider.Result<T> adjusted = result.addSingleDependency(file);
                CachedValueProfiler.onResultCreated(adjusted, result);
                return adjusted;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return myProvider.toString();
    }
}
