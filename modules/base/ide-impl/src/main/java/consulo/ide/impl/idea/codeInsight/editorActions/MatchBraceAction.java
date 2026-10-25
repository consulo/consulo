// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.language.editor.action.BraceHighlightingAndNavigationContext;
import consulo.language.editor.action.BraceMatchingUtil;
import consulo.language.editor.highlight.BraceMatcher;
import consulo.language.editor.highlight.CodeBlockSupportHandler;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiFile;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

/**
 * Moves caret to the matching brace:
 * <ul>
 * <li>If caret is on the closing brace - moves in the beginning of the matching opening brace</li>
 * <li>If caret is on the opening brace - moves to the end of the matching closing brace</li>
 * <li>Otherwise moves from the caret position to the beginning of the file and finds first opening brace not closed before the caret position</li>
 * </ul>
 **/
@ActionImpl(id = "EditorMatchBrace")
public final class MatchBraceAction extends EditorAction {
    public MatchBraceAction() {
        super(CodeEditorLocalize.actionMatchBraceText(), new MyHandler());
        setInjectedContext(true);
    }

    private static final class MyHandler extends EditorActionHandler.ForEachCaret {
        @Override
        @RequiredUIAccess
        public void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            final PsiFile file = dataContext.getData(PsiFile.KEY);
            if (file == null) return;

            int targetOffset = getClosestTargetOffset(editor, file);

            if (targetOffset > -1) {
                moveCaret(editor, editor.getCaretModel().getCurrentCaret(), targetOffset);
            }
        }

        /**
         * Attempts to find the closest target offset for the caret. Uses {@link BraceMatcher} and {@link CodeBlockSupportHandler}
         *
         * @return target caret offset or -1 if uncomputable.
         */
        @RequiredReadAction
        private static int getClosestTargetOffset(Editor editor, PsiFile file) {
            int offsetFromBraceMatcher = getOffsetFromBraceMatcher(editor, file);
            TextRange rangeFromCodeBlockSupport = CodeBlockSupportHandler.findCodeBlockRange(editor, file);
            if (rangeFromCodeBlockSupport.isEmpty() || rangeFromCodeBlockSupport.contains(offsetFromBraceMatcher)) {
                return offsetFromBraceMatcher;
            }

            final EditorHighlighter highlighter = editor.getHighlighter();
            int caretOffset = editor.getCaretModel().getOffset();
            HighlighterIterator iterator = highlighter.createIterator(caretOffset);

            // end of file or at block closing token
            if (iterator.atEnd() || iterator.getEnd() == rangeFromCodeBlockSupport.getEndOffset() ||
                    // edge case - end of closing token
                    (caretOffset > 0 && highlighter.createIterator(caretOffset - 1).getEnd() == rangeFromCodeBlockSupport.getEndOffset())) {
                return rangeFromCodeBlockSupport.getStartOffset();
            }
            return rangeFromCodeBlockSupport.getEndOffset();
        }

        /**
         * @return offset to move caret to, computed from the brace matcher. If it's not possible to compute - returns {@code -1}
         * @implNote this code partially duplicates {@code BraceHighlightingHandler#updateBraces()} and probably can be extracted.
         */
        @RequiredReadAction
        private static int getOffsetFromBraceMatcher(Editor editor, PsiFile file) {
            BraceHighlightingAndNavigationContext matchingContext = BraceMatchingUtil.computeHighlightingAndNavigationContext(editor, file);
            return matchingContext != null ? matchingContext.navigationOffset() : tryFindPreviousUnclosedOpeningBraceOffset(editor, file);
        }

        /**
         * Moving back from the caret position closing and opening braces (in dumb way, no need to be same type or whatever). Stops if
         * encounters first opening brace which was not closed before.
         *
         * @return start offset of the opening brace or -1 if non were found.
         */
        @RequiredReadAction
        private static int tryFindPreviousUnclosedOpeningBraceOffset(Editor editor, PsiFile file) {
            final EditorHighlighter highlighter = BraceMatchingUtil.getLazyParsableHighlighterIfAny(file.getProject(), editor, file);
            final CharSequence text = editor.getDocument().getCharsSequence();
            final HighlighterIterator iterator = highlighter.createIterator(editor.getCaretModel().getOffset());
            final FileType fileType = iterator.atEnd() ? null : PsiUtilBase.getPsiFileAtOffset(file, iterator.getStart()).getFileType();

            if (fileType == null) {
                return -1;
            }

            int unopenedBraces = 0;
            while (true) {
                if (BraceMatchingUtil.isRBraceToken(iterator, text, fileType)) {
                    unopenedBraces++;
                }
                else if (BraceMatchingUtil.isLBraceToken(iterator, text, fileType)) {
                    unopenedBraces--;
                }
                if (unopenedBraces < 0) {
                    return iterator.getStart();
                }

                if (iterator.getStart() == 0) {
                    return -1;
                }
                iterator.retreat();
            }
        }
    }

    private static void moveCaret(Editor editor, Caret caret, int offset) {
        caret.removeSelection();
        caret.moveToOffset(offset);
        EditorModificationUtil.scrollToCaret(editor);
    }
}
