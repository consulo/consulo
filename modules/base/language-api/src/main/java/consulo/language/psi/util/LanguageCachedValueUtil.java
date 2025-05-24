/*
 * Copyright 2013-2022 consulo.io
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

import consulo.application.internal.util.CachedValueManagerHelper;
import consulo.application.internal.util.CachedValueProfiler;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 24-Apr-22
 */
public class LanguageCachedValueUtil {
    /**
     * Create a cached value with the given provider and non-tracked return value, store it in PSI element's user data.
     * If it's already stored, reuse it.
     * The passed cached value provider may only depend on the passed context PSI element and project/application components/services,
     * see {@link CachedValue} documentation for more details.
     *
     * @return The cached value
     */
    public static <T> T getCachedValue(final @Nonnull PsiElement context, final @Nonnull CachedValueProvider<T> provider) {
        return getCachedValue(context, CachedValueManagerHelper.getKeyForClass(provider.getClass()), provider);
    }

    /**
     * Create a cached value with the given provider, store it in PSI element's user data. If it's already stored, reuse it.
     * Invalidate on any PSI change in the project.
     * The passed cached value provider may only depend on the passed context PSI element and project/application components/services,
     * see {@link CachedValue} documentation for more details.
     *
     * @return The cached value
     */
    public static <E extends PsiElement, T> T getProjectPsiDependentCache(
        @Nonnull E context,
        @Nonnull Function<? super E, ? extends T> provider
    ) {
        return getCachedValue(context, CachedValueManagerHelper.getKeyForClass(provider.getClass()), () -> {
            CachedValueProvider.Result<? extends T> result =
                CachedValueProvider.Result.create(provider.apply(context), PsiModificationTracker.MODIFICATION_COUNT);
            CachedValueProfiler.onResultCreated(result, provider);
            return result;
        });
    }

    /**
     * Create a cached value with the given provider and non-tracked return value, store it in PSI element's user data.
     * If it's already stored, reuse it.
     * The passed cached value provider may only depend on the passed context PSI element and project/application components/services,
     * see {@link CachedValue} documentation for more details.
     *
     * @return The cached value
     */
    public static <T> T getCachedValue(
        final @Nonnull PsiElement context,
        @Nonnull Key<CachedValue<T>> key,
        final @Nonnull CachedValueProvider<T> provider
    ) {
        CachedValue<T> value = context.getUserData(key);
        if (value != null) {
            Supplier<T> data = value.getUpToDateOrNull();
            if (data != null) {
                return data.get();
            }
        }

        return CachedValuesManager.getManager(context.getProject()).getCachedValue(
            context,
            key,
            new CachedValueProvider<T>() {
                @Override
                public
                @Nullable
                Result<T> compute() {
                    CachedValueProvider.Result<T> result = provider.compute();
                    if (result != null && !context.isPhysical()) {
                        PsiFile file = context.getContainingFile();
                        if (file != null) {
                            Result<T> adjusted = Result.create(
                                result.getValue(),
                                ArrayUtil.append(result.getDependencyItems(), file, ArrayUtil.OBJECT_ARRAY_FACTORY)
                            );
                            CachedValueProfiler.onResultCreated(adjusted, result);
                            return adjusted;
                        }
                    }
                    return result;
                }

                @Override
                public String toString() {
                    return provider.toString();
                }
            },
            false
        );
    }
}
