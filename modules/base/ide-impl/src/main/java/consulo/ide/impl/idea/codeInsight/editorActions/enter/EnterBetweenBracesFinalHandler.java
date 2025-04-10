// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.editorActions.enter;

import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.action.CodeDocumentationUtil;
import consulo.ide.impl.idea.codeInsight.editorActions.EnterHandler;
import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.language.editor.action.EnterBetweenBracesDelegate;
import consulo.language.editor.action.EnterHandlerDelegateAdapter;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Please, don't extend the class.
 * Use the {@code EnterBetweenBracesDelegate} language-specific implementation instead.
 */
public abstract class EnterBetweenBracesFinalHandler extends EnterHandlerDelegateAdapter {
    @Override
    public Result preprocessEnter(
        @Nonnull final PsiFile file,
        @Nonnull final Editor editor,
        @Nonnull final Ref<Integer> caretOffsetRef,
        @Nonnull final Ref<Integer> caretAdvance,
        @Nonnull final DataContext dataContext,
        final EditorActionHandler originalHandler
    ) {
        if (!CodeInsightSettings.getInstance().SMART_INDENT_ON_ENTER) {
            return Result.Continue;
        }

        Document document = editor.getDocument();
        CharSequence text = document.getCharsSequence();
        int caretOffset = caretOffsetRef.get().intValue();

        final EnterBetweenBracesDelegate helper = getLanguageImplementation(EnterHandler.getLanguage(dataContext));

        if (!isApplicable(file, editor, text, caretOffset, helper)) {
            return Result.Continue;
        }

        final int line = document.getLineNumber(caretOffset);
        final int start = document.getLineStartOffset(line);
        final CodeDocumentationUtil.CommentContext commentContext =
            CodeDocumentationUtil.tryParseCommentContext(file, text, caretOffset, start);

        // special case: enter inside "()" or "{}"
        String indentInsideJavadoc = helper.isInComment(file, editor, caretOffset) && commentContext.docAsterisk
            ? CodeDocumentationUtil.getIndentInsideJavadoc(document, caretOffset)
            : null;

        originalHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), dataContext);

        Project project = editor.getProject();
        if (indentInsideJavadoc != null && project != null
            && CodeStyleManager.getInstance(project).getDocCommentSettings(file).isLeadingAsteriskEnabled()) {
            document.insertString(editor.getCaretModel().getOffset(), "*" + indentInsideJavadoc);
        }

        helper.formatAtOffset(file, editor, editor.getCaretModel().getOffset(), EnterHandler.getLanguage(dataContext));
        return indentInsideJavadoc == null ? Result.Continue : Result.DefaultForceIndent;
    }

    protected boolean isApplicable(
        @Nonnull PsiFile file,
        @Nonnull Editor editor,
        CharSequence documentText,
        int caretOffset,
        EnterBetweenBracesDelegate helper
    ) {
        if (!helper.isApplicable(file, editor, documentText, caretOffset)) {
            return false;
        }

        int prevCharOffset = CharArrayUtil.shiftBackward(documentText, caretOffset - 1, " \t");
        int nextCharOffset = CharArrayUtil.shiftForward(documentText, caretOffset, " \t");
        return isValidOffset(prevCharOffset, documentText) &&
            isValidOffset(nextCharOffset, documentText) &&
            helper.isBracePair(documentText.charAt(prevCharOffset), documentText.charAt(nextCharOffset)) &&
            !helper.bracesAreInTheSameElement(file, editor, prevCharOffset, nextCharOffset);
    }

    @Nonnull
    protected EnterBetweenBracesDelegate getLanguageImplementation(@Nullable Language language) {
        if (language != null) {
            final EnterBetweenBracesDelegate helper = EnterBetweenBracesDelegate.forLanguage(language);
            if (helper != null) {
                return helper;
            }
        }
        return ourDefaultBetweenDelegate;
    }

    protected static EnterBetweenBracesDelegate ourDefaultBetweenDelegate = new EnterBetweenBracesDelegate() {
        @Nonnull
        @Override
        public Language getLanguage() {
            return Language.ANY;
        }
    };

    protected static boolean isValidOffset(int offset, CharSequence text) {
        return offset >= 0 && offset < text.length();
    }
}
