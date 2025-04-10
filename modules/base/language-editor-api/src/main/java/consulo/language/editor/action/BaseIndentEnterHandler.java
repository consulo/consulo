/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.editor.action;

import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionUtil;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.codeEditor.util.EditorUtil;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Created by IntelliJ IDEA.
 *
 * @author oleg
 * @date 11/17/10
 */
public class BaseIndentEnterHandler extends EnterHandlerDelegateAdapter {
    private final Language myLanguage;
    private final TokenSet myIndentTokens;
    private final IElementType myLineCommentType;
    private final String myLineCommentPrefix;
    private final TokenSet myWhitespaceTokens;
    private final boolean myWorksWithFormatter;

    public BaseIndentEnterHandler(
        final Language language,
        final TokenSet indentTokens,
        final IElementType lineCommentType,
        final String lineCommentPrefix,
        final TokenSet whitespaceTokens
    ) {
        this(language, indentTokens, lineCommentType, lineCommentPrefix, whitespaceTokens, false);
    }


    public BaseIndentEnterHandler(
        final Language language,
        final TokenSet indentTokens,
        final IElementType lineCommentType,
        final String lineCommentPrefix,
        final TokenSet whitespaceTokens,
        final boolean worksWithFormatter
    ) {
        myLanguage = language;
        myIndentTokens = indentTokens;
        myLineCommentType = lineCommentType;
        myLineCommentPrefix = lineCommentPrefix;
        myWhitespaceTokens = whitespaceTokens;
        myWorksWithFormatter = worksWithFormatter;
    }

    protected Result shouldSkipWithResult(
        @Nonnull final PsiFile file,
        @Nonnull final Editor editor,
        @Nonnull final DataContext dataContext
    ) {
        final Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return Result.Continue;
        }

        if (!file.getViewProvider().getLanguages().contains(myLanguage)) {
            return Result.Continue;
        }

        if (editor.isViewer()) {
            return Result.Continue;
        }

        final Document document = editor.getDocument();
        if (!document.isWritable()) {
            return Result.Continue;
        }

        PsiDocumentManager.getInstance(project).commitDocument(document);

        int caret = editor.getCaretModel().getOffset();
        if (caret == 0) {
            return Result.DefaultSkipIndent;
        }
        if (caret <= 0) {
            return Result.Continue;
        }
        return null;
    }

    @Override
    public Result preprocessEnter(
        @Nonnull final PsiFile file,
        @Nonnull final Editor editor,
        @Nonnull final Ref<Integer> caretOffset,
        @Nonnull final Ref<Integer> caretAdvance,
        @Nonnull final DataContext dataContext,
        final EditorActionHandler originalHandler
    ) {
        Result res = shouldSkipWithResult(file, editor, dataContext);
        if (res != null) {
            return res;
        }

        final Document document = editor.getDocument();
        int caret = editor.getCaretModel().getOffset();
        final int lineNumber = document.getLineNumber(caret);

        final int lineStartOffset = document.getLineStartOffset(lineNumber);
        final int previousLineStartOffset = lineNumber > 0 ? document.getLineStartOffset(lineNumber - 1) : lineStartOffset;
        final EditorHighlighter highlighter = editor.getHighlighter();
        final HighlighterIterator iterator = highlighter.createIterator(caret - 1);
        final IElementType type = getNonWhitespaceElementType(iterator, lineStartOffset, previousLineStartOffset);

        final CharSequence editorCharSequence = document.getCharsSequence();
        final CharSequence lineIndent =
            editorCharSequence.subSequence(lineStartOffset, EditorActionUtil.findFirstNonSpaceOffsetOnTheLine(document, lineNumber));

        // Enter in line comment
        if (type == myLineCommentType) {
            final String restString = editorCharSequence.subSequence(caret, document.getLineEndOffset(lineNumber)).toString();
            if (!StringUtil.isEmptyOrSpaces(restString)) {
                final String linePrefix = lineIndent + myLineCommentPrefix;
                EditorModificationUtil.insertStringAtCaret(editor, "\n" + linePrefix);
                editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(lineNumber + 1, linePrefix.length()));
                return Result.Stop;
            }
            else if (iterator.getStart() < lineStartOffset) {
                EditorModificationUtil.insertStringAtCaret(editor, "\n" + lineIndent);
                return Result.Stop;
            }
        }

        if (!myWorksWithFormatter && FormattingModelBuilder.forLanguage(myLanguage) != null) {
            return Result.Continue;
        }
        else {
            if (myIndentTokens.contains(type)) {
                final String newIndent = getNewIndent(file, document, lineIndent);
                EditorModificationUtil.insertStringAtCaret(editor, "\n" + newIndent);
                return Result.Stop;
            }

            EditorModificationUtil.insertStringAtCaret(editor, "\n" + lineIndent);
            editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(lineNumber + 1, calcLogicalLength(editor, lineIndent)));
            return Result.Stop;
        }
    }

    protected String getNewIndent(
        @Nonnull final PsiFile file,
        @Nonnull final Document document,
        @Nonnull final CharSequence oldIndent
    ) {
        CharSequence nonEmptyIndent = oldIndent;
        final CharSequence editorCharSequence = document.getCharsSequence();
        final int nLines = document.getLineCount();
        for (int line = 0; line < nLines && nonEmptyIndent.length() == 0; ++line) {
            final int lineStart = document.getLineStartOffset(line);
            final int indentEnd = EditorActionUtil.findFirstNonSpaceOffsetOnTheLine(document, line);
            if (lineStart < indentEnd) {
                nonEmptyIndent = editorCharSequence.subSequence(lineStart, indentEnd);
            }
        }

        final boolean usesSpacesForIndentation = nonEmptyIndent.length() > 0 && nonEmptyIndent.charAt(nonEmptyIndent.length() - 1) == ' ';
        final boolean firstIndent = nonEmptyIndent.length() == 0;

        final CodeStyleSettings currentSettings = CodeStyleSettingsManager.getSettings(file.getProject());
        final CommonCodeStyleSettings.IndentOptions indentOptions = currentSettings.getIndentOptions(file.getFileType());
        if (firstIndent && indentOptions.USE_TAB_CHARACTER || !firstIndent && !usesSpacesForIndentation) {
            int nTabsToIndent = indentOptions.INDENT_SIZE / indentOptions.TAB_SIZE;
            if (indentOptions.INDENT_SIZE % indentOptions.TAB_SIZE != 0) {
                ++nTabsToIndent;
            }
            return oldIndent + StringUtil.repeatSymbol('\t', nTabsToIndent);
        }
        return oldIndent + StringUtil.repeatSymbol(' ', indentOptions.INDENT_SIZE);
    }

    private static int calcLogicalLength(Editor editor, CharSequence lineIndent) {
        int result = 0;
        for (int i = 0; i < lineIndent.length(); i++) {
            if (lineIndent.charAt(i) == '\t') {
                result += EditorUtil.getTabSize(editor);
            }
            else {
                result++;
            }
        }
        return result;
    }

    @Nullable
    protected IElementType getNonWhitespaceElementType(
        final HighlighterIterator iterator,
        int currentLineStartOffset,
        final int prevLineStartOffset
    ) {
        while (!iterator.atEnd() && iterator.getEnd() >= currentLineStartOffset && iterator.getStart() >= prevLineStartOffset) {
            final IElementType tokenType = (IElementType)iterator.getTokenType();
            if (!myWhitespaceTokens.contains(tokenType)) {
                return tokenType;
            }
            iterator.retreat();
        }
        return null;
    }
}
