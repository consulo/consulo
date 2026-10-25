// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.diff.DiffContext;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.combined.CombinedBlockId;
import consulo.diff.combined.CombinedBlockProducer;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.ErrorDiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class CombinedDiffModel {
    private static final Logger LOG = Logger.getInstance(CombinedDiffModel.class);

    private final Project myProject;

    private final Disposable myDisposable = Disposable.newDisposable("CombinedDiffModel");
    private volatile boolean myDisposed;

    private final List<CombinedDiffModelListener> myModelListeners = new CopyOnWriteArrayList<>();

    private final Executor myContentLoadingQueue = AppExecutorUtil.createBoundedApplicationPoolExecutor("CombinedDiffModel", 1);
    private @Nullable LoadContentRequest myPendingRequest;

    private Map<CombinedBlockId, CombinedBlockProducer> myRequests = Map.of();

    private final Map<CombinedBlockId, DiffRequest> myLoadedRequests = new ConcurrentHashMap<>();

    private final DiffContext myContext;

    public CombinedDiffModel(Project project) {
        myProject = project;
        myContext = new CombinedDiffContext(project);
        Disposer.register(myDisposable, () -> myDisposed = true);
    }

    public Project getProject() {
        return myProject;
    }

    public Disposable getDisposable() {
        return myDisposable;
    }

    public DiffContext getContext() {
        return myContext;
    }

    public List<CombinedBlockProducer> getRequests() {
        return List.copyOf(myRequests.values());
    }

    public void cleanBlocks() {
        cleanLoadedRequests();
    }

    private void cleanLoadedRequests() {
        for (DiffRequest request : myLoadedRequests.values()) {
            request.onAssigned(false);
        }
        myLoadedRequests.clear();
    }

    @RequiredUIAccess
    public void reload() {
        Map<CombinedBlockId, DiffRequest> previouslyLoaded = new LinkedHashMap<>(myLoadedRequests);
        cleanLoadedRequests();
        if (!previouslyLoaded.isEmpty()) {
            loadRequestContents(previouslyLoaded.keySet());
        }
    }

    @RequiredUIAccess
    public void setBlocks(List<CombinedBlockProducer> requests) {
        cleanLoadedRequests();
        Map<CombinedBlockId, CombinedBlockProducer> map = new LinkedHashMap<>();
        for (CombinedBlockProducer request : requests) {
            map.put(request.getId(), request);
        }
        myRequests = map;
        for (CombinedDiffModelListener listener : myModelListeners) {
            listener.onModelReset();
        }
    }

    public @Nullable CombinedBlockProducer getBlock(CombinedBlockId blockId) {
        return myRequests.get(blockId);
    }

    public @Nullable DiffRequest getLoadedRequest(CombinedBlockId blockId) {
        return myLoadedRequests.get(blockId);
    }

    public List<DiffRequest> getLoadedRequests() {
        return List.copyOf(myLoadedRequests.values());
    }

    @RequiredUIAccess
    public void loadRequestContents(Collection<CombinedBlockId> blockIds) {
        List<CombinedBlockId> notLoadedBlockIds = new ArrayList<>();
        for (CombinedBlockId blockId : blockIds) {
            if (!myLoadedRequests.containsKey(blockId)) {
                notLoadedBlockIds.add(blockId);
            }
        }
        if (notLoadedBlockIds.isEmpty()) {
            return;
        }

        LoadContentRequest previous = myPendingRequest;
        if (previous != null) {
            previous.reject();
        }

        LoadContentRequest request = new LoadContentRequest(notLoadedBlockIds, UIAccess.current());
        myPendingRequest = request;
        myContentLoadingQueue.execute(request);
    }

    @RequiredUIAccess
    public void unloadRequestContents(Collection<CombinedBlockId> blockIds) {
        Map<CombinedBlockId, DiffRequest> unloadedRequests = new LinkedHashMap<>();
        int loadedRequestsLimit = CombinedDiffRegistry.getMaxBlockCountInMemory();

        for (CombinedBlockId blockId : blockIds) {
            if (loadedRequestsLimit < 0 || loadedRequestsLimit < myLoadedRequests.size()) {
                DiffRequest unloadedRequest = myLoadedRequests.remove(blockId);
                if (unloadedRequest == null) {
                    continue;
                }
                unloadedRequests.put(blockId, unloadedRequest);
            }
        }
        if (!unloadedRequests.isEmpty()) {
            for (CombinedDiffModelListener listener : myModelListeners) {
                listener.onRequestContentsUnloaded(unloadedRequests);
            }
        }
    }

    public void addListener(CombinedDiffModelListener listener, Disposable disposable) {
        myModelListeners.add(listener);
        Disposer.register(disposable, () -> myModelListeners.remove(listener));
    }

    private void loadRequests(ProgressIndicator indicator, Collection<CombinedBlockId> blockIds, UIAccess uiAccess) {
        for (CombinedBlockId blockId : blockIds) {
            indicator.checkCanceled();

            if (myLoadedRequests.containsKey(blockId)) {
                continue;
            }

            CombinedBlockProducer requestProducer = myRequests.get(blockId);
            if (requestProducer == null) {
                continue;
            }

            DiffRequest loadedRequest = loadRequest(indicator, blockId, requestProducer.getProducer());
            uiAccess.execute(() -> {
                if (myDisposed) {
                    return;
                }
                for (CombinedDiffModelListener listener : myModelListeners) {
                    listener.onRequestsLoaded(blockId, loadedRequest);
                }
            });
        }
    }

    private DiffRequest loadRequest(ProgressIndicator indicator, CombinedBlockId blockId, DiffRequestProducer producer) {
        DiffRequest request;
        try {
            request = producer.process(myContext, indicator);
        }
        catch (DiffRequestProducerException e) {
            request = new ErrorDiffRequest(producer, e);
        }
        request.putUserData(DiffUserDataKeysEx.EDITORS_HIDE_TITLE, true);
        myLoadedRequests.put(blockId, request);
        return request;
    }

    private final class LoadContentRequest implements Runnable {
        private final Collection<CombinedBlockId> myBlockIds;
        private final UIAccess myUIAccess;
        private final ProgressIndicator myIndicator = new EmptyProgressIndicator();
        private final AtomicBoolean myStarted = new AtomicBoolean();

        private LoadContentRequest(Collection<CombinedBlockId> blockIds, UIAccess uiAccess) {
            myBlockIds = blockIds;
            myUIAccess = uiAccess;
        }

        private void reject() {
            if (!myStarted.get()) {
                myIndicator.cancel();
            }
        }

        @Override
        public void run() {
            if (!myStarted.compareAndSet(false, true) || myIndicator.isCanceled() || myDisposed) {
                return;
            }
            try {
                ProgressManager.getInstance().runProcess(() -> loadRequests(myIndicator, myBlockIds, myUIAccess), myIndicator);
            }
            catch (ProcessCanceledException ignored) {
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
    }
}
