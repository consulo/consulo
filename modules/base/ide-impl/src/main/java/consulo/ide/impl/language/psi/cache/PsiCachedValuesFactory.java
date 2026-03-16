// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.language.psi.cache;

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.util.CachedValuesFactory;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.language.psi.PsiManager;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.function.Supplier;

/**
 * @author Dmitry Avdeev
 */
@Singleton
@ServiceImpl
public final class PsiCachedValuesFactory implements CachedValuesFactory {
    private final PsiManager myManager;

    @Inject
    public PsiCachedValuesFactory(PsiManager psiManager) {
        myManager = psiManager;
    }

    
    @Override
    public <T> CachedValue<T> createCachedValue(CachedValueProvider<T> provider, boolean trackValue) {
        return new PsiCachedValueImpl<>(myManager, provider, trackValue, this);
    }

    
    @Override
    public <T, P> ParameterizedCachedValue<T, P> createParameterizedCachedValue(
        ParameterizedCachedValueProvider<T, P> provider,
        boolean trackValue
    ) {
        return new PsiParameterizedCachedValue<>(myManager, provider, trackValue, this);
    }

    @Override
    public void checkProviderForMemoryLeak(
        CachedValueProvider<?> provider,
        Key<?> key,
        UserDataHolder userDataHolder
    ) {
        CachedValueLeakChecker.checkProvider(provider, key, userDataHolder);
    }

    @Override
    public boolean areRandomChecksEnabled() {
        return IdempotenceChecker.areRandomChecksEnabled();
    }

    @Override
    public <T> void applyForRandomCheck(T data, Object provider, Supplier<? extends T> recomputeValue) {
        IdempotenceChecker.applyForRandomCheck(data, provider, recomputeValue);
    }

    @Override
    public <T> void checkEquivalence(
        @Nullable T existing,
        @Nullable T fresh,
        Class<?> providerClass,
        @Nullable Supplier<? extends T> recomputeValue
    ) {
        IdempotenceChecker.checkEquivalence(existing, fresh, providerClass, recomputeValue);
    }
}
