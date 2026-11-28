// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.codeStyle.service;

import consulo.application.Application;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.internal.AsyncDocumentFormattingSupport;
import consulo.language.codeStyle.internal.AsyncDocumentFormattingSupportFactory;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationGroup;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * Extend this class if there is a long-lasting formatting operation which may block EDT. The actual formatting code is placed then
 * in {@link FormattingTask#run()} method which may be slow.
 * <p>
 * If another {@code formatDocument()} call is made for the same document, the previous request is cancelled. On success, if
 * {@code cancel()} returns {@code true}, another request replaces the previous one. Otherwise, the newer request is rejected.
 * <p>
 * Before the actual formatting starts, {@link #createFormattingTask(AsyncFormattingRequest)} method is called. It should be fast enough not to
 * block EDT. If it succeeds (doesn't return null), further formatting is started using the created runnable on a separate thread.
 */
public abstract class AsyncDocumentFormattingService extends AbstractDocumentFormattingService {
    public static final Key<Boolean> FORMAT_DOCUMENT_SYNCHRONOUSLY =
        Key.create("consulo.language.codeStyle.service.AsyncDocumentFormattingService.FORMAT_DOCUMENT_SYNCHRONOUSLY");

    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final AsyncDocumentFormattingSupport mySupport = Application.get()
        .getInstance(AsyncDocumentFormattingSupportFactory.class).create(this);

    @Override
    public final void formatDocument(
        Document document,
        List<TextRange> formattingRanges,
        FormattingContext formattingContext,
        boolean canChangeWhiteSpaceOnly,
        boolean quickFormat
    ) {
        mySupport.formatDocument(document, formattingRanges, formattingContext, canChangeWhiteSpaceOnly, quickFormat);
    }

    /**
     * Called before the actual formatting starts.
     *
     * @param formattingRequest The formatting request to create the formatting task for.
     * @return {@link FormattingTask} if successful and formatting can proceed, {@code null} otherwise. The latter may be a result, for
     * example, of misconfiguration.
     */
    protected abstract @Nullable FormattingTask createFormattingTask(AsyncFormattingRequest formattingRequest);

    /**
     * @return A notification group to use when error messages are shown to an end user.
     */
    protected abstract NotificationGroup getNotificationGroup();

    /**
     * @return A notification display ID to use when timeout error messages are shown to an end user.
     */
    protected @Nullable String getTimeoutNotificationDisplayId() {
        return null;
    }

    protected boolean needToUpdate() {
        return true;
    }

    /**
     * Returns false if the task must always run outside the caller's read or write action.
     * Such services ignore {@link #FORMAT_DOCUMENT_SYNCHRONOUSLY} and the headless synchronous mode.
     * This capability controls execution mode and does not affect service selection.
     */
    protected boolean supportsSynchronousFormatting() {
        return true;
    }

    /**
     * @return A name which can be used in UI, for example, in notification messages.
     */
    protected abstract LocalizeValue getName();

    /**
     * Hook called before creating a formatting request. Default implementation saves the document
     * to ensure external formatters working with IO files see the latest content.
     */
    protected void prepareForFormatting(Document document, FormattingContext formattingContext) {
        FileDocumentManager.getInstance().saveDocument(document);
    }

    /**
     * @return A duration to wait for the service to respond (call either {@code onTextReady()} or {@code onError()}).
     */
    protected Duration getTimeout() {
        return DEFAULT_TIMEOUT;
    }

    protected Duration getTimeout(FormattingContext context) {
        return getTimeout();
    }

    protected AnAction[] getTimeoutActions(FormattingContext context) {
        return AnAction.EMPTY_ARRAY;
    }

    public interface FormattingTask extends Runnable {
        /**
         * Cancel the current runnable.
         * @return {@code true} if the runnable has been successfully cancelled, {@code false} otherwise.
         */
        boolean cancel();

        /**
         * @return True if the task must be run under progress (a progress indicator is created automatically). Otherwise, the task is
         * responsible for visualizing the progress by itself, it is just started on a background thread.
         */
        default boolean isRunUnderProgress() {
            return false;
        }
    }

    // region Internal Accessors
    public static @Nullable FormattingTask createFormattingTask(AsyncDocumentFormattingService service, AsyncFormattingRequest request) {
        return service.createFormattingTask(request);
    }

    public static NotificationGroup getNotificationGroup(AsyncDocumentFormattingService service) {
        return service.getNotificationGroup();
    }

    public static @Nullable String getTimeoutNotificationDisplayId(AsyncDocumentFormattingService service) {
        return service.getTimeoutNotificationDisplayId();
    }

    public static boolean needToUpdate(AsyncDocumentFormattingService service) {
        return service.needToUpdate();
    }

    public static boolean supportsSynchronousFormatting(AsyncDocumentFormattingService service) {
        return service.supportsSynchronousFormatting();
    }

    public static LocalizeValue getName(AsyncDocumentFormattingService service) {
        return service.getName();
    }

    public static void prepareForFormatting(
        AsyncDocumentFormattingService service,
        Document document,
        FormattingContext formattingContext
    ) {
        service.prepareForFormatting(document, formattingContext);
    }

    public static Duration getTimeout(AsyncDocumentFormattingService service) {
        return service.getTimeout();
    }

    public static Duration getTimeout(AsyncDocumentFormattingService service, FormattingContext context) {
        return service.getTimeout(context);
    }

    public static AnAction[] getTimeoutActions(AsyncDocumentFormattingService service, FormattingContext context) {
        return service.getTimeoutActions(context);
    }
    // endregion
}
