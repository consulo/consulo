/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.codeStyle;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.internal.CodeStyleInternalHelper;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a model of the document containing the formatted text, as seen by the
 * formatter. Allows a formatter to access information about the document.
 *
 * @see FormattingModel#getDocumentModel()
 */

public interface FormattingDocumentModel {
    @Nonnull
    static FormattingDocumentModel create(@Nonnull PsiFile file) {
        return CodeStyleInternalHelper.getInstance().createFormattingDocumentModel(file);
    }

    @Nonnull
    static FormattingDocumentModel create(@Nonnull Document document, @Nullable PsiFile file) {
        return CodeStyleInternalHelper.getInstance().createFormattingDocumentModel(document, file);
    }

    /**
     * Returns the line number corresponding to the specified offset in the document.
     *
     * @param offset the offset for which the line number is requested.
     * @return the line number corresponding to the offset.
     */
    int getLineNumber(int offset);

    /**
     * Returns the offset corresponding to the start of the specified line in the document.
     *
     * @param line the line number for which the offset is requested.
     * @return the start offset of the line.
     */
    int getLineStartOffset(int line);

    /**
     * Returns the text contained in the specified text range of the document.
     *
     * @param textRange the text range for which the text is requested.
     * @return the text at the specified text range.
     */
    CharSequence getText(TextRange textRange);

    /**
     * Returns the length of the entire document text.
     *
     * @return the document text length.
     */
    int getTextLength();

    @Nonnull
    Document getDocument();

    /**
     * Allows to answer if all document symbols from <code>[startOffset; endOffset)</code> region are treated as white spaces by formatter.
     *
     * @param startOffset target start document offset (inclusive)
     * @param endOffset   target end document offset (exclusive)
     * @return <code>true</code> if all document symbols from <code>[startOffset; endOffset)</code> region are treated
     * as white spaces by formatter; <code>false</code> otherwise
     */
    boolean containsWhiteSpaceSymbolsOnly(int startOffset, int endOffset);

    /**
     * There is a possible case that white space to apply should be additionally adjusted because of formatter processing. That is
     * true, for example, for Python where it may be mandatory to use <code>'\'</code> symbol at multi-line expression.
     * <p/>
     * Current method adjusts given white space text if necessary.
     *
     * @param whiteSpaceText white space text to use by default
     * @param startOffset    start offset of the document text that is intended to be replaced by the given white space text (inclusive)
     * @param endOffset      end offset of the document text that is intended to be replaced by the given white space text (exclusive)
     * @param nodeAfter      the AST node following the block, if known
     * @param changedViaPsi  flag that identifies whether formatter introduces changes via PSI tree or directly via the document
     * @return white space to use for replacing document symbols at <code>[startOffset; endOffset)</code> region
     */
    @Nonnull
    CharSequence adjustWhiteSpaceIfNecessary(
        @Nonnull CharSequence whiteSpaceText,
        int startOffset,
        int endOffset,
        @Nullable ASTNode nodeAfter,
        boolean changedViaPsi
    );

    @Nullable
    default PsiFile getFile() {
        return null;
    }
}
