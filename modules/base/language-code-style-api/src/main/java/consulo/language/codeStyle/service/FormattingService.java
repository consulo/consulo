// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.FormattingRangesInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Base class for any kind of formatting tools, both built-in and external. The default implementation is {@code CoreFormattingService}
 * based on {@link consulo.language.codeStyle.FormattingDocumentModel}. Alternative implementations are searched using
 * {@link #canFormat(PsiFile, Feature...)} method and features supported by a tool {@link #getFeatures()}.
 * <p>
 * For integration with external command line formatting tools consider using {@link AsyncDocumentFormattingService}.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FormattingService {
    /**
     * A feature supported by the service.
     */
    enum Feature {
        /**
         * The service can provide fast formatting of multiple collected ranges, for example, upon refactoring. It must also support
         * {@link #FORMAT_FRAGMENTS} feature.
         */
        AD_HOC_FORMATTING,
        /**
         * The service can format multiple text ranges within the same document as opposed to complete file only.
         */
        FORMAT_FRAGMENTS
    }

    /**
     * @return The features supported by the service or an empty list if only the whole file formatting is supported.
     */
    Set<Feature> getFeatures();

    /**
     * @param file The PSI file to check service availability for.
     * @return True if the service can be used to format the file, false otherwise.
     */
    @RequiredReadAction
    default boolean canFormat(PsiFile file, Feature... features) {
        return canFormat(file);
    }

    @RequiredReadAction
    boolean canFormat(PsiFile file);

    /**
     * Format the element and return its formatted equivalent.
     *
     * @param element                 The original element.
     * @param canChangeWhiteSpaceOnly True if only whitespaces can be changed (indents, spacing), other elements can't be inserted, changed
     *                                or removed.
     * @return The formatted equivalent of the original element.
     */
    PsiElement formatElement(PsiElement element, boolean canChangeWhiteSpaceOnly);

    /**
     * Format the element within the given text range and return its formatted equivalent.
     *
     * @param element                 The original element.
     * @param range                   The text range to format within.
     * @param canChangeWhiteSpaceOnly True if only whitespaces can be changed (indents, spacing), other elements can't be inserted, changed
     *                                or removed.
     * @return The formatted equivalent of the original element.
     */
    PsiElement formatElement(PsiElement element, TextRange range, boolean canChangeWhiteSpaceOnly);

    /**
     * Format multiple ranges within the PSI file.
     *
     * @param file                    The PSI file to format.
     * @param rangesInfo              Ranges information, see {@link FormattingRangesInfo}
     * @param canChangeWhiteSpaceOnly True if only whitespaces can be changed (indents, spacing), other elements can't be inserted, changed
     *                                or removed.
     * @param quickFormat             Only quick format is allowed. The {@code true} value is passed if the service supports
     *                                {@link Feature#AD_HOC_FORMATTING}. This mode is used, for example, in quick fixes and refactorings.
     */
    void formatRanges(PsiFile file, FormattingRangesInfo rangesInfo, boolean canChangeWhiteSpaceOnly, boolean quickFormat);

    /**
     * @return A class of the service which should be run prior to this FormattingService (default is {@code null})
     * <p>
     * For example:
     * <pre><code>
     *   Class<? extends FormattingService> runAfter() {
     *     return CoreFormattingService.class;
     *   }
     * </code></pre>
     * will call platform formatter prior to executing the current formatting service.
     * <p>
     * <b>NOTE:</b> It works only if all the file content is formatted. In case of subrange(s) only the current service
     * is called since original ranges become invalid after formatting.
     */
    default @Nullable Class<? extends FormattingService> runAfter() {
        return null;
    }
}
