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

package com.intellij.psi.impl.source.resolve;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.AnyPsiChangeListener;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.util.containers.ConcurrentWeakKeySoftValueHashMap;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotations.RequiredReadAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class ResolveCache {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.resolve.ResolveCache");
  @SuppressWarnings("unchecked")
  private final ConcurrentMap[] myMaps = new ConcurrentWeakKeySoftValueHashMap[2*2*2]; //boolean physical, boolean incompleteCode, boolean isPoly
  private final RecursionGuard myGuard = RecursionManager.createGuard("resolveCache");

  public static ResolveCache getInstance(Project project) {
    ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly
    return ServiceManager.getService(project, ResolveCache.class);
  }

  public interface AbstractResolver<TRef extends PsiReference, TResult> {
    TResult resolve(@Nonnull TRef ref, boolean incompleteCode);
  }

  /**
   * Resolver which returns array of possible resolved variants instead of just one
   */
  public interface PolyVariantResolver<T extends PsiPolyVariantReference> extends AbstractResolver<T,ResolveResult[]> {
    @Override
    @Nonnull
    ResolveResult[] resolve(@Nonnull T t, boolean incompleteCode);
  }

  /**
   * Poly variant resolver with additional containingFile parameter, which helps to avoid costly tree up traversal
   */
  public interface PolyVariantContextResolver<T extends PsiPolyVariantReference> {
    @Nonnull
    ResolveResult[] resolve(@Nonnull T ref, @Nonnull PsiFile containingFile, boolean incompleteCode);
  }

  /**
   * Resolver specialized to resolve PsiReference to PsiElement
   */
  public interface Resolver extends AbstractResolver<PsiReference, PsiElement> {
  }

  @Inject
  public ResolveCache(@Nonnull Project project) {
    for (int i = 0; i < myMaps.length; i++) {
      myMaps[i] = createWeakMap();
    }
    project.getMessageBus().connect().subscribe(PsiManagerImpl.ANY_PSI_CHANGE_TOPIC, new AnyPsiChangeListener.Adapter() {
      @Override
      public void beforePsiChanged(boolean isPhysical) {
        clearCache(isPhysical);
      }
    });
  }

  @Nonnull
  private static <K,V> ConcurrentMap<K, V> createWeakMap() {
    return new ConcurrentWeakKeySoftValueHashMap<K, V>(100, 0.75f, Runtime.getRuntime().availableProcessors(), ContainerUtil.<K>canonicalStrategy()){
      @Nonnull
      @Override
      protected ValueReference<K, V> createValueReference(@Nonnull final V value,
                                                          @Nonnull ReferenceQueue<V> queue) {
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
    int startIndex = isPhysical ? 0 : 1;
    for (int i=startIndex;i<2;i++)for (int j=0;j<2;j++)for (int k=0;k<2;k++) myMaps[i*4+j*2+k].clear();
  }

  @Nullable
  private <TRef extends PsiReference, TResult> TResult resolve(@Nonnull final TRef ref,
                                                               @Nonnull final AbstractResolver<TRef, TResult> resolver,
                                                               boolean needToPreventRecursion,
                                                               final boolean incompleteCode,
                                                               boolean isPoly,
                                                               boolean isPhysical) {
    ProgressIndicatorProvider.checkCanceled();
    if (isPhysical) {
      ApplicationManager.getApplication().assertReadAccessAllowed();
    }

    int index = getIndex(isPhysical, incompleteCode, isPoly);
    ConcurrentMap<TRef, TResult> map = getMap(index);
    TResult result = map.get(ref);
    if (result != null) {
      return result;
    }

    RecursionGuard.StackStamp stamp = myGuard.markStack();
    result = needToPreventRecursion ? myGuard.doPreventingRecursion(Trinity.create(ref, incompleteCode, isPoly), true, new Computable<TResult>() {
      @Override
      public TResult compute() {
        return resolver.resolve(ref, incompleteCode);
      }
    }) : resolver.resolve(ref, incompleteCode);
    PsiElement element = result instanceof ResolveResult ? ((ResolveResult)result).getElement() : null;
    LOG.assertTrue(element == null || element.isValid(), result);

    if (stamp.mayCacheNow()) {
      cache(ref, map, result);
    }
    return result;
  }

  @Nonnull
  public <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(@Nonnull T ref,
                                                                                @Nonnull PolyVariantResolver<T> resolver,
                                                                                boolean needToPreventRecursion,
                                                                                boolean incompleteCode) {
    return resolveWithCaching(ref, resolver, needToPreventRecursion, incompleteCode, ref.getElement().getContainingFile());
  }
  @Nonnull
  public <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(@Nonnull T ref,
                                                                                @Nonnull PolyVariantResolver<T> resolver,
                                                                                boolean needToPreventRecursion,
                                                                                boolean incompleteCode,
                                                                                @Nonnull PsiFile containingFile) {
    ResolveResult[] result = resolve(ref, resolver, needToPreventRecursion, incompleteCode, true, containingFile.isPhysical());
    return result == null ? ResolveResult.EMPTY_ARRAY : result;
  }

  @Nonnull
  @RequiredReadAction
  public <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(@Nonnull final T ref,
                                                                                @Nonnull final PolyVariantContextResolver<T> resolver,
                                                                                boolean needToPreventRecursion,
                                                                                final boolean incompleteCode,
                                                                                @Nonnull final PsiFile containingFile) {
    ProgressIndicatorProvider.checkCanceled();
    ApplicationManager.getApplication().assertReadAccessAllowed();

    int index = getIndex(containingFile.isPhysical(), incompleteCode, true);
    ConcurrentMap<T, ResolveResult[]> map = getMap(index);
    ResolveResult[] result = map.get(ref);
    if (result != null) {
      return result;
    }

    RecursionGuard.StackStamp stamp = myGuard.markStack();
    result = needToPreventRecursion ? myGuard.doPreventingRecursion(Pair.create(ref, incompleteCode), true, new Computable<ResolveResult[]>() {
      @Override
      public ResolveResult[] compute() {
        return resolver.resolve(ref, containingFile, incompleteCode);
      }
    }) : resolver.resolve(ref, containingFile, incompleteCode);

    if (stamp.mayCacheNow()) {
      cache(ref, map, result);
    }
    return result == null ? ResolveResult.EMPTY_ARRAY : result;
  }

  @Nullable
  public <T extends PsiPolyVariantReference> ResolveResult[] getCachedResults(@Nonnull T ref, boolean physical, boolean incompleteCode, boolean isPoly) {
    Map<T, ResolveResult[]> map = getMap(getIndex(physical, incompleteCode, isPoly));
    return map.get(ref);
  }

  @Nullable
  public <TRef extends PsiReference, TResult>
  TResult resolveWithCaching(@Nonnull TRef ref,
                             @Nonnull AbstractResolver<TRef, TResult> resolver,
                             boolean needToPreventRecursion,
                             boolean incompleteCode) {
    return resolve(ref, resolver, needToPreventRecursion, incompleteCode, false, ref.getElement().isPhysical());
  }

  @Nonnull
  private <TRef extends PsiReference,TResult> ConcurrentMap<TRef, TResult> getMap(int index) {
    //noinspection unchecked
    return myMaps[index];
  }

  private static int getIndex(boolean physical, boolean incompleteCode, boolean isPoly) {
    return (physical ? 0 : 1)*4 + (incompleteCode ? 0 : 1)*2 + (isPoly ? 0 : 1);
  }

  private static final Object NULL_RESULT = new Object();
  private <TRef extends PsiReference, TResult> void cache(@Nonnull TRef ref,
                                                          @Nonnull ConcurrentMap<TRef, TResult> map,
                                                          TResult result) {
    // optimization: less contention
    TResult cached = map.get(ref);
    if (cached != null && cached == result) {
      return;
    }
    if (result == null) {
      // no use in creating SoftReference to null
      //noinspection unchecked
      cached = (TResult)NULL_RESULT;
    }
    else {
      //noinspection unchecked
      cached = result;
    }
    map.put(ref, cached);
  }

  @Nonnull
  private static <K, V> StrongValueReference<K, V> createStrongReference(@Nonnull V value) {
    return value == NULL_RESULT ? NULL_VALUE_REFERENCE : value == ResolveResult.EMPTY_ARRAY ? EMPTY_RESOLVE_RESULT : new StrongValueReference<K, V>(value);
  }

  private static final StrongValueReference NULL_VALUE_REFERENCE = new StrongValueReference(NULL_RESULT);
  private static final StrongValueReference EMPTY_RESOLVE_RESULT = new StrongValueReference(ResolveResult.EMPTY_ARRAY);
  private static class StrongValueReference<K, V> implements ConcurrentWeakKeySoftValueHashMap.ValueReference<K, V> {
    private final V myValue;

    public StrongValueReference(@Nonnull V value) {
      myValue = value;
    }

    @Nonnull
    @Override
    public ConcurrentWeakKeySoftValueHashMap.KeyReference<K, V> getKeyReference() {
      throw new UnsupportedOperationException(); // will never GC so this method will never be called so no implementation is necessary
    }

    @Override
    public V get() {
      return myValue;
    }
  }
}
