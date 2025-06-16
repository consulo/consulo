/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.codeStyle.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.FormattingDocumentModel;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

/**
 * @author VISTALL
 * @since 30-Apr-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeStyleInternalHelper {
    static CodeStyleInternalHelper getInstance() {
        return Application.get().getInstance(CodeStyleInternalHelper.class);
    }

    void debugTreeToBuffer(@Nonnull Appendable buffer,
                           @Nonnull ASTNode root,
                           int indent,
                           boolean skipWhiteSpaces,
                           boolean showRanges,
                           boolean showChildrenRanges,
                           boolean usePsi);

    void replaceLastWhiteSpace(ASTNode astNode, String whiteSpace, TextRange textRange);

    void replaceWhiteSpace(String whiteSpace, ASTNode leafElement, IElementType whiteSpaceToken, @Nullable TextRange textRange);

    void replaceInnerWhiteSpace(@Nonnull String newWhiteSpaceText, @Nonnull ASTNode holder, @Nonnull TextRange whiteSpaceRange);

    boolean containsWhiteSpacesOnly(@Nullable ASTNode node);

    @Nullable
    ASTNode getPreviousNonWhitespaceLeaf(@Nullable ASTNode node);

    void allowToMarkNodesForPostponedFormatting(boolean value);

    FormattingDocumentModel createFormattingDocumentModel(PsiFile file);

    FormattingDocumentModel createFormattingDocumentModel(@Nonnull Document document, @Nullable PsiFile file);

    int nextTabStop(int x, @Nonnull Object editor, int tabSize);

    int nextTabStop(int x, @Nonnull Object editor);

    int charWidth(char c, @JdkConstants.FontStyle int fontType, @Nonnull Object editor);

    int getSpaceWidth(@JdkConstants.FontStyle int fontType, @Nonnull Object editor);

    @RequiredUIAccess
    void showDetectIndentSettings(@Nullable Project project);
}
