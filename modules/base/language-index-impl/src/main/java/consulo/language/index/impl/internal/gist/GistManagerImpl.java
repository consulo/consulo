// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.gist;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.application.util.MergingProcessingQueue;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.gist.GistManager;
import consulo.language.psi.stub.gist.PsiFileGist;
import consulo.language.psi.stub.gist.VirtualFileGist;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

@Singleton
@ServiceImpl
public final class GistManagerImpl extends GistManager {
    private static final Logger LOG = Logger.getInstance(GistManagerImpl.class);
    private static final Set<String> ourKnownIds = ConcurrentHashMap.newKeySet();
    private static final String ourPropertyName = "file.gist.reindex.count";
    private static final Object DROP_CACHES_KEY = new Object();
    private final AtomicInteger myReindexCount = new AtomicInteger(ApplicationPropertiesComponent.getInstance().getInt(ourPropertyName, 0));
    private final AtomicBoolean myDropCachesQueued = new AtomicBoolean();
    private final MergingProcessingQueue<Object> myDropCachesQueue;
    private final AtomicInteger myMergingDropCachesRequestors = new AtomicInteger();
    private final AtomicBoolean myMergedDropCachesRequested = new AtomicBoolean();
    private final CoroutineScope myCoroutineScope;

    @Inject
    public GistManagerImpl(Application application, ApplicationConcurrency applicationConcurrency) {
        myCoroutineScope = CoroutineScope.of(application.coroutineContext());
        myDropCachesQueue = new MergingProcessingQueue<>(applicationConcurrency, 500) {
            @Override
            protected void process(Object key) {
                myDropCachesQueued.set(false);
                dropCaches();
            }
        };
    }

    private void dropCaches() {
        WriteLock.apply((o, continuation) -> {
                for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
                    PsiManager.getInstance(openProject).dropPsiCaches();
                }
                return null;
            })
            .toCoroutine()
            .runAsync(myCoroutineScope, null);
    }

    @Override
    public <Data> VirtualFileGist<Data> newVirtualFileGist(String id, int version, DataExternalizer<Data> externalizer, BiFunction<Project, VirtualFile, Data> calcData) {
        if (!ourKnownIds.add(id)) {
            throw new IllegalArgumentException("Gist '" + id + "' is already registered");
        }

        return new VirtualFileGistImpl<>(id, version, externalizer, calcData);
    }

    @Override
    public <Data> PsiFileGist<Data> newPsiFileGist(String id, int version, DataExternalizer<Data> externalizer, Function<PsiFile, Data> calculator) {
        return new PsiFileGistImpl<>(id, version, externalizer, calculator);
    }

    int getReindexCount() {
        return myReindexCount.get();
    }

    @Override
    public void invalidateData() {
        invalidateGists();
        invalidateDependentCaches();
    }

    protected void invalidateGists() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Invalidating gists", new Throwable());
        }
        // Clear all cache at once to simplify and speedup this operation.
        // It can be made per-file if cache recalculation ever becomes an issue.
        ApplicationPropertiesComponent.getInstance().setValue(ourPropertyName, myReindexCount.incrementAndGet(), 0);
    }

    private void invalidateDependentCaches() {
        if (myMergingDropCachesRequestors.get() == 0) {
            if (myDropCachesQueued.compareAndSet(false, true)) {
                myDropCachesQueue.queueAdd(DROP_CACHES_KEY);
            }
        }
        else {
            myMergedDropCachesRequested.set(true);
        }
    }

    public AccessToken mergeDependentCacheInvalidations() {
        myMergingDropCachesRequestors.incrementAndGet();
        return new AccessToken() {
            private final AtomicBoolean alreadyFinished = new AtomicBoolean(false);

            @Override
            public void finish() {
                if (alreadyFinished.compareAndSet(false, true)) {
                    if (myMergingDropCachesRequestors.decrementAndGet() == 0 && myMergedDropCachesRequested.compareAndSet(true, false)) {
                        dropCaches();
                    }
                }
            }
        };
    }

    public void runWithMergingDependentCacheInvalidations(Runnable runnable) {
        try (AccessToken ignored = mergeDependentCacheInvalidations()) {
            runnable.run();
        }
    }

    @TestOnly
    public void resetReindexCount() {
        myReindexCount.set(0);
        ApplicationPropertiesComponent.getInstance().unsetValue(ourPropertyName);
    }
}
