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

import consulo.application.util.CachedValuesManager;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.language.psi.PsiElement;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

/**
 * @author UNV
 * @since 2025-05-25
 */
public class LanguageCachedValueTemplate<T, E extends PsiElement> implements CachedValuesManager.ValueTemplate<T, E> {
    private Key<ParameterizedCachedValue<T, E>> myKey = null;
    @Nonnull
    private final ParameterizedCachedValueProvider<T, E> myProvider;

    private LanguageCachedValueTemplate(@Nonnull ParameterizedCachedValueProvider<T, E> provider) {
        myProvider = new FileDependencyAddingCachedValueProvider<>(provider);
    }

    public static <T, E extends PsiElement> LanguageCachedValueTemplate<T, E> of(ParameterizedCachedValueProvider<T, E> provider) {
        return new LanguageCachedValueTemplate<>(provider);
    }

    @Nonnull
    @Override
    public Key<ParameterizedCachedValue<T, E>> getKey() {
        if (myKey != null) {
            return myKey;
        }
        synchronized (LanguageCachedValueTemplate.class) {
            if (myKey == null) {
                myKey = Key.create(myProvider.getClass().getName());
            }
            return myKey;
        }
    }

    @Nonnull
    @Override
    public ParameterizedCachedValueProvider<T, E> getProvider() {
        return myProvider;
    }

    @Override
    public boolean isTrackValue() {
        return false;
    }
}
