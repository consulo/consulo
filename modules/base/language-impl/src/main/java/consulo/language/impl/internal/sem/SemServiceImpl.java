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
package consulo.language.impl.internal.sem;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.LowMemoryWatcher;
import consulo.application.util.RecursionGuard;
import consulo.application.util.RecursionManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTrackerListener;
import consulo.language.sem.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.collection.MultiMap;
import consulo.util.collection.SmartList;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public class SemServiceImpl extends SemService {
  private static final Logger LOG = Logger.getInstance(SemServiceImpl.class);

  private final ConcurrentMap<PsiElement, SemCacheChunk> myCache = Maps.newConcurrentWeakKeySoftValueHashMap();
  private volatile MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> myProducers;
  private final Project myProject;

  private boolean myBulkChange = false;
  private final AtomicInteger myCreatingSem = new AtomicInteger(0);

  @Inject
  public SemServiceImpl(Project project, PsiManager psiManager) {
    myProject = project;
    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(PsiModificationTrackerListener.class, new PsiModificationTrackerListener() {
      @Override
      public void modificationCountChanged() {
        if (!isInsideAtomicChange()) {
          clearCache();
        }
      }
    });

    ((PsiManagerEx)psiManager).registerRunnableToRunOnChange(() -> {
      if (!isInsideAtomicChange()) {
        clearCache();
      }
    });


    LowMemoryWatcher.register(() -> {
      if (myCreatingSem.get() == 0) {
        clearCache();
      }
      //System.out.println("SemService cache flushed");
    }, project);
  }

  private MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> collectProducers() {
    final MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> map = MultiMap.createSmart();

    SemRegistrar registrar = new SemRegistrar() {
      @Override
      public <T extends SemElement, V extends PsiElement> void registerSemElementProvider(SemKey<T> key, ElementPattern<? extends V> place, Function<V, T> provider) {
        map.putValue(key, element -> {
          if (place.accepts(element)) {
            return provider.apply((V)element);
          }
          return null;
        });
      }
    };

    for (SemContributor contributor : myProject.getExtensionList(SemContributor.class)) {
      contributor.registerSemProviders(registrar);
    }

    return map;
  }

  @Override
  public void clearCache() {
    myCache.clear();
  }

  @Override
  public void performAtomicChange(@Nonnull Runnable change) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    boolean oldValue = myBulkChange;
    myBulkChange = true;
    try {
      change.run();
    }
    finally {
      myBulkChange = oldValue;
      if (!oldValue) {
        clearCache();
      }
    }
  }

  @Override
  public boolean isInsideAtomicChange() {
    return myBulkChange;
  }

  @Override
  @Nullable
  public <T extends SemElement> List<T> getSemElements(SemKey<T> key, @Nonnull PsiElement psi) {
    List<T> cached = _getCachedSemElements(key, true, psi);
    if (cached != null) {
      return cached;
    }

    ensureInitialized();

    RecursionGuard.StackStamp stamp = RecursionManager.createGuard("semService").markStack();

    LinkedHashSet<T> result = new LinkedHashSet<>();
    Map<SemKey, List<SemElement>> map = new HashMap<>();
    for (SemKey each : key.getInheritors()) {
      List<SemElement> list = createSemElements(each, psi);
      map.put(each, list);
      result.addAll((List<T>)list);
    }

    if (stamp.mayCacheNow()) {
      SemCacheChunk persistent = getOrCreateChunk(psi);
      for (SemKey semKey : map.keySet()) {
        persistent.putSemElements(semKey, map.get(semKey));
      }
    }

    return new ArrayList<>(result);
  }

  private void ensureInitialized() {
    if (myProducers == null) {
      myProducers = collectProducers();
    }
  }

  @Nonnull
  private List<SemElement> createSemElements(SemKey key, PsiElement psi) {
    List<SemElement> result = null;
    Collection<Function<PsiElement, ? extends SemElement>> producers = myProducers.get(key);
    if (!producers.isEmpty()) {
      for (Function<PsiElement, ? extends SemElement> producer : producers) {
        myCreatingSem.incrementAndGet();
        try {
          SemElement element = producer.apply(psi);
          if (element != null) {
            if (result == null) result = new SmartList<>();
            result.add(element);
          }
        }
        finally {
          myCreatingSem.decrementAndGet();
        }
      }
    }
    return result == null ? Collections.emptyList() : Collections.unmodifiableList(result);
  }

  @Override
  @Nullable
  public <T extends SemElement> List<T> getCachedSemElements(SemKey<T> key, @Nonnull PsiElement psi) {
    return _getCachedSemElements(key, false, psi);
  }

  @Nullable
  private <T extends SemElement> List<T> _getCachedSemElements(SemKey<T> key, boolean paranoid, PsiElement element) {
    SemCacheChunk chunk = obtainChunk(element);
    if (chunk == null) return null;

    List<T> singleList = null;
    LinkedHashSet<T> result = null;
    List<SemKey> inheritors = key.getInheritors();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < inheritors.size(); i++) {
      List<T> cached = (List<T>)chunk.getSemElements(inheritors.get(i));

      if (cached == null && paranoid) {
        return null;
      }

      if (cached != null && cached != Collections.<T>emptyList()) {
        if (singleList == null) {
          singleList = cached;
          continue;
        }

        if (result == null) {
          result = new LinkedHashSet<>(singleList);
        }
        result.addAll(cached);
      }
    }


    if (result == null) {
      if (singleList != null) {
        return singleList;
      }

      return List.of();
    }

    return new ArrayList<>(result);
  }

  @Nullable
  private SemCacheChunk obtainChunk(@Nullable PsiElement root) {
    return myCache.get(root);
  }

  @Override
  public <T extends SemElement> void setCachedSemElement(SemKey<T> key, @Nonnull PsiElement psi, @Nullable T semElement) {
    getOrCreateChunk(psi).putSemElements(key, ContainerUtil.createMaybeSingletonList(semElement));
  }

  @Override
  public void clearCachedSemElements(@Nonnull PsiElement psi) {
    myCache.remove(psi);
  }

  private SemCacheChunk getOrCreateChunk(PsiElement element) {
    SemCacheChunk chunk = obtainChunk(element);
    if (chunk == null) {
      chunk = Maps.cacheOrGet(myCache, element, new SemCacheChunk());
    }
    return chunk;
  }

  private static class SemCacheChunk {
    private final ConcurrentIntObjectMap<List<SemElement>> map = IntMaps.newConcurrentIntObjectHashMap();

    public List<SemElement> getSemElements(SemKey<?> key) {
      return map.get(key.getUniqueId());
    }

    public void putSemElements(SemKey<?> key, List<SemElement> elements) {
      map.put(key.getUniqueId(), elements);
    }

    @Override
    public int hashCode() {
      return 0; // ConcurrentWeakKeySoftValueHashMap.SoftValue requires hashCode, and this is faster than identityHashCode
    }
  }

}
