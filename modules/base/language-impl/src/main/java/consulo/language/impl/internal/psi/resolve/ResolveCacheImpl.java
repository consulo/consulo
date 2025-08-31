/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.impl.internal.psi.resolve;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.RecursionGuard;
import consulo.application.util.RecursionManager;
import consulo.language.psi.*;
import consulo.language.psi.resolve.ResolveCache;
import consulo.project.Project;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.impl.map.ConcurrentWeakKeySoftValueHashMap;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.Trinity;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

@Singleton
@ServiceImpl
public class ResolveCacheImpl implements ResolveCache {
  private final AtomicReferenceArray<Map> myPhysicalMaps = new AtomicReferenceArray<>(4); //boolean incompleteCode, boolean isPoly
  private final AtomicReferenceArray<Map> myNonPhysicalMaps = new AtomicReferenceArray<>(4); //boolean incompleteCode, boolean isPoly

  @Inject
  public ResolveCacheImpl(@Nonnull Project project) {
    project.getMessageBus().connect().subscribe(AnyPsiChangeListener.class, new AnyPsiChangeListener() {
      @Override
      public void beforePsiChanged(boolean isPhysical) {
        clearCache(isPhysical);
      }
    });
  }

  @Nonnull
  private static <K, V> Map<K, V> createWeakMap() {
    return new ConcurrentWeakKeySoftValueHashMap<K, V>(100, 0.75f, Runtime.getRuntime().availableProcessors(), HashingStrategy.canonical()) {
      @Nonnull
      @Override
      protected ValueReference<K, V> createValueReference(@Nonnull V value, @Nonnull ReferenceQueue<? super V> queue) {
        ValueReference<K, V> result;
        if (value == NULL_RESULT || value instanceof Object[] && ((Object[])value).length == 0) {
          // no use in creating SoftReference to null
          result = createStrongReference(value);
        }
        else {
          result = super.createValueReference(value, queue);
        }
        return result;
      }

      @Override
      public V get(@Nonnull Object key) {
        V v = super.get(key);
        return v == NULL_RESULT ? null : v;
      }
    };
  }

  public void clearCache(boolean isPhysical) {
    if (isPhysical) {
      clearArray(myPhysicalMaps);
    }
    clearArray(myNonPhysicalMaps);
  }

  private static void clearArray(AtomicReferenceArray<?> array) {
    for (int i = 0; i < array.length(); i++) {
      array.set(i, null);
    }
  }

  @Nullable
  private <TRef extends PsiReference, TResult> TResult resolve(@Nonnull TRef ref,
                                                               @Nonnull AbstractResolver<? super TRef, TResult> resolver,
                                                               boolean needToPreventRecursion,
                                                               boolean incompleteCode,
                                                               boolean isPoly,
                                                               boolean isPhysical) {
    ProgressIndicatorProvider.checkCanceled();
    if (isPhysical) {
      ApplicationManager.getApplication().assertReadAccessAllowed();
    }
    int index = getIndex(incompleteCode, isPoly);
    Map<TRef, TResult> map = getMap(isPhysical, index);
    TResult result = map.get(ref);
    if (result != null) {
      return result;
    }

    RecursionGuard.StackStamp stamp = RecursionManager.markStack();
    result = needToPreventRecursion
             ? RecursionManager.doPreventingRecursion(Trinity.create(ref, incompleteCode, isPoly), true, () -> resolver.resolve(ref, incompleteCode))
             : resolver.resolve(ref, incompleteCode);
    if (result instanceof ResolveResult) {
      ensureValidPsi((ResolveResult)result);
    }
    else if (result instanceof ResolveResult[]) {
      ensureValidResults((ResolveResult[])result);
    }
    else if (result instanceof PsiElement) {
      PsiUtilCore.ensureValid((PsiElement)result);
    }

    if (stamp.mayCacheNow()) {
      cache(ref, map, result);
    }
    return result;
  }

  @Override
  @Nonnull
  public <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(@Nonnull T ref, @Nonnull PolyVariantResolver<T> resolver, boolean needToPreventRecursion, boolean incompleteCode) {
    return resolveWithCaching(ref, resolver, needToPreventRecursion, incompleteCode, ref.getElement().getContainingFile());
  }

  @Override
  @Nonnull
  public <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(@Nonnull T ref,
                                                                                @Nonnull PolyVariantResolver<T> resolver,
                                                                                boolean needToPreventRecursion,
                                                                                boolean incompleteCode,
                                                                                @Nonnull PsiFile containingFile) {
    ResolveResult[] result = resolve(ref, resolver, needToPreventRecursion, incompleteCode, true, containingFile.isPhysical());
    return result == null ? ResolveResult.EMPTY_ARRAY : result;
  }

