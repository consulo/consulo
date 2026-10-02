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
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.sem.*;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.collection.SmartList;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public class SemServiceImpl extends SemService {
    private static final Key<SemData> SEM_CACHE_KEY = Key.create("SEM");

    private volatile @Nullable MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> myProducers;
    private final Project myProject;
    private final PsiModificationTracker myPsiModificationTracker;

    private final AtomicLong myClearCount = new AtomicLong();
    private volatile boolean myBulkChange = false;
    private volatile long myFrozenModCount;
    private final AtomicInteger myCreatingSem = new AtomicInteger(0);

    @Inject
    public SemServiceImpl(Project project) {
        myProject = project;
        myPsiModificationTracker = PsiModificationTracker.getInstance(project);

        LowMemoryWatcher.register(() -> {
            if (myCreatingSem.get() == 0) {
                clearCache();
            }
        }, project);
    }

    private MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> collectProducers() {
        MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> map = MultiMap.createSmart();

        SemRegistrar registrar = new SemRegistrar() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends SemElement, V extends PsiElement> void registerSemElementProvider(
                SemKey<T> key,
                ElementPattern<? extends V> place,
                Function<V, T> provider
            ) {
                map.putValue(key, element -> {
                    if (place.accepts(element)) {
                        return provider.apply((V) element);
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
        myClearCount.incrementAndGet();
    }

    @Override
    public void performAtomicChange(Runnable change) {
        ApplicationManager.getApplication().assertWriteAccessAllowed();

        boolean oldValue = myBulkChange;
        if (!oldValue) {
            myFrozenModCount = computeModCount();
        }
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
    @SuppressWarnings("unchecked")
    public <T extends SemElement> List<T> getSemElements(SemKey<T> key, PsiElement psi) {
        MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> producers = ensureInitialized();

        ConcurrentIntObjectMap<List<SemElement>> chunk = getUpToDate(getCacheHolder(psi));
        List<T> cached = findCached(key, chunk, true);
        if (cached != null) {
            return cached;
        }

        RecursionGuard.StackStamp stamp = RecursionManager.createGuard("semService").markStack();

        Set<T> result = new LinkedHashSet<>();
        Map<SemKey, List<SemElement>> map = new HashMap<>();
        for (SemKey each : key.getInheritors()) {
            List<SemElement> list = createSemElements(producers, each, psi);
            map.put(each, list);
            result.addAll((List<T>) list);
        }

        if (stamp.mayCacheNow()) {
            for (Map.Entry<SemKey, List<SemElement>> entry : map.entrySet()) {
                chunk.put(entry.getKey().getUniqueId(), entry.getValue());
            }
        }

        return new ArrayList<>(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SemElement> List<T> getSemElementsNoCache(SemKey<T> key, PsiElement psi) {
        MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> producers = ensureInitialized();

        SemData holder = psi.getUserData(SEM_CACHE_KEY);
        if (holder != null) {
            List<T> cached = findCached(key, getUpToDate(holder), true);
            if (cached != null) {
                return cached;
            }
        }

        Set<T> result = new LinkedHashSet<>();
        for (SemKey each : key.getInheritors()) {
            result.addAll((List<T>) createSemElements(producers, each, psi));
        }
        return new ArrayList<>(result);
    }

    private MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> ensureInitialized() {
        MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> producers = myProducers;
        if (producers == null) {
            producers = collectProducers();
            myProducers = producers;
        }
        return producers;
    }

    private List<SemElement> createSemElements(
        MultiMap<SemKey, Function<PsiElement, ? extends SemElement>> producers,
        SemKey key,
        PsiElement psi
    ) {
        List<SemElement> result = null;
        Collection<Function<PsiElement, ? extends SemElement>> functions = producers.get(key);
        if (!functions.isEmpty()) {
            for (Function<PsiElement, ? extends SemElement> producer : functions) {
                myCreatingSem.incrementAndGet();
                try {
                    SemElement element = producer.apply(psi);
                    if (element != null) {
                        if (result == null) {
                            result = new SmartList<>();
                        }
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
    public @Nullable <T extends SemElement> List<T> getCachedSemElements(SemKey<T> key, PsiElement psi) {
        SemData holder = psi.getUserData(SEM_CACHE_KEY);
        if (holder == null) {
            return null;
        }
        return findCached(key, getUpToDate(holder), false);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable <T extends SemElement> List<T> findCached(
        SemKey<T> key,
        ConcurrentIntObjectMap<List<SemElement>> chunk,
        boolean paranoid
    ) {
        List<T> singleList = null;
        Set<T> result = null;
        for (SemKey inheritor : key.getInheritors()) {
            List<T> cached = (List<T>) chunk.get(inheritor.getUniqueId());

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

    @Override
    public <T extends SemElement> void setCachedSemElement(SemKey<T> key, PsiElement psi, @Nullable T semElement) {
        getUpToDate(getCacheHolder(psi)).put(key.getUniqueId(), ContainerUtil.createMaybeSingletonList(semElement));
    }

    @Override
    public void clearCachedSemElements(PsiElement psi) {
        psi.putUserData(SEM_CACHE_KEY, null);
    }

    private SemData getCacheHolder(PsiElement psi) {
        return UserDataHolderUtil.computeIfAbsent(psi, SEM_CACHE_KEY, () -> new SemData(getModCount()));
    }

    private long getModCount() {
        return myBulkChange ? myFrozenModCount : computeModCount();
    }

    private long computeModCount() {
        return myPsiModificationTracker.getModificationCount() + myClearCount.get();
    }

    private ConcurrentIntObjectMap<List<SemElement>> getUpToDate(SemData holder) {
        long currentModCount = getModCount();
        long cachedModCount = holder.myModificationCount;
        if (currentModCount == cachedModCount) {
            return holder.myData;
        }
        return holder.refresh(cachedModCount, currentModCount);
    }

    private static final class SemData {
        private volatile long myModificationCount;
        private final ConcurrentIntObjectMap<List<SemElement>> myData = IntMaps.newConcurrentIntObjectHashMap(4, 0.75f, 2);

        private SemData(long count) {
            myModificationCount = count;
        }

        private synchronized ConcurrentIntObjectMap<List<SemElement>> refresh(long expectedModCount, long currentModCount) {
            if (expectedModCount == myModificationCount) {
                myData.clear();
                myModificationCount = currentModCount;
            }
            return myData;
        }

        @Override
        public String toString() {
            return "SemData{count=" + myData.size() + '}';
        }
    }
}
