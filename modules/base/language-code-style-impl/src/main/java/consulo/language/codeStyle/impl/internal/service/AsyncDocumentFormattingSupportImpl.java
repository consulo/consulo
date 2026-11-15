// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.codeStyle.impl.internal.service;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.platform.PlatformTargetManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.internal.AsyncDocumentFormattingSupport;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.service.AsyncDocumentFormattingService;
import consulo.language.codeStyle.service.AsyncDocumentFormattingService.FormattingTask;
import consulo.language.codeStyle.service.AsyncFormattingRequest;
import consulo.language.codeStyle.service.DocumentMerger;
import consulo.language.codeStyle.service.FormattingNotificationService;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.FORMAT_DOCUMENT_SYNCHRONOUSLY;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.createFormattingTask;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.getName;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.getNotificationGroup;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.getTimeout;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.getTimeoutActions;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.getTimeoutNotificationDisplayId;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.needToUpdate;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.prepareForFormatting;
import static consulo.language.codeStyle.service.AsyncDocumentFormattingService.supportsSynchronousFormatting;

public class AsyncDocumentFormattingSupportImpl implements AsyncDocumentFormattingSupport {
    private static final Logger LOG = Logger.getInstance(AsyncDocumentFormattingSupportImpl.class);

    private static final String TEMP_FILE_PREFIX = "consulo-format-temp";

    private final AsyncDocumentFormattingService myService;
    private final ConcurrentHashMap<Document, FormattingRequestImpl> myPendingRequests = new ConcurrentHashMap<>();

    public AsyncDocumentFormattingSupportImpl(AsyncDocumentFormattingService service) {
        myService = service;
    }

    @Override
    public synchronized void formatDocument(
        Document document,
        List<TextRange> formattingRanges,
        FormattingContext formattingContext,
        boolean canChangeWhiteSpaceOnly,
        boolean quickFormat
    ) {
        FormattingRequestImpl currRequest = myPendingRequests.get(document);
        boolean isSync = isSyncFormat(document);
        if (currRequest != null) {
            if (!currRequest.cancel()) {
                LOG.warn("Pending request can't be cancelled");
                return;
            }
        }
        if (!Application.get().getInstance(PlatformTargetManager.class).isReachable(formattingContext.getProject().getPlatform())) {
            // the machine of the project is not connected: formatting there is not possible, and it must not fall back to the local one
            return;
        }
        prepareForFormatting(myService, document, formattingContext);
        FormattingRequestImpl formattingRequest =
            new FormattingRequestImpl(formattingContext, document, formattingRanges, canChangeWhiteSpaceOnly, quickFormat, isSync);
        FormattingTask formattingTask = createFormattingTask(myService, formattingRequest);
        if (formattingTask != null) {
            formattingRequest.myTask = formattingTask;
            myPendingRequests.put(document, formattingRequest);
            if (isSync) {
                runAsyncFormatBlocking(formattingRequest);
            }
            else {
                launchAsyncFormat(formattingRequest);
            }
        }
    }

    private void launchAsyncFormat(FormattingRequestImpl formattingRequest) {
        formattingRequest.myModalityState = Application.get().getDefaultModalityState();
        AppExecutorUtil.getAppExecutorService().execute(() -> runAsyncFormat(formattingRequest));
    }

    private boolean isSyncFormat(Document document) {
        if (!supportsSynchronousFormatting(myService)) {
            return false;
        }
        boolean forceSync = isForceSyncFormat(document);
        boolean isHeadless = Application.get().isHeadlessEnvironment();
        boolean isIgnoreHeadless = Boolean.getBoolean("consulo.async.formatting.ignoreHeadless");
        return forceSync || (isHeadless && !isIgnoreHeadless);
    }

    private static boolean isForceSyncFormat(Document document) {
        return document.getUserData(FORMAT_DOCUMENT_SYNCHRONOUSLY) == Boolean.TRUE;
    }

    private void runAsyncFormatBlocking(FormattingRequestImpl request) {
        runAsyncFormat(request);
    }

    private void runAsyncFormat(FormattingRequestImpl formattingRequest) {
        underProgressIfNeeded(
            Objects.requireNonNull(formattingRequest.myTask).isRunUnderProgress() && !formattingRequest.myIsSync,
            formattingRequest.getContext().getProject(),
            indicator -> {
                try {
                    formattingRequest.runAndAwaitTask(indicator);
                }
                finally {
                    myPendingRequests.remove(formattingRequest.myDocument, formattingRequest);
                }
            }
        );
    }

    private interface ProgressBlock {
        void run(@Nullable ProgressIndicator indicator);
    }