  @Override
  @Nonnull
  public <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(@Nonnull T ref,
                                                                                @Nonnull PolyVariantContextResolver<T> resolver,
                                                                                boolean needToPreventRecursion,
                                                                                boolean incompleteCode,
                                                                                @Nonnull PsiFile containingFile) {
    ProgressIndicatorProvider.checkCanceled();
    ApplicationManager.getApplication().assertReadAccessAllowed();

    boolean physical = containingFile.isPhysical();
    int index = getIndex(incompleteCode, true);
    Map<T, ResolveResult[]> map = getMap(physical, index);
    ResolveResult[] result = map.get(ref);
    if (result != null) {
      return result;
    }

    RecursionGuard.StackStamp stamp = RecursionManager.markStack();
    result = needToPreventRecursion
             ? RecursionManager.doPreventingRecursion(Pair.create(ref, incompleteCode), true, () -> resolver.resolve(ref, containingFile, incompleteCode))
             : resolver.resolve(ref, containingFile, incompleteCode);
    if (result != null) {
      ensureValidResults(result);
    }

    if (stamp.mayCacheNow()) {
      cache(ref, map, result);
    }
    return result == null ? ResolveResult.EMPTY_ARRAY : result;
  }

  private static void ensureValidResults(ResolveResult[] result) {
    for (ResolveResult resolveResult : result) {
      ensureValidPsi(resolveResult);
    }
  }

  private static void ensureValidPsi(ResolveResult resolveResult) {
    PsiElement element = resolveResult.getElement();
    if (element != null) {
      PsiUtilCore.ensureValid(element);
    }
  }

  @Override
  @Nullable // null means not cached
  public <T extends PsiPolyVariantReference> ResolveResult[] getCachedResults(@Nonnull T ref, boolean physical, boolean incompleteCode, boolean isPoly) {
    Map<T, ResolveResult[]> map = getMap(physical, getIndex(incompleteCode, isPoly));
    return map.get(ref);
  }

  @Override
  @Nullable
  public <TRef extends PsiReference, TResult> TResult resolveWithCaching(@Nonnull TRef ref, @Nonnull AbstractResolver<TRef, TResult> resolver, boolean needToPreventRecursion, boolean incompleteCode) {
    return resolve(ref, resolver, needToPreventRecursion, incompleteCode, false, ref.getElement().isPhysical());
  }

  @Nonnull
  private <TRef extends PsiReference, TResult> Map<TRef, TResult> getMap(boolean physical, int index) {
    AtomicReferenceArray<Map> array = physical ? myPhysicalMaps : myNonPhysicalMaps;
    Map map = array.get(index);
    while (map == null) {
      Map newMap = createWeakMap();
      map = array.compareAndSet(index, null, newMap) ? newMap : array.get(index);
    }
    //noinspection unchecked
    return map;
  }

  private static int getIndex(boolean incompleteCode, boolean isPoly) {
    return (incompleteCode ? 0 : 1) * 2 + (isPoly ? 0 : 1);
  }

  private static final Object NULL_RESULT = ObjectUtil.sentinel("ResolveCache.NULL_RESULT");

  private static <TRef extends PsiReference, TResult> void cache(@Nonnull TRef ref, @Nonnull Map<? super TRef, TResult> map, TResult result) {
    // optimization: less contention
    TResult cached = map.get(ref);
    if (cached != null && cached == result) {
      return;
    }
    if (result == null) {
      // no use in creating SoftReference to null
      cached = (TResult)NULL_RESULT;
    }
    else {
      cached = result;
    }
    map.put(ref, cached);
  }

  @Nonnull
  private static <K, V> StrongValueReference<K, V> createStrongReference(@Nonnull V value) {
    return value == NULL_RESULT ? NULL_VALUE_REFERENCE : value == ResolveResult.EMPTY_ARRAY ? EMPTY_RESOLVE_RESULT : new StrongValueReference<>(value);
  }

  private static final StrongValueReference NULL_VALUE_REFERENCE = new StrongValueReference<>(NULL_RESULT);
  private static final StrongValueReference EMPTY_RESOLVE_RESULT = new StrongValueReference<>(ResolveResult.EMPTY_ARRAY);

  private static class StrongValueReference<K, V> implements consulo.util.collection.impl.map.ConcurrentWeakKeySoftValueHashMap.ValueReference<K, V> {
    private final V myValue;

    StrongValueReference(@Nonnull V value) {
      myValue = value;
    }

    @Nonnull
    @Override
    public consulo.util.collection.impl.map.ConcurrentWeakKeySoftValueHashMap.KeyReference<K, V> getKeyReference() {
      throw new UnsupportedOperationException(); // will never GC so this method will never be called so no implementation is necessary
    }

    @Override
    public V get() {
      return myValue;
    }
  }
}
