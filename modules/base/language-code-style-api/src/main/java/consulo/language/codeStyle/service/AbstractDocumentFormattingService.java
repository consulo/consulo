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

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.FormattingMode;
import consulo.language.codeStyle.FormattingRangesInfo;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;

import java.util.Collections;
import java.util.List;

/**
 * Base class for synchronous document formatting services.
 */
public abstract class AbstractDocumentFormattingService implements FormattingService {
    private static final Key<Document> DOCUMENT_KEY = Key.create("formatting.service.document");

    @Override
    public final PsiElement formatElement(PsiElement element, boolean canChangeWhiteSpaceOnly) {
        return formatElement(element, element.getTextRange(), canChangeWhiteSpaceOnly);
    }

    @Override
    public final PsiElement formatElement(PsiElement element, TextRange range, boolean canChangeWhiteSpaceOnly) {
        PsiFile file = element.getContainingFile();
        FormattingContext formattingContext = FormattingContext.create(file, range, CodeStyle.getSettings(file), FormattingMode.REFORMAT);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
        Document document = documentManager.getDocument(file);
        if (document == null) {
            document = file.getUserData(DOCUMENT_KEY);
        }
        if (document != null) {
            int offset = element.getTextOffset();
            formatDocument(document, Collections.singletonList(range), formattingContext, canChangeWhiteSpaceOnly, false);
            documentManager.commitDocument(document);
            PsiElement resultingElement = file.findElementAt(offset);
            if (resultingElement != null) {
                return resultingElement;
            }
        }
        return element;
    }

    @Override
    public final void formatRanges(
        PsiFile file,
        FormattingRangesInfo rangesInfo,
        boolean canChangeWhiteSpaceOnly,
        boolean quickFormat
    ) {
        TextRange boundRange = ObjectUtil.notNull(rangesInfo.getBoundRange(), file.getTextRange());
        FormattingContext formattingContext = FormattingContext.create(file, boundRange, CodeStyle.getSettings(file), FormattingMode.REFORMAT);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
        Document document = documentManager.getDocument(file);
        if (document != null) {
            formatDocument(document, rangesInfo.getTextRanges(), formattingContext, canChangeWhiteSpaceOnly, quickFormat);
            documentManager.commitDocument(document);
        }
    }


    /**
     * Formats the document within the specified ranges.
     *
     * @param document                The document to format.
     * @param formattingRanges        Ranges in which the document should be formatted.
     * @param formattingContext       Formatting information, see {@link FormattingContext}
     * @param canChangeWhiteSpaceOnly True if only whitespace can be changed (indents, spacing)
     * @param quickFormat             True if only quick adjustments are allowed, e.g. after a quick fix or refactoring.
     */
    public abstract void formatDocument(
        Document document,
        List<TextRange> formattingRanges,
        FormattingContext formattingContext,
        boolean canChangeWhiteSpaceOnly,
        boolean quickFormat
    );


    public static void setDocument(PsiFile file, Document document) {
        file.putUserData(DOCUMENT_KEY, document);
    }
}
