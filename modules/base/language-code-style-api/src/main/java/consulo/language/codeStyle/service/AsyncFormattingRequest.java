// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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

import consulo.document.util.TextRange;
import consulo.language.codeStyle.FormattingContext;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Contains formatting data and methods handling formatting results.
 */
public interface AsyncFormattingRequest {
    /**
     * @return The document text to be formatted.
     */
    String getDocumentText();

    /**
     * @return The file in the local file system with the same content as returned by {@link #getDocumentText()} method. If
     * originally the document isn't associated with a physical file, a temporary file is created. The method returns {@code null} if the
     * file can't be created. The error message is logged.
     */
    @Nullable Path getIOFile();

    /**
     * @return A list of formatting ranges. It must be used by {@code asyncFormat()} implementation if {@code
     * AsyncDocumentFormattingService} supports range formatting: {@link FormattingService.Feature#FORMAT_FRAGMENTS} feature.
     */
    List<TextRange> getFormattingRanges();

    /**
     * @return True if only whitespaces changes are allowed.
     */
    boolean canChangeWhitespaceOnly();

    /**
     * @return True if the service must provide a quick ad-hoc formatting rather than a long-lasting document processing.
     * @see FormattingService.Feature#AD_HOC_FORMATTING
     */
    boolean isQuickFormat();

    /**
     * @return The current {@link FormattingContext}. Note: use {@link #getFormattingRanges()} instead of
     * {@link FormattingContext#getFormattingRange()} to get proper ranges which can be modified if formatting service supports range
     * formatting.
     */
    FormattingContext getContext();

    /**
     * Call this method when the result (formatted text) is available, or if there's no need to update the document text.
     * If the original document has changed, the result will be merged with
     * an available {@link DocumentMerger} extension. If there are no suitable document merge extensions, the result will be ignored.
     * <p>
     * <b>Note:</b> {@code onTextReady()} may be called only once, subsequent calls will be ignored.
     *
     * @param updatedText new document text or {@code null} if the document text should not be changed
     */
    void onTextReady(@Nullable String updatedText);

    /**
     * Show an error notification to an end user. The notification uses {@link AsyncDocumentFormattingService#getNotificationGroup()}.
     * <p>
     * <b>Note:</b> {@code onError()} may be called only once, subsequent calls will be ignored.
     *
     * @param title   The notification title.
     * @param message The notification message.
     */
    void onError(LocalizeValue title, LocalizeValue message);

    /**
     * Show an error notification to an end user. The notification uses {@link AsyncDocumentFormattingService#getNotificationGroup()}.
     * <p>
     * <b>Note:</b> {@code onError()} may be called only once, subsequent calls will be ignored.
     *
     * @param title     The notification title.
     * @param message   The notification message.
     * @param displayId The notification displayId.
     */
    default void onError(LocalizeValue title, LocalizeValue message, @Nullable String displayId) {
        onError(title, message);
    }

    /**
     * Show an error notification to an end user. The notification uses {@link AsyncDocumentFormattingService#getNotificationGroup()}. In
     * addition, either navigate to the given offset in the open document or create a navigation link to a file from the formatting context.
     * <p>
     * <b>Note:</b> {@code onError()} may be called only once, subsequent calls will be ignored.
     *
     * @param title   The notification title.
     * @param message The notification message.
     * @param offset  Offset in the document to navigate to or -1 for no offset.
     */
    void onError(LocalizeValue title, LocalizeValue message, int offset);

    /**
     * Show an error notification to an end user. The notification uses {@link AsyncDocumentFormattingService#getNotificationGroup()}. In
     * addition, either navigate to the given offset in the open document or create a navigation link to a file from the formatting context.
     * <p>
     * <b>Note:</b> {@code onError()} may be called only once, subsequent calls will be ignored.
     *
     * @param title     The notification title.
     * @param message   The notification message.
     * @param displayId The notification displayId.
     * @param offset    Offset in the document to navigate to or -1 for no offset.
     */
    default void onError(LocalizeValue title, LocalizeValue message, @Nullable String displayId, int offset) {
        onError(title, message, offset);
    }
}
