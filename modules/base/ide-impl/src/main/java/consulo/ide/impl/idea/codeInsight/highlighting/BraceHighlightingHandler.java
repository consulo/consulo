/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.*;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.internal.DocumentEx;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.codeInsight.hint.EditorFragmentComponent;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.ILazyParseableElementType;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.action.BraceMatchingUtil;
import consulo.language.editor.highlight.HighlighterIteratorWrapper;
import consulo.language.editor.highlight.LexerEditorHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.plain.PlainTextFileType;
import consulo.language.plain.psi.PsiPlainTextFile;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.util.ColorValueUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.function.Predicates;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author mike
 * @since 2002-09-27
 */
public class BraceHighlightingHandler {
    private static final Key<List<RangeHighlighter>> BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY =
        Key.create("BraceHighlighter.BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY");
    private static final Key<RangeHighlighter> LINE_MARKER_IN_EDITOR_KEY = Key.create("BraceHighlighter.LINE_MARKER_IN_EDITOR_KEY");
    private static final Key<LightweightHintImpl> HINT_IN_EDITOR_KEY = Key.create("BraceHighlighter.HINT_IN_EDITOR_KEY");

    /**
     * Holds weak references to the editors that are being processed at non-EDT.
     * <p/>
     * Is intended to be used to avoid submitting unnecessary new processing request from EDT, i.e. it's assumed that the collection
     * is accessed from the single thread (EDT).
     */
    private static final Set<Editor> PROCESSED_EDITORS = Collections.newSetFromMap(ContainerUtil.createWeakMap());

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final Editor myEditor;
    private final Alarm myAlarm;

    private final DocumentEx myDocument;
    private final PsiFile myPsiFile;
    private final CodeInsightSettings myCodeInsightSettings;

    private BraceHighlightingHandler(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Alarm alarm, PsiFile psiFile) {
        myProject = project;

        myEditor = editor;
        myAlarm = alarm;
        myDocument = (DocumentEx)myEditor.getDocument();

        myPsiFile = psiFile;
        myCodeInsightSettings = CodeInsightSettings.getInstance();
    }