    private void underProgressIfNeeded(boolean isNeeded, Project project, ProgressBlock block) {
        if (isNeeded) {
            Task.Backgroundable task = new Task.Backgroundable(project, CodeStyleLocalize.asyncFormattingServiceRunning(getName(myService)), true) {
                @Override
                public void run(ProgressIndicator indicator) {
                    block.run(indicator);
                }
            };
            task.setCancelText(CodeStyleLocalize.asyncFormattingServiceCancel(getName(myService)));
            ProgressManager.getInstance().run(task);
        }
        else {
            block.run(null);
        }
    }

    private final class FormattingRequestImpl implements AsyncFormattingRequest {
        private final FormattingContext myContext;
        private final Document myDocument;
        private final List<TextRange> myRanges;
        private final boolean myCanChangeWhitespaceOnly;
        private final boolean myQuickFormat;
        private final boolean myIsSync;
        private final long myInitialModificationStamp;

        private volatile @Nullable FormattingTask myTask;
        private volatile @Nullable ModalityState myModalityState;

        private final CompletableFuture<Void> myTaskStarted = new CompletableFuture<>();
        private final CompletableFuture<@Nullable String> myTaskResult = new CompletableFuture<>();

        private FormattingRequestImpl(
            FormattingContext context,
            Document document,
            List<TextRange> ranges,
            boolean canChangeWhitespaceOnly,
            boolean quickFormat,
            boolean isSync
        ) {
            myContext = context;
            myDocument = document;
            myRanges = ranges;
            myCanChangeWhitespaceOnly = canChangeWhitespaceOnly;
            myQuickFormat = quickFormat;
            myIsSync = isSync;
            myInitialModificationStamp = document.getModificationStamp();
        }

        boolean cancel() {
            if (!myTaskStarted.isDone()) {
                return false;
            }
            // for our purpose, taskResult.cancel is equivalent, but we need the CAS semantics
            if (myTaskResult.completeExceptionally(new CancellationException())) {
                FormattingTask formattingTask = Objects.requireNonNull(myTask);
                return formattingTask.cancel();
            }
            return false;
        }

        private void notifyExpired() {
            FormattingNotificationService.getInstance(myContext.getProject()).reportError(
                getNotificationGroup(myService),
                getTimeoutNotificationDisplayId(myService),
                getName(myService),
                CodeStyleLocalize.asyncFormattingServiceTimeout(getName(myService), String.valueOf(getTimeout(myService, myContext).getSeconds())),
                getTimeoutActions(myService, myContext)
            );
        }

        void runAndAwaitTask(@Nullable ProgressIndicator indicator) {
            FormattingTask task = Objects.requireNonNull(myTask);
            // There is an existing contract that a task that was already created is also started.
            CompletableFuture<Void> taskJob = new CompletableFuture<>();
            Runnable taskRunnable = () -> {
                try {
                    runUnderIndicatorIfNeeded(task.isRunUnderProgress(), indicator, () -> {
                        myTaskStarted.complete(null);
                        task.run();
                    });
                    taskJob.complete(null);
                }
                catch (Throwable t) {
                    // unblock waiter if failure happened before `taskStarted.complete(Unit)` could run
                    myTaskStarted.completeExceptionally(t);
                    myTaskResult.completeExceptionally(t);
                    taskJob.completeExceptionally(t);
                }
            };
            if (myIsSync) {
                // FormattingTask.run must be called from the calling thread in sync mode
                taskRunnable.run();
            }
            else {
                AppExecutorUtil.getAppExecutorService().execute(taskRunnable);
            }

            Duration timeout = getTimeout(myService, myContext);
            long markStarted = System.nanoTime();
            try {
                await(myTaskStarted, null, -1);
                String formattedText = await(myTaskResult, indicator, timeout.toNanos());
                if (formattedText == null) {
                    return;
                }
                if (myIsSync) {
                    updateDocument(formattedText);
                }
                else {
                    Application.get().invokeAndWait(
                        () -> CommandProcessor.getInstance().runUndoTransparentAction(
                            () -> WriteAction.run(() -> updateDocument(formattedText))
                        ),
                        Objects.requireNonNull(myModalityState)
                    );
                }
            }
            catch (Throwable t) {
                if (!isCancellation(t)) {
                    LOG.error(t);
                }
                cancel();
            }
            finally {
                long remainingToTimeout = timeout.toNanos() - (System.nanoTime() - markStarted);
                if (!awaitQuietly(taskJob, remainingToTimeout)) {
                    notifyExpired();
                }
            }
        }

