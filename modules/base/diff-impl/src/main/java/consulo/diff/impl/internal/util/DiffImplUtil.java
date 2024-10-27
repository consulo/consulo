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
package consulo.diff.impl.internal.util;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.*;
import consulo.colorScheme.EditorColorsManager;
import consulo.component.persist.StoragePathMacros;
import consulo.dataContext.DataProvider;
import consulo.diff.DiffContext;
import consulo.diff.DiffTool;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.SuppressiveDiffTool;
import consulo.diff.comparison.ByWord;
import consulo.diff.comparison.ComparisonManager;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.FileContent;
import consulo.diff.fragment.DiffFragment;
import consulo.diff.fragment.LineFragment;
import consulo.diff.fragment.MergeLineFragment;
import consulo.diff.impl.internal.DiffSettingsHolder;
import consulo.diff.impl.internal.merge.MergeInnerDifferences;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.diff.util.*;
import consulo.document.Document;
import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.highlight.EmptyEditorHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.JBValue;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.image.Image;
import consulo.undoRedo.*;
import consulo.undoRedo.builder.CommandBuilder;
import consulo.undoRedo.builder.ProxyCommandBuilder;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.BiPredicate;

public class DiffImplUtil {
    private static final Logger LOG = Logger.getInstance(DiffImplUtil.class);

    @Nonnull
    public static final String DIFF_CONFIG = StoragePathMacros.APP_CONFIG + "/diff.xml";
    public static final JBValue TITLE_GAP = new JBValue.Float(2);

    //
    // Editor
    //

    public static boolean isDiffEditor(@Nonnull Editor editor) {
        return editor.getEditorKind() == EditorKind.DIFF;
    }

    @Nullable
    public static EditorHighlighter initEditorHighlighter(
        @Nullable Project project,
        @Nonnull DocumentContent content,
        @Nonnull CharSequence text
    ) {
        EditorHighlighter highlighter = createEditorHighlighter(project, content);
        if (highlighter == null) {
            return null;
        }
        highlighter.setText(text);
        return highlighter;
    }

    @Nonnull
    public static EditorHighlighter initEmptyEditorHighlighter(@Nonnull CharSequence text) {
        EditorHighlighter highlighter = createEmptyEditorHighlighter();
        highlighter.setText(text);
        return highlighter;
    }

