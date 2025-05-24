// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal.util;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.*;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.util.lang.ObjectUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * @author ven
 */
@Singleton
@ServiceImpl
public final class CachedValuesManagerImpl extends CachedValuesManager {
    private static final Logger LOG = Logger.getInstance(CachedValuesManagerImpl.class);

    private static final Object NULL = ObjectUtil.sentinel("CachedValuesManagerImpl#NULL");

    private ConcurrentMap<UserDataHolder, Object> myCacheHolders = Maps.newConcurrentWeakIdentityMap();
    private Set<Key<?>> myKeys = ContainerUtil.newConcurrentSet();

    private final Project myProject;
    private final CachedValuesFactory myFactory;

    @Inject
    public CachedValuesManagerImpl(@Nonnull Project project, @Nonnull CachedValuesFactory factory) {
        myProject = project;
        myFactory = factory;
    }

    @Override
    @Nonnull
    public <T> CachedValue<T> createCachedValue(@Nonnull CachedValueProvider<T> provider, boolean trackValue) {
        return myFactory.createCachedValue(provider, trackValue);
    }

    @Override
    public
    @Nonnull <T, P> ParameterizedCachedValue<T, P> createParameterizedCachedValue(
        @Nonnull ParameterizedCachedValueProvider<T, P> provider,
        boolean trackValue
    ) {
        return myFactory.createParameterizedCachedValue(provider, trackValue);
    }

    @Override
    public
    @Nullable <T> T getCachedValue(
        @Nonnull UserDataHolder dataHolder,
        @Nonnull Key<CachedValue<T>> key,
        @Nonnull CachedValueProvider<T> provider,
        boolean trackValue
    ) {
        CachedValue<T> value = dataHolder.getUserData(key);
        if (value instanceof CachedValueBase && ((CachedValueBase<?>)value).isFromMyProject(myProject)) {
            Supplier<T> data = value.getUpToDateOrNull();
            if (data != null) {
                return data.get();
            }
            try {
                CachedValueStabilityChecker.checkProvidersEquivalent(provider, value.getValueProvider(), key);
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
        if (value == null) {
            value = saveInUserData(dataHolder, key, freshCachedValue(dataHolder, key, provider, trackValue));
        }
        return value.getValue();
    }

    private <T> CachedValue<T> saveInUserData(@Nonnull UserDataHolder dataHolder, @Nonnull Key<CachedValue<T>> key, CachedValue<T> value) {
        trackKeyHolder(dataHolder, key);

        if (dataHolder instanceof UserDataHolderEx) {
            return dataHolder.putUserDataIfAbsent(key, value);
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (dataHolder) {
            CachedValue<T> existing = dataHolder.getUserData(key);
            if (existing != null) {
                return existing;
            }
            dataHolder.putUserData(key, value);
            return value;
        }
    }

    @Override
    protected void trackKeyHolder(@Nonnull UserDataHolder dataHolder, @Nonnull Key<?> key) {
        if (!isClearedOnPluginUnload(dataHolder)) {
            myCacheHolders.put(dataHolder, NULL);
            myKeys.add(key);
        }
    }

    private static boolean isClearedOnPluginUnload(@Nonnull UserDataHolder dataHolder) {
        return dataHolder instanceof PsiElement || dataHolder instanceof ASTNode || dataHolder instanceof FileViewProvider;
    }

    private <T> CachedValue<T> freshCachedValue(
        UserDataHolder dh,
        Key<CachedValue<T>> key,
        CachedValueProvider<T> provider,
        boolean trackValue
    ) {
        myFactory.checkProviderForMemoryLeak(provider, key, dh);
        CachedValue<T> value = createCachedValue(provider, trackValue);
        assert ((CachedValueBase<?>)value).isFromMyProject(myProject);
        return value;
    }

    public void clearCachedValues() {
        for (UserDataHolder holder : myCacheHolders.keySet()) {
            for (Key<?> key : myKeys) {
                holder.putUserData(key, null);
            }
        }
        CachedValueStabilityChecker.cleanupFieldCache();
        myCacheHolders = Maps.newConcurrentWeakIdentityMap();
        myKeys = ContainerUtil.newConcurrentSet();
    }
}
