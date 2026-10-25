// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.application.progress.ProgressIndicator;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.combined.CombinedPathBlockId;
import consulo.diff.request.DiffRequest;
import consulo.review.util.ComputedResult;
import consulo.util.dataholder.UserDataHolder;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.PresentableChange;
import consulo.versionControlSystem.review.diff.model.AsyncDiffViewModel;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

final class AsyncDiffViewModelRequestProducer implements DiffRequestProducer, PresentableChange {
    private static final long WAIT_STEP_MS = 50;

    private final AsyncDiffViewModel myModel;
    private final CombinedPathBlockId myId;

    AsyncDiffViewModelRequestProducer(AsyncDiffViewModel model, CombinedPathBlockId id) {
        myModel = model;
        myId = id;
    }

    @Override
    public FilePath getFilePath() {
        return (FilePath) myId.getPath();
    }

    @Override
    public FileStatus getFileStatus() {
        FileStatus fileStatus = myId.getFileStatus();
        return fileStatus != null ? fileStatus : FileStatus.UNKNOWN;
    }

    @Override
    public String getName() {
        return myId.getPath().getPath();
    }

    @Override
    public DiffRequest process(UserDataHolder context, ProgressIndicator indicator) throws DiffRequestProducerException {
        CompletableFuture<DiffRequest> future = new CompletableFuture<>();
        Consumer<@Nullable ComputedResult<DiffRequest>> check = result -> {
            if (result == null || result.isInProgress()) {
                return;
            }
            Throwable error = result.exceptionOrNull();
            DiffRequest request = result.getOrNull();
            if (error != null || request == null) {
                future.completeExceptionally(error != null ? error : new IllegalStateException("No diff request"));
            }
            else {
                future.complete(request);
            }
        };

        Runnable unsubscribe = myModel.getRequest().addListener(check);
        try {
            check.accept(myModel.getRequest().get());
            while (true) {
                indicator.checkCanceled();
                try {
                    return future.get(WAIT_STEP_MS, TimeUnit.MILLISECONDS);
                }
                catch (TimeoutException ignored) {
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DiffRequestProducerException(e);
                }
                catch (ExecutionException e) {
                    throw new DiffRequestProducerException(e.getCause() != null ? e.getCause() : e);
                }
            }
        }
        finally {
            unsubscribe.run();
        }
    }
}
