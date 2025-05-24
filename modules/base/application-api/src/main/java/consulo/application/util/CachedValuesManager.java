// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.internal.util.CachedValueManagerHelper;
import consulo.component.ComponentManager;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderEx;

import jakarta.annotation.Nonnull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A service used to create and store {@link CachedValue} objects.<p/>
 * <p>
 * By default cached values are stored in the user data of associated objects implementing {@link UserDataHolder}.
 *
 * @see #createCachedValue(CachedValueProvider, boolean)
 * @see #getCachedValue(PsiElement, CachedValueProvider)
 * @see #getCachedValue(UserDataHolder, CachedValueProvider)
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class CachedValuesManager {
    public static CachedValuesManager getManager(@Nonnull ComponentManager project) {
        return project.getInstance(CachedValuesManager.class);
    }

    /**
     * Creates new CachedValue instance with given provider. If the return value is marked as trackable, it's treated as
     * yet another dependency and must comply its specification. See {@link CachedValueProvider.Result#getDependencyItems()} for
     * the details.
     *
     * @param provider   computes values.
     * @param trackValue if value tracking is required. T should be trackable in this case.
     * @return new CachedValue instance.
     */
    @Nonnull
    public abstract <T> CachedValue<T> createCachedValue(@Nonnull CachedValueProvider<T> provider, boolean trackValue);

    @Nonnull
    public abstract <T, P> ParameterizedCachedValue<T, P> createParameterizedCachedValue(
        @Nonnull ParameterizedCachedValueProvider<T, P> provider,
        boolean trackValue
    );

    /**
     * Creates a new CachedValue instance with the given provider.
     */
    public
    @Nonnull <T> CachedValue<T> createCachedValue(@Nonnull CachedValueProvider<T> provider) {
        return createCachedValue(provider, false);
    }

    public <T, P> T getParameterizedCachedValue(
        @Nonnull UserDataHolder dataHolder,
        @Nonnull Key<ParameterizedCachedValue<T, P>> key,
        @Nonnull ParameterizedCachedValueProvider<T, P> provider,
        boolean trackValue,
        P parameter
    ) {
        ParameterizedCachedValue<T, P> value;

        if (dataHolder instanceof UserDataHolderEx) {
            UserDataHolderEx dh = (UserDataHolderEx)dataHolder;
            value = dh.getUserData(key);
            if (value == null) {
                trackKeyHolder(dataHolder, key);
                value = createParameterizedCachedValue(provider, trackValue);
                value = dh.putUserDataIfAbsent(key, value);
            }
        }
        else {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (dataHolder) {
                value = dataHolder.getUserData(key);
                if (value == null) {
                    trackKeyHolder(dataHolder, key);
                    value = createParameterizedCachedValue(provider, trackValue);
                    dataHolder.putUserData(key, value);
                }
            }
        }
        return value.getValue(parameter);
    }

    protected abstract void trackKeyHolder(@Nonnull UserDataHolder dataHolder, @Nonnull Key<?> key);

    /**
     * Utility method storing created cached values in a {@link UserDataHolder}.
     * The passed cached value provider may only depend on the passed user data holder and longer-living system state (e.g. project/application components/services),
     * see {@link CachedValue} documentation for more details.
     *
     * @param dataHolder holder to store the cached value, e.g. a PsiElement.
     * @param key        key to store the cached value.
     * @param provider   provider creating the cached value.
     * @param trackValue if value tracking is required (T should be trackable in that case). See {@link #createCachedValue(CachedValueProvider, boolean)} for more details.
     * @return up-to-date value.
     */
    public abstract <T> T getCachedValue(
        @Nonnull UserDataHolder dataHolder,
        @Nonnull Key<CachedValue<T>> key,
        @Nonnull CachedValueProvider<T> provider,
        boolean trackValue
    );

    /**
     * Create a cached value with the given provider and non-tracked return value, store it in the first argument's user data. If it's already stored, reuse it.
     * The passed cached value provider may only depend on the passed user data holder and longer-living system state (e.g. project/application components/services),
     * see {@link CachedValue} documentation for more details.
     *
     * @return The cached value
     */
    public <T> T getCachedValue(@Nonnull UserDataHolder dataHolder, @Nonnull CachedValueProvider<T> provider) {
        return getCachedValue(dataHolder, this.getKeyForClass(provider.getClass()), provider, false);
    }

    private final ConcurrentMap<String, Key<CachedValue<?>>> keyForProvider = new ConcurrentHashMap<>();

    @Nonnull
    public <T> Key<CachedValue<T>> getKeyForClass(@Nonnull Class<?> providerClass) {
        return CachedValueManagerHelper.getKeyForClass(providerClass, keyForProvider);
    }
}