    @Nullable
    private static EditorHighlighter createEditorHighlighter(@Nullable Project project, @Nonnull DocumentContent content) {
        FileType type = content.getContentType();
        VirtualFile file = content.getHighlightFile();
        Language language = content.getUserData(Language.KEY);

        EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
        if (language != null) {
            SyntaxHighlighter syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file);
            return highlighterFactory.createEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().getGlobalScheme());
        }
        if (file != null) {
            if ((type == null || type == PlainTextFileType.INSTANCE) || file.getFileType() == type || file instanceof LightVirtualFile) {
                return highlighterFactory.createEditorHighlighter(project, file);
            }
        }
        if (type != null) {
            return highlighterFactory.createEditorHighlighter(project, type);
        }
        return null;
    }

    @Nonnull
    private static EditorHighlighter createEmptyEditorHighlighter() {
        return new EmptyEditorHighlighter(EditorColorsManager.getInstance().getGlobalScheme().getAttributes(HighlighterColors.TEXT));
    }

    public static void setEditorHighlighter(@Nullable Project project, @Nonnull EditorEx editor, @Nonnull DocumentContent content) {
        EditorHighlighter highlighter = createEditorHighlighter(project, content);
        if (highlighter != null) {
            editor.setHighlighter(highlighter);
        }
    }

    public static void setEditorCodeStyle(@Nullable Project project, @Nonnull EditorEx editor, @Nullable FileType fileType) {
        if (project != null && fileType != null) {
            editor.getSettings().setTabSize(CodeStyle.getProjectOrDefaultSettings(project).getTabSize(fileType));
            editor.getSettings().setUseTabCharacter(CodeStyle.getProjectOrDefaultSettings(project).useTabCharacter(fileType));
        }
        editor.getSettings().setCaretRowShown(false);
        editor.reinitSettings();
    }

    public static void setFoldingModelSupport(@Nonnull EditorEx editor) {
        editor.getSettings().setFoldingOutlineShown(true);
        editor.getSettings().setAutoCodeFoldingEnabled(false);
        editor.getColorsScheme().setAttributes(EditorColors.FOLDED_TEXT_ATTRIBUTES, null);
    }

    @Nonnull
    public static EditorEx createEditor(@Nonnull Document document, @Nullable Project project, boolean isViewer) {
        return createEditor(document, project, isViewer, false);
    }

    @Nonnull
    public static EditorEx createEditor(@Nonnull Document document, @Nullable Project project, boolean isViewer, boolean enableFolding) {
        EditorFactory factory = EditorFactory.getInstance();
        EditorEx editor = (EditorEx)(isViewer
            ? factory.createViewer(document, project, EditorKind.DIFF)
            : factory.createEditor(document, project, EditorKind.DIFF));

        editor.getSettings().setShowIntentionBulb(false);
        editor.getMarkupModel().setErrorStripeVisible(true);
        editor.getGutterComponentEx().setShowDefaultGutterPopup(false);

        if (enableFolding) {
            setFoldingModelSupport(editor);
        }
        else {
            editor.getSettings().setFoldingOutlineShown(false);
            editor.getFoldingModel().setFoldingEnabled(false);
        }

        UIUtil.removeScrollBorder(editor.getComponent());

        return editor;
    }

    public static void configureEditor(@Nonnull EditorEx editor, @Nonnull DocumentContent content, @Nullable Project project) {
        setEditorHighlighter(project, editor, content);
        setEditorCodeStyle(project, editor, content.getContentType());
        editor.reinitSettings();
    }

    public static boolean isMirrored(@Nonnull Editor editor) {
        return editor instanceof EditorEx editorEx && editorEx.getVerticalScrollbarOrientation() == EditorEx.VERTICAL_SCROLLBAR_LEFT;
    }

    //
    // Scrolling
    //

    public static void moveCaret(@Nullable final Editor editor, int line) {
        if (editor == null) {
            return;
        }
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line, 0));
    }

    public static void scrollEditor(@Nullable final Editor editor, int line, boolean animated) {
        scrollEditor(editor, line, 0, animated);
    }

    public static void scrollEditor(@Nullable final Editor editor, int line, int column, boolean animated) {
        if (editor == null) {
            return;
        }
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(line, column));
        scrollToCaret(editor, animated);
    }

    public static void scrollToCaret(@Nullable Editor editor, boolean animated) {
        if (editor == null) {
            return;
        }
        if (!animated) {
            editor.getScrollingModel().disableAnimation();
        }
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        if (!animated) {
            editor.getScrollingModel().enableAnimation();
        }
    }

    @Nonnull
    public static LogicalPosition getCaretPosition(@Nullable Editor editor) {
        return editor != null ? editor.getCaretModel().getLogicalPosition() : new LogicalPosition(0, 0);
    }

    //
    // Icons
    //

    @Nonnull
    public static Image getArrowIcon(@Nonnull Side sourceSide) {
        return sourceSide.select(AllIcons.Diff.ArrowRight, AllIcons.Diff.Arrow);
    }

    @Nonnull
    public static Image getArrowDownIcon(@Nonnull Side sourceSide) {
        return sourceSide.select(AllIcons.Diff.ArrowRightDown, AllIcons.Diff.ArrowLeftDown);
    }

    //
    // UI
    //

    public static void addActionBlock(@Nonnull DefaultActionGroup group, AnAction... actions) {
        addActionBlock(group, Arrays.asList(actions));
    }

    public static void addActionBlock(@Nonnull DefaultActionGroup group, @Nullable List<? extends AnAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        group.addSeparator();

        AnAction[] children = group.getChildren(null);
        for (AnAction action : actions) {
            if (!ArrayUtil.contains(action, children)) {
                group.add(action);
            }
        }
    }

    @Nonnull
    public static String getSettingsConfigurablePath() {
        return "Settings | Tools | Diff";
    }

    @Nonnull
    public static String createTooltipText(@Nonnull String text, @Nullable String appendix) {
        StringBuilder result = new StringBuilder();
        result.append("<html><body>");
        result.append(text);
        if (appendix != null) {
            result.append("<br><div style='margin-top: 5px'><font size='2'>");
            result.append(appendix);
            result.append("</font></div>");
        }
        result.append("</body></html>");
        return result.toString();
    }

    @Nonnull
    public static String createNotificationText(@Nonnull String text, @Nullable String appendix) {
        StringBuilder result = new StringBuilder();
        result.append("<html><body>");
        result.append(text);
        if (appendix != null) {
            result.append("<br><span style='color:#").append(ColorUtil.toHex(JBColor.gray)).append("'><small>");
            result.append(appendix);
            result.append("</small></span>");
        }
        result.append("</body></html>");
        return result.toString();
    }

    @Nonnull
    public static List<LineFragment> compare(
        @Nonnull DiffRequest request,
        @Nonnull CharSequence text1,
        @Nonnull CharSequence text2,
        @Nonnull DiffConfig config,
        @Nonnull ProgressIndicator indicator
    ) {
        indicator.checkCanceled();

        DiffUserDataKeysEx.DiffComputer diffComputer = request.getUserData(DiffUserDataKeysEx.CUSTOM_DIFF_COMPUTER);

        List<LineFragment> fragments;
        if (diffComputer != null) {
            fragments = diffComputer.compute(text1, text2, config.policy, config.innerFragments, indicator);
        }
        else {
            if (config.innerFragments) {
                fragments = ComparisonManager.getInstance().compareLinesInner(text1, text2, config.policy, indicator);
            }
            else {
                fragments = ComparisonManager.getInstance().compareLines(text1, text2, config.policy, indicator);
            }
        }

        indicator.checkCanceled();
        return ComparisonManager.getInstance()
            .processBlocks(fragments, text1, text2, config.policy, config.squashFragments, config.trimFragments);
    }

    @Nullable
    public static MergeInnerDifferences compareThreesideInner(
        @Nonnull List<CharSequence> chunks,
        @Nonnull ComparisonPolicy comparisonPolicy,
        @Nonnull ProgressIndicator indicator
    ) {
        if (chunks.get(0) == null && chunks.get(1) == null && chunks.get(2) == null) {
            return null; // ---
        }

        if (comparisonPolicy == ComparisonPolicy.IGNORE_WHITESPACES) {
            if (isChunksEquals(chunks.get(0), chunks.get(1), comparisonPolicy) &&
                isChunksEquals(chunks.get(0), chunks.get(2), comparisonPolicy)) {
                // whitespace-only changes, ex: empty lines added/removed
                return new MergeInnerDifferences(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }
        }

        if (chunks.get(0) == null && chunks.get(1) == null ||
            chunks.get(0) == null && chunks.get(2) == null ||
            chunks.get(1) == null && chunks.get(2) == null) { // =--, -=-, --=
            return null;
        }

        if (chunks.get(0) != null && chunks.get(1) != null && chunks.get(2) != null) { // ===
            List<DiffFragment> fragments1 = ByWord.compare(chunks.get(1), chunks.get(0), comparisonPolicy, indicator);
            List<DiffFragment> fragments2 = ByWord.compare(chunks.get(1), chunks.get(2), comparisonPolicy, indicator);

            List<TextRange> left = new ArrayList<>();
            List<TextRange> base = new ArrayList<>();
            List<TextRange> right = new ArrayList<>();

            for (DiffFragment wordFragment : fragments1) {
                base.add(new TextRange(wordFragment.getStartOffset1(), wordFragment.getEndOffset1()));
                left.add(new TextRange(wordFragment.getStartOffset2(), wordFragment.getEndOffset2()));
            }

            for (DiffFragment wordFragment : fragments2) {
                base.add(new TextRange(wordFragment.getStartOffset1(), wordFragment.getEndOffset1()));
                right.add(new TextRange(wordFragment.getStartOffset2(), wordFragment.getEndOffset2()));
            }

            return new MergeInnerDifferences(left, base, right);
        }

        // ==-, =-=, -==
        final ThreeSide side1 = chunks.get(0) != null ? ThreeSide.LEFT : ThreeSide.BASE;
        final ThreeSide side2 = chunks.get(2) != null ? ThreeSide.RIGHT : ThreeSide.BASE;
        CharSequence chunk1 = side1.select(chunks);
        CharSequence chunk2 = side2.select(chunks);

        List<DiffFragment> wordConflicts = ByWord.compare(chunk1, chunk2, comparisonPolicy, indicator);

        List<List<TextRange>> textRanges = ThreeSide.map(side -> {
            if (side == side1) {
                return ContainerUtil.map(wordConflicts, fragment -> new TextRange(fragment.getStartOffset1(), fragment.getEndOffset1()));
            }
            if (side == side2) {
                return ContainerUtil.map(wordConflicts, fragment -> new TextRange(fragment.getStartOffset2(), fragment.getEndOffset2()));
            }
            return null;
        });

        return new MergeInnerDifferences(textRanges.get(0), textRanges.get(1), textRanges.get(2));
    }

    private static boolean isChunksEquals(
        @Nullable CharSequence chunk1,
        @Nullable CharSequence chunk2,
        @Nonnull ComparisonPolicy comparisonPolicy
    ) {
        if (chunk1 == null) {
            chunk1 = "";
        }
        if (chunk2 == null) {
            chunk2 = "";
        }
        return ComparisonManager.getInstance().isEquals(chunk1, chunk2, comparisonPolicy);
    }

    @Nonnull
    public static <T> int[] getSortedIndexes(@Nonnull List<T> values, @Nonnull Comparator<T> comparator) {
        final List<Integer> indexes = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            indexes.add(i);
        }

        ContainerUtil.sort(indexes, (i1, i2) -> {
            T val1 = values.get(indexes.get(i1));
            T val2 = values.get(indexes.get(i2));
            return comparator.compare(val1, val2);
        });

        return ArrayUtil.toIntArray(indexes);
    }

    @Nonnull
    public static int[] invertIndexes(@Nonnull int[] indexes) {
        int[] inverted = new int[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            inverted[indexes[i]] = i;
        }
        return inverted;
    }

    //
    // Document modification
    //

    @Nonnull
    public static BitSet getSelectedLines(@Nonnull Editor editor) {
        Document document = editor.getDocument();
        int totalLines = getLineCount(document);
        BitSet lines = new BitSet(totalLines + 1);

        for (Caret caret : editor.getCaretModel().getAllCarets()) {
            if (caret.hasSelection()) {
                int line1 = editor.offsetToLogicalPosition(caret.getSelectionStart()).line;
                int line2 = editor.offsetToLogicalPosition(caret.getSelectionEnd()).line;
                lines.set(line1, line2 + 1);
                if (caret.getSelectionEnd() == document.getTextLength()) {
                    lines.set(totalLines);
                }
            }
            else {
                lines.set(caret.getLogicalPosition().line);
                if (caret.getOffset() == document.getTextLength()) {
                    lines.set(totalLines);
                }
            }
        }

        return lines;
    }

    public static boolean isSelectedByLine(int line, int line1, int line2) {
        return line1 == line2 && line == line1
            || line >= line1 && line < line2;
    }

    public static boolean isSelectedByLine(@Nonnull BitSet selected, int line1, int line2) {
        if (line1 == line2) {
            return selected.get(line1);
        }
        else {
            int next = selected.nextSetBit(line1);
            return next != -1 && next < line2;
        }
    }

    private static void deleteLines(@Nonnull Document document, int line1, int line2) {
        TextRange range = getLinesRange(document, line1, line2);
        int offset1 = range.getStartOffset();
        int offset2 = range.getEndOffset();

        if (offset1 > 0) {
            offset1--;
        }
        else if (offset2 < document.getTextLength()) {
            offset2++;
        }
        document.deleteString(offset1, offset2);
    }

    private static void insertLines(@Nonnull Document document, int line, @Nonnull CharSequence text) {
        if (line == getLineCount(document)) {
            document.insertString(document.getTextLength(), "\n" + text);
        }
        else {
            document.insertString(document.getLineStartOffset(line), text + "\n");
        }
    }

    private static void replaceLines(@Nonnull Document document, int line1, int line2, @Nonnull CharSequence text) {
        TextRange currentTextRange = getLinesRange(document, line1, line2);
        int offset1 = currentTextRange.getStartOffset();
        int offset2 = currentTextRange.getEndOffset();

        document.replaceString(offset1, offset2, text);
    }

    public static void applyModification(
        @Nonnull Document document,
        int line1,
        int line2,
        @Nonnull List<? extends CharSequence> newLines
    ) {
        if (line1 == line2 && newLines.isEmpty()) {
            return;
        }
        if (line1 == line2) {
            insertLines(document, line1, StringUtil.join(newLines, "\n"));
        }
        else if (newLines.isEmpty()) {
            deleteLines(document, line1, line2);
        }
        else {
            replaceLines(document, line1, line2, StringUtil.join(newLines, "\n"));
        }
    }

    public static void applyModification(
        @Nonnull Document document1,
        int line1,
        int line2,
        @Nonnull Document document2,
        int oLine1,
        int oLine2
    ) {
        if (line1 == line2 && oLine1 == oLine2) {
            return;
        }
        if (line1 == line2) {
            insertLines(document1, line1, getLinesContent(document2, oLine1, oLine2));
        }
        else if (oLine1 == oLine2) {
            deleteLines(document1, line1, line2);
        }
        else {
            replaceLines(document1, line1, line2, getLinesContent(document2, oLine1, oLine2));
        }
    }

    @Nonnull
    public static CharSequence getLinesContent(@Nonnull Document document, int line1, int line2) {
        TextRange otherRange = getLinesRange(document, line1, line2);
        return document.getImmutableCharSequence().subSequence(otherRange.getStartOffset(), otherRange.getEndOffset());
    }

    /**
     * Return affected range, without non-internal newlines
     * <p/>
     * we consider '\n' not as a part of line, but a separator between lines
     * ex: if last line is not empty, the last symbol will not be '\n'
     */
    public static TextRange getLinesRange(@Nonnull Document document, int line1, int line2) {
        return getLinesRange(document, line1, line2, false);
    }

    @Nonnull
    public static TextRange getLinesRange(@Nonnull Document document, int line1, int line2, boolean includeNewline) {
        if (line1 == line2) {
            int lineStartOffset = line1 < getLineCount(document) ? document.getLineStartOffset(line1) : document.getTextLength();
            return new TextRange(lineStartOffset, lineStartOffset);
        }
        else {
            int startOffset = document.getLineStartOffset(line1);
            int endOffset = document.getLineEndOffset(line2 - 1);
            if (includeNewline && endOffset < document.getTextLength()) {
                endOffset++;
            }
            return new TextRange(startOffset, endOffset);
        }
    }

    public static int getOffset(@Nonnull Document document, int line, int column) {
        if (line < 0) {
            return 0;
        }
        if (line >= getLineCount(document)) {
            return document.getTextLength();
        }

        int start = document.getLineStartOffset(line);
        int end = document.getLineEndOffset(line);
        return Math.min(start + column, end);
    }

    public static int getLineCount(@Nonnull Document document) {
        return Math.max(document.getLineCount(), 1);
    }

    public static int lineToY(@Nonnull Editor editor, int line) {
        Document document = editor.getDocument();
        if (line >= getLineCount(document)) {
            int y = lineToY(editor, getLineCount(document) - 1);
            return y + editor.getLineHeight() * (line - getLineCount(document) + 1);
        }
        return editor.logicalPositionToXY(editor.offsetToLogicalPosition(document.getLineStartOffset(line))).y;
    }

    @Nonnull
    public static List<String> getLines(@Nonnull Document document) {
        return getLines(document, 0, getLineCount(document));
    }

    @Nonnull
    public static List<String> getLines(@Nonnull Document document, int startLine, int endLine) {
        if (startLine < 0 || startLine > endLine || endLine > getLineCount(document)) {
            throw new IndexOutOfBoundsException(String.format(
                "Wrong line range: [%d, %d); lineCount: '%d'",
                startLine,
                endLine,
                document.getLineCount()
            ));
        }

        List<String> result = new ArrayList<>();
        for (int i = startLine; i < endLine; i++) {
            int start = document.getLineStartOffset(i);
            int end = document.getLineEndOffset(i);
            result.add(document.getText(new TextRange(start, end)));
        }
        return result;
    }

    //
    // Updating ranges on change
    //

    @Nonnull
    public static LineRange getAffectedLineRange(@Nonnull DocumentEvent e) {
        int line1 = e.getDocument().getLineNumber(e.getOffset());
        int line2 = e.getDocument().getLineNumber(e.getOffset() + e.getOldLength()) + 1;
        return new LineRange(line1, line2);
    }

    public static int countLinesShift(@Nonnull DocumentEvent e) {
        return StringUtil.countNewLines(e.getNewFragment()) - StringUtil.countNewLines(e.getOldFragment());
    }

    @Nonnull
    public static UpdatedLineRange updateRangeOnModification(int start, int end, int changeStart, int changeEnd, int shift) {
        return updateRangeOnModification(start, end, changeStart, changeEnd, shift, false);
    }

    @Nonnull
    public static UpdatedLineRange updateRangeOnModification(
        int start,
        int end,
        int changeStart,
        int changeEnd,
        int shift,
        boolean greedy
    ) {
        if (end <= changeStart) { // change before
            return new UpdatedLineRange(start, end, false);
        }
        if (start >= changeEnd) { // change after
            return new UpdatedLineRange(start + shift, end + shift, false);
        }

        if (start <= changeStart && end >= changeEnd) { // change inside
            return new UpdatedLineRange(start, end + shift, false);
        }

        // range is damaged. We don't know new boundaries.
        // But we can try to return approximate new position
        int newChangeEnd = changeEnd + shift;

        if (start >= changeStart && end <= changeEnd) { // fully inside change
            return greedy ? new UpdatedLineRange(changeStart, newChangeEnd, true) :
                new UpdatedLineRange(newChangeEnd, newChangeEnd, true);
        }

        if (start < changeStart) { // bottom boundary damaged
            return greedy ? new UpdatedLineRange(start, newChangeEnd, true) :
                new UpdatedLineRange(start, changeStart, true);
        }
        else { // top boundary damaged
            return greedy ? new UpdatedLineRange(changeStart, end + shift, true) :
                new UpdatedLineRange(newChangeEnd, end + shift, true);
        }
    }

    public static class UpdatedLineRange {
        public final int startLine;
        public final int endLine;
        public final boolean damaged;

        public UpdatedLineRange(int startLine, int endLine, boolean damaged) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.damaged = damaged;
        }
    }

    //
    // Types
    //

    @Nonnull
    public static TextDiffType getLineDiffType(@Nonnull LineFragment fragment) {
        boolean left = fragment.getStartLine1() != fragment.getEndLine1();
        boolean right = fragment.getStartLine2() != fragment.getEndLine2();
        return getDiffType(left, right);
    }

    @Nonnull
    public static TextDiffType getDiffType(@Nonnull DiffFragment fragment) {
        boolean left = fragment.getEndOffset1() != fragment.getStartOffset1();
        boolean right = fragment.getEndOffset2() != fragment.getStartOffset2();
        return getDiffType(left, right);
    }

    @Nonnull
    public static TextDiffType getDiffType(boolean hasDeleted, boolean hasInserted) {
        if (hasDeleted && hasInserted) {
            return TextDiffType.MODIFIED;
        }
        else if (hasDeleted) {
            return TextDiffType.DELETED;
        }
        else if (hasInserted) {
            return TextDiffType.INSERTED;
        }
        else {
            LOG.error("Diff fragment should not be empty");
            return TextDiffType.MODIFIED;
        }
    }

    @Nonnull
    public static MergeConflictType getMergeType(
        @Nonnull Condition<ThreeSide> emptiness,
        @Nonnull BiPredicate<ThreeSide, ThreeSide> equality
    ) {
        boolean isLeftEmpty = emptiness.value(ThreeSide.LEFT);
        boolean isBaseEmpty = emptiness.value(ThreeSide.BASE);
        boolean isRightEmpty = emptiness.value(ThreeSide.RIGHT);
        assert !isLeftEmpty || !isBaseEmpty || !isRightEmpty;

        if (isBaseEmpty) {
            if (isLeftEmpty) { // --=
                return new MergeConflictType(TextDiffType.INSERTED, false, true);
            }
            else if (isRightEmpty) { // =--
                return new MergeConflictType(TextDiffType.INSERTED, true, false);
            }
            else { // =-=
                boolean equalModifications = equality.test(ThreeSide.LEFT, ThreeSide.RIGHT);
                return new MergeConflictType(equalModifications ? TextDiffType.INSERTED : TextDiffType.CONFLICT);
            }
        }
        else if (isLeftEmpty && isRightEmpty) { // -=-
            return new MergeConflictType(TextDiffType.DELETED);
        }
        else { // -==, ==-, ===
            boolean unchangedLeft = equality.test(ThreeSide.BASE, ThreeSide.LEFT);
            boolean unchangedRight = equality.test(ThreeSide.BASE, ThreeSide.RIGHT);
            assert !unchangedLeft || !unchangedRight;

            if (unchangedLeft) {
                return new MergeConflictType(isRightEmpty ? TextDiffType.DELETED : TextDiffType.MODIFIED, false, true);
            }
            if (unchangedRight) {
                return new MergeConflictType(isLeftEmpty ? TextDiffType.DELETED : TextDiffType.MODIFIED, true, false);
            }

            boolean equalModifications = equality.test(ThreeSide.LEFT, ThreeSide.RIGHT);
            return new MergeConflictType(equalModifications ? TextDiffType.MODIFIED : TextDiffType.CONFLICT);
        }
    }

    @Nonnull
    public static MergeConflictType getLineMergeType(
        @Nonnull MergeLineFragment fragment,
        @Nonnull List<? extends Document> documents,
        @Nonnull ComparisonPolicy policy
    ) {
        return getMergeType(
            (side) -> isLineMergeIntervalEmpty(fragment, side),
            (side1, side2) -> compareLineMergeContents(fragment, documents, policy, side1, side2)
        );
    }

    private static boolean compareLineMergeContents(
        @Nonnull MergeLineFragment fragment,
        @Nonnull List<? extends Document> documents,
        @Nonnull ComparisonPolicy policy,
        @Nonnull ThreeSide side1,
        @Nonnull ThreeSide side2
    ) {
        int start1 = fragment.getStartLine(side1);
        int end1 = fragment.getEndLine(side1);
        int start2 = fragment.getStartLine(side2);
        int end2 = fragment.getEndLine(side2);

        if (end2 - start2 != end1 - start1) {
            return false;
        }

        Document document1 = side1.select(documents);
        Document document2 = side2.select(documents);

        for (int i = 0; i < end1 - start1; i++) {
            int line1 = start1 + i;
            int line2 = start2 + i;

            CharSequence content1 = getLinesContent(document1, line1, line1 + 1);
            CharSequence content2 = getLinesContent(document2, line2, line2 + 1);
            if (!ComparisonManager.getInstance().isEquals(content1, content2, policy)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isLineMergeIntervalEmpty(@Nonnull MergeLineFragment fragment, @Nonnull ThreeSide side) {
        return fragment.getStartLine(side) == fragment.getEndLine(side);
    }

    //
    // Writable
    //

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public static <R> CommandBuilder<R, ? extends CommandBuilder<R, ?>> newWriteCommand() {
        CommandBuilder commandBuilder = CommandProcessor.getInstance().newCommand().inWriteAction();

        return new ProxyCommandBuilder(commandBuilder) {
            @Override
            @RequiredUIAccess
            public void run(@RequiredUIAccess @Nonnull Runnable runnable) {
                CommandDescriptor descriptor = mySubBuilder.build(runnable);
                if (!makeWritable(descriptor.project(), descriptor.document())) {
                    VirtualFile file = FileDocumentManager.getInstance().getFile(descriptor.document());
                    LOG.warn("Document is read-only" + (file != null ? ": " + file.getPresentableName() : ""));
                    return;
                }

                super.run(runnable);
            }
        };
    }

    @Deprecated(forRemoval = true)
    @RequiredUIAccess
    public static void executeWriteCommand(
        @Nullable Project project,
        @Nonnull Document document,
        @Nullable String commandName,
        @Nullable String commandGroupId,
        @Nonnull UndoConfirmationPolicy confirmationPolicy,
        boolean underBulkUpdate,
        @Nonnull Runnable task
    ) {
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .document(document)
            .name(LocalizeValue.ofNullable(commandName))
            .groupId(commandGroupId)
            .undoConfirmationPolicy(confirmationPolicy)
            .inWriteAction()
            .inBulkUpdateIf(underBulkUpdate)
            .run(task);
    }

    @Deprecated(forRemoval = true)
    @RequiredUIAccess
    public static void executeWriteCommand(
        @Nonnull Document document,
        @Nullable Project project,
        @Nullable String commandName,
        @Nonnull Runnable task
    ) {
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .document(document)
            .name(LocalizeValue.ofNullable(commandName))
            .inWriteAction()
            .run(task);
    }

    public static boolean isEditable(@Nonnull Editor editor) {
        return !editor.isViewer() && canMakeWritable(editor.getDocument());
    }

    public static boolean canMakeWritable(@Nonnull Document document) {
        if (document.isWritable()) {
            return true;
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return file != null && file.isValid() && file.isInLocalFileSystem() && !file.isWritable();
    }

    @RequiredUIAccess
    public static boolean makeWritable(@Nullable Project project, @Nonnull Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null || !file.isValid()) {
            return document.isWritable();
        }
        return makeWritable(project, file) && document.isWritable();
    }

    @RequiredUIAccess
    public static boolean makeWritable(@Nullable Project project, @Nonnull VirtualFile file) {
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        return !ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(file).hasReadonlyFiles();
    }

    public static void putNonundoableOperation(@Nullable Project project, @Nonnull Document document) {
        UndoManager undoManager = project != null ? ProjectUndoManager.getInstance(project) : ApplicationUndoManager.getInstance();
        if (undoManager != null) {
            DocumentReference ref = DocumentReferenceManager.getInstance().create(document);
            undoManager.nonundoableActionPerformed(ref, false);
        }
    }

    /**
     * Difference with {@link VfsUtil#markDirtyAndRefresh} is that refresh from VfsUtil will be performed with ModalityState.NON_MODAL.
     */
    public static void markDirtyAndRefresh(boolean async, boolean recursive, boolean reloadChildren, @Nonnull VirtualFile... files) {
        ModalityState modalityState = Application.get().getDefaultModalityState();
        VirtualFileUtil.markDirty(recursive, reloadChildren, files);
        RefreshQueue.getInstance().refresh(async, recursive, null, modalityState, files);
    }

    public static <T> UserDataHolderBase createUserDataHolder(@Nonnull Key<T> key, @Nullable T value) {
        UserDataHolderBase holder = new UserDataHolderBase();
        holder.putUserData(key, value);
        return holder;
    }

    public static boolean isUserDataFlagSet(@Nonnull Key<Boolean> key, UserDataHolder... holders) {
        for (UserDataHolder holder : holders) {
            if (holder == null) {
                continue;
            }
            Boolean data = holder.getUserData(key);
            if (data != null) {
                return data;
            }
        }
        return false;
    }

    public static <T> T getUserData(@Nullable UserDataHolder first, @Nullable UserDataHolder second, @Nonnull Key<T> key) {
        if (first != null) {
            T data = first.getUserData(key);
            if (data != null) {
                return data;
            }
        }
        if (second != null) {
            T data = second.getUserData(key);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    @Nullable
    public static VirtualFile getVirtualFile(@Nonnull ContentDiffRequest request, @Nonnull Side currentSide) {
        List<DiffContent> contents = request.getContents();
        DiffContent content1 = currentSide.select(contents);
        DiffContent content2 = currentSide.other().select(contents);

        if (content1 instanceof FileContent) {
            return ((FileContent)content1).getFile();
        }
        if (content2 instanceof FileContent) {
            return ((FileContent)content2).getFile();
        }
        return null;
    }

    @Nullable
    public static VirtualFile getVirtualFile(@Nonnull ContentDiffRequest request, @Nonnull ThreeSide currentSide) {
        List<DiffContent> contents = request.getContents();
        DiffContent content1 = currentSide.select(contents);
        DiffContent content2 = ThreeSide.BASE.select(contents);

        if (content1 instanceof FileContent) {
            return ((FileContent)content1).getFile();
        }
        if (content2 instanceof FileContent) {
            return ((FileContent)content2).getFile();
        }
        return null;
    }

    @Nullable
    public static Object getData(@Nullable DataProvider provider, @Nullable DataProvider fallbackProvider, Key<?> dataId) {
        if (provider != null) {
            Object data = provider.getData(dataId);
            if (data != null) {
                return data;
            }
        }
        if (fallbackProvider != null) {
            Object data = fallbackProvider.getData(dataId);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    @Nonnull
    public static DiffSettingsHolder.DiffSettings getDiffSettings(@Nonnull DiffContext context) {
        DiffSettingsHolder.DiffSettings settings = context.getUserData(DiffSettingsHolder.KEY);
        if (settings == null) {
            settings = DiffSettingsHolder.DiffSettings.getSettings(context.getUserData(DiffUserDataKeys.PLACE));
            context.putUserData(DiffSettingsHolder.KEY, settings);
        }
        return settings;
    }

    //
    // Tools
    //

    @Nonnull
    public static <T extends DiffTool> List<T> filterSuppressedTools(@Nonnull List<T> tools) {
        if (tools.size() < 2) {
            return tools;
        }

        final List<Class<? extends DiffTool>> suppressedTools = new ArrayList<>();
        for (T tool : tools) {
            try {
                if (tool instanceof SuppressiveDiffTool) {
                    suppressedTools.addAll(((SuppressiveDiffTool)tool).getSuppressedTools());
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        if (suppressedTools.isEmpty()) {
            return tools;
        }

        List<T> filteredTools = ContainerUtil.filter(tools, tool -> !suppressedTools.contains(tool.getClass()));
        return filteredTools.isEmpty() ? tools : filteredTools;
    }

    //
    // Helpers
    //

    public static class DiffConfig {
        @Nonnull
        public final ComparisonPolicy policy;
        public final boolean innerFragments;
        public final boolean squashFragments;
        public final boolean trimFragments;

        public DiffConfig(@Nonnull ComparisonPolicy policy, boolean innerFragments, boolean squashFragments, boolean trimFragments) {
            this.policy = policy;
            this.innerFragments = innerFragments;
            this.squashFragments = squashFragments;
            this.trimFragments = trimFragments;
        }

        public DiffConfig(@Nonnull IgnorePolicy ignorePolicy, @Nonnull HighlightPolicy highlightPolicy) {
            this(ignorePolicy.getComparisonPolicy(), highlightPolicy.isFineFragments(), highlightPolicy.isShouldSquash(),
                ignorePolicy.isShouldTrimChunks()
            );
        }
    }
}