        @Override
        public @Nullable Path getIOFile() {
            VirtualFile originalFile = myContext.getVirtualFile();
            String ext;
            Charset charset;
            if (originalFile != null) {
                if (originalFile.getFileSystem() instanceof ArchiveFileSystem) {
                    // a file inside an archive has no path on the machine
                    return null;
                }
                Path path = getMachinePath(originalFile);
                if (path != null) {
                    return path;
                }
                ext = originalFile.getExtension();
                charset = originalFile.getCharset();
            }
            else {
                ext = myContext.getContainingFile().getFileType().getDefaultExtension();
                charset = ApplicationEncodingManager.getInstance().getDefaultCharset();
            }
            try {
                // the temporary file is created on the machine of the project
                Path tempFile = myContext.getProject().getPlatform().fs().createTempFile(TEMP_FILE_PREFIX, "." + ext);
                try (Writer writer = Files.newBufferedWriter(tempFile, charset)) {
                    writer.write(getDocumentText());
                }
                return tempFile;
            }
            catch (IOException e) {
                LOG.warn(e);
                return null;
            }
        }

        void updateDocument(String newText) {
            if (!needToUpdate(myService)) {
                return;
            }
            if (myDocument.getModificationStamp() > myInitialModificationStamp) {
                for (DocumentMerger merger : Application.get().getExtensionList(DocumentMerger.class)) {
                    if (merger.updateDocument(myDocument, newText)) {
                        break;
                    }
                }
            }
            else {
                myDocument.setText(newText);
            }
        }

        @Override
        public void onTextReady(@Nullable String updatedText) {
            myTaskResult.complete(updatedText);
        }

        @Override
        public void onError(LocalizeValue title, LocalizeValue message) {
            onError(title, message, (String) null);
        }

        @Override
        public void onError(LocalizeValue title, LocalizeValue message, @Nullable String displayId) {
            if (myTaskResult.complete(null)) {
                FormattingNotificationService.getInstance(myContext.getProject())
                    .reportError(getNotificationGroup(myService), displayId, title, message);
            }
        }

        @Override
        public void onError(LocalizeValue title, LocalizeValue message, int offset) {
            onError(title, message, null, offset);
        }

        @Override
        public void onError(LocalizeValue title, LocalizeValue message, @Nullable String displayId, int offset) {
            if (myTaskResult.complete(null)) {
                FormattingNotificationService.getInstance(myContext.getProject())
                    .reportErrorAndNavigate(getNotificationGroup(myService), displayId, title, message, myContext, offset);
            }
        }

        @Override
        public String getDocumentText() {
            return myDocument.getText();
        }

        @Override
        public List<TextRange> getFormattingRanges() {
            return myRanges;
        }

        @Override
        public boolean canChangeWhitespaceOnly() {
            return myCanChangeWhitespaceOnly;
        }

        @Override
        public FormattingContext getContext() {
            return myContext;
        }

        @Override
        public boolean isQuickFormat() {
            return myQuickFormat;
        }
    }

    /**
     * The path of the file on the machine of its file system (the local one or a remote one), {@code null} if the file has none.
     */
    private static @Nullable Path getMachinePath(VirtualFile file) {
        if (file.isInLocalFileSystem()) {
            return file.toNioPath();
        }
        return file.getFileSystem().getNioPath(file);
    }

    private static void runUnderIndicatorIfNeeded(boolean isNeeded, @Nullable ProgressIndicator indicator, Runnable block) {
        if (isNeeded && indicator != null) {
            ProgressManager.getInstance().runProcess(block, indicator);
        }
        else {
            block.run();
        }
    }

    /**
     * Waits for the future, checking the progress indicator for cancellation.
     *
     * @param timeoutNanos the timeout, or a negative value to wait without a timeout
     */
    private static <T> T await(CompletableFuture<T> future, @Nullable ProgressIndicator indicator, long timeoutNanos)
        throws Throwable {
        long deadline = System.nanoTime() + timeoutNanos;
        while (true) {
            if (indicator != null) {
                indicator.checkCanceled();
            }
            long waitNanos = TimeUnit.MILLISECONDS.toNanos(10);
            if (timeoutNanos >= 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new TimeoutException();
                }
                waitNanos = Math.min(waitNanos, remaining);
            }
            try {
                return future.get(waitNanos, TimeUnit.NANOSECONDS);
            }
            catch (TimeoutException ignored) {
            }
            catch (ExecutionException | CompletionException e) {
                throw e.getCause() != null ? e.getCause() : e;
            }
        }
    }

    private static boolean awaitQuietly(CompletableFuture<?> future, long timeoutNanos) {
        if (timeoutNanos <= 0) {
            return false;
        }
        try {
            future.get(timeoutNanos, TimeUnit.NANOSECONDS);
        }
        catch (TimeoutException e) {
            return false;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (ExecutionException | CancellationException ignored) {
        }
        return true;
    }

    private static boolean isCancellation(Throwable t) {
        return t instanceof CancellationException || t instanceof TimeoutException || t instanceof ProcessCanceledException;
    }
}
