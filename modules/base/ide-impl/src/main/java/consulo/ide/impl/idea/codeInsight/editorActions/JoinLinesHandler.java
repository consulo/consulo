// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.internal.ApplicationEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.internal.DocumentEx;
import consulo.document.util.DocumentUtil;
import consulo.language.codeStyle.internal.FormatterEx;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.language.codeStyle.*;
import consulo.language.editor.action.JoinLinesHandlerDelegate;
import consulo.language.editor.action.JoinRawLinesHandlerDelegate;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static consulo.language.editor.action.JoinLinesHandlerDelegate.CANNOT_JOIN;

@ExtensionImpl(order = "first")
public class JoinLinesHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private static final Logger LOG = Logger.getInstance(JoinLinesHandler.class);
    private EditorActionHandler myOriginalHandler;

    public JoinLinesHandler() {
        super(true);
    }

    @Override
    public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
        assert caret != null;

        if (editor.isViewer() || !EditorModificationUtil.requestWriting(editor)) {
            return;
        }

        if (!(editor.getDocument() instanceof DocumentEx)) {
            myOriginalHandler.execute(editor, caret, dataContext);
            return;
        }
        DocumentEx doc = (DocumentEx)editor.getDocument();
        Project project = DataManager.getInstance().getDataContext(editor.getContentComponent()).getData(Project.KEY);
        if (project == null) {
            return;
        }

        PsiDocumentManager docManager = PsiDocumentManager.getInstance(project);
        PsiFile psiFile = docManager.getPsiFile(doc);

        if (psiFile == null) {
            myOriginalHandler.execute(editor, caret, dataContext);
            return;
        }

        LogicalPosition caretPosition = caret.getLogicalPosition();
        int startLine = caretPosition.line;
        int endLine = startLine + 1;
        if (caret.hasSelection()) {
            startLine = doc.getLineNumber(caret.getSelectionStart());
            endLine = doc.getLineNumber(caret.getSelectionEnd());
            if (doc.getLineStartOffset(endLine) == caret.getSelectionEnd()) {
                endLine--;
            }
        }

        if (endLine >= doc.getLineCount()) {
            return;
        }

        int lineCount = endLine - startLine;
        int line = startLine;

        ((ApplicationEx)Application.get()).runWriteActionWithCancellableProgressInDispatchThread("Join Lines", project, null, indicator -> {
            indicator.setIndeterminate(false);
            JoinLineProcessor processor = new JoinLineProcessor(doc, psiFile, line, indicator);
            processor.process(editor, caret, lineCount);
        });
    }

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_JOIN_LINES;
    }

    private static class JoinLineProcessor {
        @Nonnull
        private final DocumentEx myDoc;
        @Nonnull
        private final PsiFile myFile;
        private int myLine;
        @Nonnull
        private final PsiDocumentManager myManager;
        @Nonnull
        private final CodeStyleManager myStyleManager;
        @Nonnull
        private final ProgressIndicator myIndicator;
        int myCaretRestoreOffset = CANNOT_JOIN;

        JoinLineProcessor(@Nonnull DocumentEx doc, @Nonnull PsiFile file, int line, @Nonnull ProgressIndicator indicator) {
            myDoc = doc;
            myFile = file;
            myLine = line;
            myIndicator = indicator;
            Project project = file.getProject();
            myManager = PsiDocumentManager.getInstance(project);
            myStyleManager = CodeStyleManager.getInstance(project);
        }

        void process(@Nonnull Editor editor, @Nonnull Caret caret, int lineCount) {
            myStyleManager.performActionWithFormatterDisabled((Runnable)() -> doProcess(lineCount));
            positionCaret(editor, caret);
        }

        @RequiredWriteAction
        private void doProcess(int lineCount) {
            List<RangeMarker> markers = new ArrayList<>();
            try {
                myIndicator.setText2Value(LocalizeValue.localizeTODO("Converting end-of-line comments"));
                convertEndComments(lineCount);
                myIndicator.setText2Value(LocalizeValue.localizeTODO("Removing line-breaks"));
                int newCount = processRawJoiners(lineCount);
                DocumentUtil.executeInBulk(myDoc, newCount > 100, () -> removeLineBreaks(newCount, markers));
                myIndicator.setText2Value(LocalizeValue.localizeTODO("Postprocessing"));
                List<RangeMarker> unprocessed = processNonRawJoiners(markers);
                myIndicator.setText2Value(LocalizeValue.localizeTODO("Adjusting white-space"));
                adjustWhiteSpace(unprocessed);
            }
            finally {
                markers.forEach(RangeMarker::dispose);
            }
        }

        @RequiredWriteAction
        private void convertEndComments(int lineCount) {
            List<PsiComment> endComments = new ArrayList<>();
            CharSequence text = myDoc.getCharsSequence();
            for (int i = 0; i < lineCount; i++) {
                myIndicator.checkCanceled();
                myIndicator.setFraction(0.05 * i / lineCount);
                int line = myLine + i;
                int lineEnd = myDoc.getLineEndOffset(line);
                int lastNonSpaceOffset = StringUtil.skipWhitespaceBackward(text, lineEnd);
                if (lastNonSpaceOffset > myDoc.getLineStartOffset(line)) {
                    PsiComment comment = getCommentElement(myFile.findElementAt(lastNonSpaceOffset - 1));
                    if (comment != null) {
                        int nextStart = CharArrayUtil.shiftForward(text, myDoc.getLineStartOffset(line + 1), " \t\n");
                        if (nextStart < text.length() && myDoc.getLineNumber(nextStart) <= myLine + lineCount
                            && getCommentElement(myFile.findElementAt(nextStart)) == null) {
                            endComments.add(comment);
                        }
                    }
                }
            }
            boolean changed = false;
            for (int i = 0; i < endComments.size(); i++) {
                myIndicator.checkCanceled();
                myIndicator.setFraction(0.05 + 0.05 * i / endComments.size());
                PsiComment comment = endComments.get(i);
                changed |= tryConvertEndOfLineComment(comment);
            }
            if (changed) {
                myManager.doPostponedOperationsAndUnblockDocument(myDoc);
            }
        }

        /**
         * @param lineCount number of lines to process
         * @return number of unprocessed lines
         */
        private int processRawJoiners(int lineCount) {
            int startLine = myLine;
            List<JoinLinesHandlerDelegate> list = JoinLinesHandlerDelegate.EP_NAME.getExtensionList();
            int beforeLines = myDoc.getLineCount();
            CharSequence text = myDoc.getCharsSequence();
            int finalLine = myLine + lineCount;
            int finalOffset = myDoc.getLineEndOffset(myLine + lineCount);
            while (startLine < finalLine) {
                myIndicator.checkCanceled();
                myIndicator.setFraction(0.1 + 0.2 * (startLine - myLine) / Math.max(1, finalLine - myLine));

                int rc = CANNOT_JOIN;

                int lineEndOffset = myDoc.getLineEndOffset(startLine);
                int start = StringUtil.skipWhitespaceBackward(text, lineEndOffset);
                int end = CharArrayUtil.shiftForward(text, lineEndOffset, finalOffset, " \t\n");
                int linesToJoin = myDoc.getLineNumber(end) - startLine;
                JoinRawLinesHandlerDelegate rawJoiner = null;
                if (end < finalOffset && start > 0 && text.charAt(start - 1) != '\n') {
                    // Skip raw joiners if either of first or last lines is empty
                    for (JoinLinesHandlerDelegate delegate : list) {
                        if (delegate instanceof JoinRawLinesHandlerDelegate rj) {
                            rawJoiner = rj;
                            rc = rawJoiner.tryJoinRawLines(myDoc, myFile, start, end);
                            if (rc != CANNOT_JOIN) {
                                myCaretRestoreOffset = checkOffset(rc, delegate, myDoc);
                                break;
                            }
                        }
                    }
                }
                if (rc == CANNOT_JOIN) {
                    startLine += linesToJoin;
                }
                else {
                    myManager.doPostponedOperationsAndUnblockDocument(myDoc);
                    myManager.commitDocument(myDoc);
                    int afterLines = myDoc.getLineCount();
                    if (afterLines > beforeLines) {
                        LOG.error("Raw joiner increased number of lines: " + rawJoiner + " (" + rawJoiner.getClass() + ")");
                    }
                    if (afterLines >= beforeLines && myLine == startLine) {
                        // if number of lines is the same, continue processing from the next line
                        myLine++;
                        startLine++;
                    }
                    else {
                        // Single Join two lines procedure could join more than two (e.g. if it removes braces)
                        finalLine -= Math.max(beforeLines - afterLines, 1);
                    }
                    beforeLines = afterLines;
                    text = myDoc.getCharsSequence();
                    if (finalLine >= startLine) {
                        finalOffset = myDoc.getLineEndOffset(finalLine);
                    }
                }
            }
            return startLine - myLine;
        }

        private void removeLineBreaks(int lineCount, List<RangeMarker> markers) {
            for (int i = 0; i < lineCount; i++) {
                myIndicator.checkCanceled();
                myIndicator.setFraction(0.3 + 0.2 * i / lineCount);

                JoinLinesOffsets offsets = new JoinLinesOffsets(myDoc, myLine);

                if (offsets.lastNonSpaceOffsetInStartLine == myDoc.getLineStartOffset(myLine)) {
                    myDoc.deleteString(myDoc.getLineStartOffset(myLine), offsets.firstNonSpaceOffsetInNextLine);

                    myManager.commitDocument(myDoc);
                    int indent = myStyleManager.adjustLineIndent(myFile, myLine == 0 ? 0 : myDoc.getLineStartOffset(myLine));

                    if (myCaretRestoreOffset == CANNOT_JOIN) {
                        myCaretRestoreOffset = indent;
                    }
                    continue;
                }

                myDoc.deleteString(offsets.lineEndOffset, offsets.lineEndOffset + myDoc.getLineSeparatorLength(myLine));
                RangeMarker marker = myDoc.createRangeMarker(offsets.lineEndOffset, offsets.lineEndOffset);
                marker.setGreedyToLeft(true);
                marker.setGreedyToRight(true);
                markers.add(marker);
            }
            Collections.reverse(markers);
            myManager.commitDocument(myDoc);
        }

        private List<RangeMarker> processNonRawJoiners(List<RangeMarker> markers) {
            List<RangeMarker> unprocessed = new ArrayList<>();
            for (int i = 0; i < markers.size(); i++) {
                myIndicator.checkCanceled();
                myIndicator.setFraction(0.5 + 0.2 * i / markers.size());
                RangeMarker marker = markers.get(i);
                if (!marker.isValid()) {
                    continue;
                }
                Runnable doProcess = () -> {
                    if (!joinNonRaw(marker)) {
                        unprocessed.add(marker);
                    }
                };
                ProgressManager.getInstance().executeNonCancelableSection(doProcess);
            }
            return unprocessed;
        }

        private boolean joinNonRaw(RangeMarker marker) {
            CharSequence text = myDoc.getCharsSequence();
            int lineEndOffset = marker.getStartOffset();
            int start = StringUtil.skipWhitespaceBackward(text, lineEndOffset) - 1;
            int end = StringUtil.skipWhitespaceForward(text, lineEndOffset);
            int rc = CANNOT_JOIN;
            for (JoinLinesHandlerDelegate delegate : JoinLinesHandlerDelegate.EP_NAME.getExtensionList()) {
                rc = checkOffset(delegate.tryJoinLines(myDoc, myFile, start, end), delegate, myDoc);
                if (rc != CANNOT_JOIN) {
                    break;
                }
            }

            if (rc != CANNOT_JOIN) {
                RangeMarker posMarker = myDoc.createRangeMarker(rc, rc);
                myManager.doPostponedOperationsAndUnblockDocument(myDoc);
                if (myCaretRestoreOffset == CANNOT_JOIN && posMarker.isValid()) {
                    myCaretRestoreOffset = posMarker.getStartOffset();
                }
                return true;
            }
            return false;
        }

        private void adjustWhiteSpace(List<RangeMarker> markers) {
            int size = markers.size();
            if (size == 0) {
                return;
            }
            int[] spacesToAdd = getSpacesToAdd(markers);
            DocumentUtil.executeInBulk(myDoc, size > 100, () -> {
                for (int i = 0; i < size; i++) {
                    myIndicator.checkCanceled();
                    myIndicator.setFraction(0.95 + 0.05 * i / size);
                    RangeMarker marker = markers.get(i);
                    if (!marker.isValid()) {
                        continue;
                    }
                    CharSequence docText = myDoc.getCharsSequence();
                    int lineEndOffset = marker.getStartOffset();
                    int start = StringUtil.skipWhitespaceBackward(docText, lineEndOffset) - 1;
                    int end = StringUtil.skipWhitespaceForward(docText, lineEndOffset);
                    int replaceStart = start == lineEndOffset ? start : start + 1;
                    if (myCaretRestoreOffset == CANNOT_JOIN) {
                        myCaretRestoreOffset = replaceStart;
                    }
                    int spacesToCreate = spacesToAdd[i];
                    String spacing = StringUtil.repeatSymbol(' ', spacesToCreate);
                    myDoc.replaceString(replaceStart, end, spacing);
                }
            });
            myManager.commitDocument(myDoc);
        }

        @RequiredReadAction
        private int[] getSpacesToAdd(List<RangeMarker> markers) {
            int size = markers.size();
            int[] spacesToAdd = new int[size];
            Arrays.fill(spacesToAdd, -1);
            CharSequence text = myDoc.getCharsSequence();
            FormattingModelBuilder builder = FormattingModelBuilder.forContext(myFile);
            CodeStyleSettings settings = CodeStyle.getSettings(myFile);
            FormattingModel model = builder == null ? null : builder.createModel(FormattingContext.create(myFile, settings));
            FormatterEx formatter = FormatterEx.getInstanceEx();
            for (int i = 0; i < size; i++) {
                myIndicator.checkCanceled();
                myIndicator.setFraction(0.7 + 0.25 * i / size);
                RangeMarker marker = markers.get(i);
                if (!marker.isValid()) {
                    continue;
                }
                int end = StringUtil.skipWhitespaceForward(text, marker.getStartOffset());
                int spacesToCreate = end >= text.length() || text.charAt(end) == '\n'
                    ? 0
                    : model == null
                    ? 1
                    : formatter.getSpacingForBlockAtOffset(model, end);
                spacesToAdd[i] = spacesToCreate < 0 ? 1 : spacesToCreate;
            }
            return spacesToAdd;
        }

        private void positionCaret(Editor editor, Caret caret) {
            if (caret.hasSelection()) {
                caret.moveToOffset(caret.getSelectionEnd());
            }
            else if (myCaretRestoreOffset != CANNOT_JOIN) {
                caret.moveToOffset(myCaretRestoreOffset);
                if (caret == editor.getCaretModel().getPrimaryCaret()) { // performance
                    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                }
                caret.removeSelection();
            }
        }
    }

    private static int checkOffset(int offset, JoinLinesHandlerDelegate delegate, DocumentEx doc) {
        if (offset == CANNOT_JOIN) {
            return offset;
        }
        if (offset < 0) {
            LOG.error("Handler returned negative offset: handler class=" + delegate.getClass() + "; offset=" + offset);
            return 0;
        }
        else if (offset > doc.getTextLength()) {
            LOG.error("Handler returned an offset which exceeds the document length: handler class=" + delegate.getClass() + "; offset=" + offset + "; length=" + doc.getTextLength());
            return doc.getTextLength();
        }
        return offset;
    }

    private static class JoinLinesOffsets {
        final int lineEndOffset;
        final int lastNonSpaceOffsetInStartLine;
        final int firstNonSpaceOffsetInNextLine;

        JoinLinesOffsets(Document doc, int startLine) {
            CharSequence text = doc.getCharsSequence();
            this.lineEndOffset = doc.getLineEndOffset(startLine);
            this.firstNonSpaceOffsetInNextLine = StringUtil.skipWhitespaceForward(text, doc.getLineStartOffset(startLine + 1));
            this.lastNonSpaceOffsetInStartLine = StringUtil.skipWhitespaceBackward(text, this.lineEndOffset);
        }
    }

    @RequiredWriteAction
    private static boolean tryConvertEndOfLineComment(PsiElement commentElement) {
        if (Commenter.forLanguage(commentElement.getLanguage()) instanceof CodeDocumentationAwareCommenter docCommenter) {
            String lineCommentPrefix = docCommenter.getLineCommentPrefix();
            String blockCommentPrefix = docCommenter.getBlockCommentPrefix();
            String blockCommentSuffix = docCommenter.getBlockCommentSuffix();
            if (commentElement.getNode().getElementType() == docCommenter.getLineCommentTokenType()
                && blockCommentPrefix != null && blockCommentSuffix != null && lineCommentPrefix != null) {
                String commentText = StringUtil.trimStart(commentElement.getText(), lineCommentPrefix);
                String suffix = docCommenter.getBlockCommentSuffix();
                if (suffix != null && suffix.length() > 1) {
                    String fixedSuffix = suffix.charAt(0) + " " + suffix.substring(1);
                    commentText = commentText.replace(suffix, fixedSuffix);
                }
                try {
                    Project project = commentElement.getProject();
                    PsiParserFacade parserFacade = PsiParserFacade.SERVICE.getInstance(project);
                    PsiComment newComment = parserFacade.createBlockCommentFromText(commentElement.getLanguage(), commentText);
                    commentElement.replace(newComment);
                    return true;
                }
                catch (IncorrectOperationException e) {
                    LOG.info("Failed to replace line comment with block comment", e);
                }
            }
        }
        return false;
    }

    private static PsiComment getCommentElement(@Nullable PsiElement element) {
        return PsiTreeUtil.getParentOfType(element, PsiComment.class, false);
    }
}