    @RequiredUIAccess
    static void lookForInjectedAndMatchBracesInOtherThread(
        @Nonnull Editor editor,
        @Nonnull Alarm alarm,
        @Nonnull Predicate<BraceHighlightingHandler> processor
    ) {
        UIAccess.assertIsUIThread();
        if (!isValidEditor(editor)) {
            return;
        }
        if (!PROCESSED_EDITORS.add(editor)) {
            // Skip processing if that is not really necessary.
            // Assuming to be in EDT here.
            return;
        }
        int offset = editor.getCaretModel().getOffset();
        Project project = editor.getProject();
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (!isValidFile(psiFile)) {
            return;
        }
        Application app = Application.get();
        app.executeOnPooledThread(() -> {
            if (!app.tryRunReadAction(() -> {
                PsiFile injected;
                try {
                    if (psiFile instanceof PsiBinaryFile || !isValidEditor(editor) || !isValidFile(psiFile)) {
                        injected = null;
                    }
                    else {
                        injected = getInjectedFileIfAny(editor, project, offset, psiFile, alarm);
                    }
                }
                catch (RuntimeException e) {
                    // Reset processing flag in case of unexpected exception.
                    app.invokeLater(() -> PROCESSED_EDITORS.remove(editor));
                    throw e;
                }
                app.invokeLater(
                    () -> {
                        try {
                            if (isValidEditor(editor) && isValidFile(injected)) {
                                Editor newEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, injected);
                                BraceHighlightingHandler handler = new BraceHighlightingHandler(project, newEditor, alarm, injected);
                                processor.test(handler);
                            }
                        }
                        finally {
                            PROCESSED_EDITORS.remove(editor);
                        }
                    },
                    app.getModalityStateForComponent(editor.getComponent())
                );
            })) {
                // write action is queued in AWT. restart after it's finished
                app.invokeLater(
                    () -> {
                        PROCESSED_EDITORS.remove(editor);
                        lookForInjectedAndMatchBracesInOtherThread(editor, alarm, processor);
                    },
                    app.getModalityStateForComponent(editor.getComponent())
                );
            }
        });
    }

    private static boolean isValidFile(PsiFile file) {
        return file != null && file.isValid() && !file.getProject().isDisposed();
    }

    public static boolean isValidEditor(@Nonnull Editor editor) {
        Project editorProject = editor.getProject();
        return editorProject != null && !editorProject.isDisposed() && !editor.isDisposed() && editor.isShowing() && !editor.isViewer();
    }

    @Nonnull
    private static PsiFile getInjectedFileIfAny(
        @Nonnull Editor editor,
        @Nonnull Project project,
        int offset,
        @Nonnull PsiFile psiFile,
        @Nonnull Alarm alarm
    ) {
        Document document = editor.getDocument();
        // when document is committed, try to highlight braces in injected lang - it's fast
        if (PsiDocumentManager.getInstance(project).isCommitted(document)) {
            PsiElement injectedElement =
                InjectedLanguageManager.getInstance(psiFile.getProject()).findInjectedElementAt(psiFile, offset);
            if (injectedElement != null /*&& !(injectedElement instanceof PsiWhiteSpace)*/) {
                PsiFile injected = injectedElement.getContainingFile();
                if (injected != null) {
                    return injected;
                }
            }
        }
        else {
            PsiDocumentManager.getInstance(project).performForCommittedDocument(document, () -> {
                if (!project.isDisposed() && !editor.isDisposed()) {
                    BackgroundHighlighter.updateBraces(editor, alarm);
                }
            });
        }
        return psiFile;
    }

    @Nonnull
    @RequiredReadAction
    static EditorHighlighter getLazyParsableHighlighterIfAny(Project project, Editor editor, PsiFile psiFile) {
        if (!PsiDocumentManager.getInstance(project).isCommitted(editor.getDocument())) {
            return editor.getHighlighter();
        }
        PsiElement elementAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        for (PsiElement e : SyntaxTraverser.psiApi().parents(elementAt).takeWhile(Predicates.notEqualTo(psiFile))) {
            if (!(PsiUtilCore.getElementType(e) instanceof ILazyParseableElementType)) {
                continue;
            }
            Language language = ILazyParseableElementType.LANGUAGE_KEY.get(e.getNode());
            if (language == null) {
                continue;
            }
            TextRange range = e.getTextRange();
            int offset = range.getStartOffset();
            SyntaxHighlighter syntaxHighlighter =
                SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, psiFile.getVirtualFile());
            LexerEditorHighlighter highlighter = new LexerEditorHighlighter(syntaxHighlighter, editor.getColorsScheme()) {
                @Nonnull
                @Override
                public HighlighterIterator createIterator(int startOffset) {
                    return new HighlighterIteratorWrapper(super.createIterator(Math.max(startOffset - offset, 0))) {
                        @Override
                        public int getStart() {
                            return super.getStart() + offset;
                        }

                        @Override
                        public int getEnd() {
                            return super.getEnd() + offset;
                        }
                    };
                }
            };
            highlighter.setText(editor.getDocument().getText(range));
            return highlighter;
        }
        return editor.getHighlighter();
    }

    @RequiredUIAccess
    void updateBraces() {
        UIAccess.assertIsUIThread();

        if (myPsiFile == null || !myPsiFile.isValid()) {
            return;
        }

        clearBraceHighlighters();

        if (!myCodeInsightSettings.HIGHLIGHT_BRACES) {
            return;
        }

        if (myEditor.getSelectionModel().hasSelection()) {
            return;
        }

        if (myEditor.getSoftWrapModel().isInsideOrBeforeSoftWrap(myEditor.getCaretModel().getVisualPosition())) {
            return;
        }

        int offset = myEditor.getCaretModel().getOffset();
        CharSequence chars = myEditor.getDocument().getCharsSequence();

        //if (myEditor.offsetToLogicalPosition(offset).column != myEditor.getCaretModel().getLogicalPosition().column) {
        //    // we are in virtual space
        //    int caretLineNumber = myEditor.getCaretModel().getLogicalPosition().line;
        //    if (caretLineNumber >= myDocument.getLineCount()) return;
        //    offset = myDocument.getLineEndOffset(caretLineNumber) + myDocument.getLineSeparatorLength(caretLineNumber);
        //}

        int originalOffset = offset;

        EditorHighlighter highlighter = getEditorHighlighter();
        HighlighterIterator iterator = highlighter.createIterator(offset);
        FileType fileType = PsiUtilBase.getPsiFileAtOffset(myPsiFile, offset).getFileType();

        if (iterator.atEnd()) {
            offset--;
        }
        else if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
            offset--;
        }
        else if (!BraceMatchingUtil.isLBraceToken(iterator, chars, fileType)) {
            offset--;

            if (offset >= 0) {
                HighlighterIterator it = highlighter.createIterator(offset);
                if (!BraceMatchingUtil.isRBraceToken(it, chars, getFileTypeByIterator(it))) {
                    offset++;
                }
            }
        }

        if (offset < 0) {
            removeLineMarkers();
            return;
        }

        iterator = highlighter.createIterator(offset);
        fileType = getFileTypeByIterator(iterator);

        myAlarm.cancelAllRequests();

        if (BraceMatchingUtil.isLBraceToken(iterator, chars, fileType) ||
            BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
            doHighlight(offset, originalOffset, fileType);
        }
        else if (offset > 0 && offset < chars.length()) {
            // There is a possible case that there are paired braces nearby the caret position and the document contains only white
            // space symbols between them. We want to highlight such braces as well.
            // Example:
            //     public void test() { <caret>
            //     }
            char c = chars.charAt(offset);
            boolean searchForward = c != '\n';

            // Try to find matched brace backwards.
            if (offset >= originalOffset && (c == ' ' || c == '\t' || c == '\n')) {
                int backwardNonWsOffset = CharArrayUtil.shiftBackward(chars, offset - 1, "\t ");
                if (backwardNonWsOffset >= 0) {
                    iterator = highlighter.createIterator(backwardNonWsOffset);
                    FileType newFileType = getFileTypeByIterator(iterator);
                    if (BraceMatchingUtil.isLBraceToken(iterator, chars, newFileType) ||
                        BraceMatchingUtil.isRBraceToken(iterator, chars, newFileType)) {
                        offset = backwardNonWsOffset;
                        searchForward = false;
                        doHighlight(backwardNonWsOffset, originalOffset, newFileType);
                    }
                }
            }

            // Try to find matched brace forward.
            if (searchForward) {
                int forwardOffset = CharArrayUtil.shiftForward(chars, offset, "\t ");
                if (forwardOffset > offset || c == ' ' || c == '\t') {
                    iterator = highlighter.createIterator(forwardOffset);
                    FileType newFileType = getFileTypeByIterator(iterator);
                    if (BraceMatchingUtil.isLBraceToken(iterator, chars, newFileType) ||
                        BraceMatchingUtil.isRBraceToken(iterator, chars, newFileType)) {
                        offset = forwardOffset;
                        doHighlight(forwardOffset, originalOffset, newFileType);
                    }
                }
            }
        }

        //highlight scope
        if (!myCodeInsightSettings.HIGHLIGHT_SCOPE) {
            removeLineMarkers();
            return;
        }

        int _offset = offset;
        FileType _fileType = fileType;
        myAlarm.addRequest(
            () -> {
                if (!myProject.isDisposed() && !myEditor.isDisposed()) {
                    highlightScope(_offset, _fileType);
                }
            },
            300
        );
    }

    @Nonnull
    private FileType getFileTypeByIterator(@Nonnull HighlighterIterator iterator) {
        return PsiUtilBase.getPsiFileAtOffset(myPsiFile, iterator.getStart()).getFileType();
    }

    @Nonnull
    private FileType getFileTypeByOffset(int offset) {
        return PsiUtilBase.getPsiFileAtOffset(myPsiFile, offset).getFileType();
    }

    @Nonnull
    @RequiredReadAction
    private EditorHighlighter getEditorHighlighter() {
        return getLazyParsableHighlighterIfAny(myProject, myEditor, myPsiFile);
    }

    @RequiredUIAccess
    private void highlightScope(int offset, @Nonnull FileType fileType) {
        if (myEditor.getFoldingModel().isOffsetCollapsed(offset)) {
            return;
        }
        if (myEditor.getDocument().getTextLength() <= offset) {
            return;
        }

        HighlighterIterator iterator = getEditorHighlighter().createIterator(offset);
        CharSequence chars = myDocument.getCharsSequence();

        if (!BraceMatchingUtil.isStructuralBraceToken(fileType, iterator, chars)) {
//      if (BraceMatchingUtil.isRBraceTokenToHighlight(myFileType, iterator) || BraceMatchingUtil.isLBraceTokenToHighlight(myFileType, iterator)) return;
        }
        else {
            if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType) ||
                BraceMatchingUtil.isLBraceToken(iterator, chars, fileType)) {
                return;
            }
        }

        if (!BraceMatchingUtil.findStructuralLeftBrace(fileType, iterator, chars)) {
            removeLineMarkers();
            return;
        }

        highlightLeftBrace(iterator, true, fileType);
    }

    @RequiredUIAccess
    private void doHighlight(int offset, int originalOffset, @Nonnull FileType fileType) {
        if (myEditor.getFoldingModel().isOffsetCollapsed(offset)) {
            return;
        }

        HighlighterIterator iterator = getEditorHighlighter().createIterator(offset);
        CharSequence chars = myDocument.getCharsSequence();

        if (BraceMatchingUtil.isLBraceToken(iterator, chars, fileType)) {
            IElementType tokenType = (IElementType)iterator.getTokenType();

            iterator.advance();
            if (!iterator.atEnd() && BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
                if (BraceMatchingUtil.isPairBraces(tokenType, (IElementType)iterator.getTokenType(), fileType) &&
                    originalOffset == iterator.getStart()) {
                    return;
                }
            }

            iterator.retreat();
            highlightLeftBrace(iterator, false, fileType);

            if (offset > 0) {
                iterator = getEditorHighlighter().createIterator(offset - 1);
                if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
                    highlightRightBrace(iterator, fileType);
                }
            }
        }
        else if (BraceMatchingUtil.isRBraceToken(iterator, chars, fileType)) {
            highlightRightBrace(iterator, fileType);
        }
    }

    @RequiredUIAccess
    private void highlightRightBrace(@Nonnull HighlighterIterator iterator, @Nonnull FileType fileType) {
        TextRange brace1 = TextRange.create(iterator.getStart(), iterator.getEnd());

        boolean matched = BraceMatchingUtil.matchBrace(myDocument.getCharsSequence(), fileType, iterator, false);

        TextRange brace2 = iterator.atEnd() ? null : TextRange.create(iterator.getStart(), iterator.getEnd());

        highlightBraces(brace2, brace1, matched, false, fileType);
    }

    @RequiredUIAccess
    private void highlightLeftBrace(@Nonnull HighlighterIterator iterator, boolean scopeHighlighting, @Nonnull FileType fileType) {
        TextRange brace1Start = TextRange.create(iterator.getStart(), iterator.getEnd());
        boolean matched = BraceMatchingUtil.matchBrace(myDocument.getCharsSequence(), fileType, iterator, true);

        TextRange brace2End = iterator.atEnd() ? null : TextRange.create(iterator.getStart(), iterator.getEnd());

        highlightBraces(brace1Start, brace2End, matched, scopeHighlighting, fileType);
    }

    @RequiredUIAccess
    private void highlightBraces(
        @Nullable TextRange lBrace,
        @Nullable TextRange rBrace,
        boolean matched,
        boolean scopeHighlighting,
        @Nonnull FileType fileType
    ) {
        if (!matched && fileType == PlainTextFileType.INSTANCE) {
            return;
        }

        EditorColorsScheme scheme = myEditor.getColorsScheme();
        TextAttributes attributes = matched
            ? scheme.getAttributes(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES)
            : scheme.getAttributes(CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES);

        if (rBrace != null && !scopeHighlighting) {
            highlightBrace(rBrace, matched);
        }

        if (lBrace != null && !scopeHighlighting) {
            highlightBrace(lBrace, matched);
        }

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject); // null in default project
        if (fileEditorManager == null || !myEditor.equals(fileEditorManager.getSelectedTextEditor())) {
            return;
        }

        if (lBrace != null && rBrace != null) {
            int startLine = myEditor.offsetToLogicalPosition(lBrace.getStartOffset()).line;
            int endLine = myEditor.offsetToLogicalPosition(rBrace.getEndOffset()).line;
            if (endLine - startLine > 0) {
                Runnable runnable = () -> {
                    if (myProject.isDisposed() || myEditor.isDisposed()) {
                        return;
                    }
                    ColorValue color = attributes.getBackgroundColor();
                    if (color == null) {
                        return;
                    }
                    color = ColorValueUtil.isDark(EditorColorsManager.getInstance()
                        .getGlobalScheme()
                        .getDefaultBackground()) ? ColorValueUtil.shift(color, 1.5d) : ColorValueUtil.darker(color);
                    lineMarkFragment(startLine, endLine, color);
                };

                if (!scopeHighlighting) {
                    myAlarm.addRequest(runnable, 300);
                }
                else {
                    runnable.run();
                }
            }
            else {
                removeLineMarkers();
            }

            if (!scopeHighlighting) {
                showScopeHint(lBrace.getStartOffset(), lBrace.getEndOffset());
            }
        }
        else {
            if (!myCodeInsightSettings.HIGHLIGHT_SCOPE) {
                removeLineMarkers();
            }
        }
    }

    private void highlightBrace(@Nonnull TextRange braceRange, boolean matched) {
        EditorColorsScheme scheme = myEditor.getColorsScheme();
        TextAttributes attributes = matched
            ? scheme.getAttributes(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES)
            : scheme.getAttributes(CodeInsightColors.UNMATCHED_BRACE_ATTRIBUTES);

        RangeHighlighter rbraceHighlighter = myEditor.getMarkupModel().addRangeHighlighter(
            braceRange.getStartOffset(),
            braceRange.getEndOffset(),
            HighlighterLayer.LAST + 1,
            attributes,
            HighlighterTargetArea.EXACT_RANGE
        );
        rbraceHighlighter.setGreedyToLeft(false);
        rbraceHighlighter.setGreedyToRight(false);
        registerHighlighter(rbraceHighlighter);
    }

    private void registerHighlighter(@Nonnull RangeHighlighter highlighter) {
        getHighlightersList().add(highlighter);
    }

    @Nonnull
    private List<RangeHighlighter> getHighlightersList() {
        // braces are highlighted across the whole editor, not in each injected editor separately
        Editor editor = myEditor instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : myEditor;
        List<RangeHighlighter> highlighters = editor.getUserData(BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY);
        if (highlighters == null) {
            highlighters = new ArrayList<>();
            editor.putUserData(BRACE_HIGHLIGHTERS_IN_EDITOR_VIEW_KEY, highlighters);
        }
        return highlighters;
    }

    private void showScopeHint(int lbraceStart, int lbraceEnd) {
        LogicalPosition bracePosition = myEditor.offsetToLogicalPosition(lbraceStart);
        Point braceLocation = myEditor.logicalPositionToXY(bracePosition);
        int y = braceLocation.y;
        myAlarm.addRequest(
            () -> {
                if (myProject.isDisposed()) {
                    return;
                }
                PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(() -> {
                    if (!myEditor.getComponent().isShowing()) {
                        return;
                    }
                    Rectangle viewRect = myEditor.getScrollingModel().getVisibleArea();
                    if (y < viewRect.y) {
                        int start = lbraceStart;
                        if (!(myPsiFile instanceof PsiPlainTextFile) && myPsiFile.isValid()) {
                            start = BraceMatchingUtil.getBraceMatcher(
                                getFileTypeByOffset(lbraceStart),
                                PsiUtilCore.getLanguageAtOffset(myPsiFile, lbraceStart)
                            ).getCodeConstructStart(myPsiFile, lbraceStart);
                        }
                        TextRange range = new TextRange(start, lbraceEnd);
                        int line1 = myDocument.getLineNumber(range.getStartOffset());
                        int line2 = myDocument.getLineNumber(range.getEndOffset());
                        line1 = Math.max(line1, line2 - 5);
                        range = new TextRange(myDocument.getLineStartOffset(line1), range.getEndOffset());
                        LightweightHintImpl hint = EditorFragmentComponent.showEditorFragmentHint(myEditor, range, true, true);
                        myEditor.putUserData(HINT_IN_EDITOR_KEY, hint);
                    }
                });
            },
            300,
            IdeaModalityState.stateForComponent(myEditor.getComponent())
        );
    }

    void clearBraceHighlighters() {
        List<RangeHighlighter> highlighters = getHighlightersList();
        for (RangeHighlighter highlighter : highlighters) {
            highlighter.dispose();
        }
        highlighters.clear();

        LightweightHintImpl hint = myEditor.getUserData(HINT_IN_EDITOR_KEY);
        if (hint != null) {
            hint.hide();
            myEditor.putUserData(HINT_IN_EDITOR_KEY, null);
        }
    }

    @RequiredUIAccess
    private void lineMarkFragment(int startLine, int endLine, @Nonnull ColorValue color) {
        removeLineMarkers();

        if (startLine >= endLine || endLine >= myDocument.getLineCount()) {
            return;
        }

        int startOffset = myDocument.getLineStartOffset(startLine);
        int endOffset = myDocument.getLineStartOffset(endLine);

        RangeHighlighter highlighter =
            myEditor.getMarkupModel().addRangeHighlighter(startOffset, endOffset, 0, null, HighlighterTargetArea.LINES_IN_RANGE);
        highlighter.setLineMarkerRenderer(new DefaultLineMarkerRenderer(color));
        myEditor.putUserData(LINE_MARKER_IN_EDITOR_KEY, highlighter);
    }

    @RequiredUIAccess
    private void removeLineMarkers() {
        UIAccess.assertIsUIThread();
        RangeHighlighter marker = myEditor.getUserData(LINE_MARKER_IN_EDITOR_KEY);
        if (marker != null && ((MarkupModelEx)myEditor.getMarkupModel()).containsHighlighter(marker)) {
            marker.dispose();
        }
        myEditor.putUserData(LINE_MARKER_IN_EDITOR_KEY, null);
    }
}
