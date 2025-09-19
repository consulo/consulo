/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.generation;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.codeInsight.CommentUtil;
import consulo.ide.impl.idea.codeInsight.actions.MultiCaretCodeInsightActionHandler;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.fileTypes.impl.AbstractFileType;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.*;
import consulo.language.codeStyle.*;
import consulo.language.codeStyle.internal.IndentData;
import consulo.language.custom.CustomSyntaxTableFileType;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.internal.custom.SyntaxTable;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class CommentByLineCommentHandler extends MultiCaretCodeInsightActionHandler {
    private Project myProject;

    private final List<Block> myBlocks = new ArrayList<>();

    // first pass - adjacent carets are grouped into blocks
    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Caret caret, @Nonnull PsiFile file) {
        myProject = project;
        file = file.getViewProvider().getPsi(file.getViewProvider().getBaseLanguage());

        PsiElement context = InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file);

        if (context != null && (context.textContains('\'') || context.textContains('\"') || context.textContains('/'))) {
            String s = context.getText();
            if (StringUtil.startsWith(s, "\"") || StringUtil.startsWith(s, "\'") || StringUtil.startsWith(s, "/")) {
                file = context.getContainingFile();
                editor = editor instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : editor;
                caret = caret instanceof CaretDelegate caretDelegate ? caretDelegate.getDelegate() : caret;
            }
        }

        Document document = editor.getDocument();
        boolean hasSelection = caret.hasSelection();
        int startOffset = caret.getSelectionStart();
        int endOffset = caret.getSelectionEnd();

        FoldRegion fold = editor.getFoldingModel().getCollapsedRegionAtOffset(startOffset);
        if (fold != null && fold.shouldNeverExpand() && fold.getStartOffset() == startOffset && fold.getEndOffset() == endOffset) {
            // Foldings that never expand are automatically selected, so the fact it is selected must not interfere with commenter's logic
            hasSelection = false;
        }

        if (document.getTextLength() == 0) {
            return;
        }

        while (true) {
            int firstLineStart = DocumentUtil.getLineStartOffset(startOffset, document);
            FoldRegion collapsedAt = editor.getFoldingModel().getCollapsedRegionAtOffset(firstLineStart - 1);
            if (collapsedAt == null) {
                break;
            }
            int regionStartOffset = collapsedAt.getStartOffset();
            if (regionStartOffset >= startOffset) {
                break;
            }
            startOffset = regionStartOffset;
        }
        while (true) {
            int lastLineEnd = DocumentUtil.getLineEndOffset(endOffset, document);
            FoldRegion collapsedAt = editor.getFoldingModel().getCollapsedRegionAtOffset(lastLineEnd);
            if (collapsedAt == null) {
                break;
            }
            int regionEndOffset = collapsedAt.getEndOffset();
            if (regionEndOffset <= endOffset) {
                break;
            }
            endOffset = regionEndOffset;
        }

        int startLine = document.getLineNumber(startOffset);
        int endLine = document.getLineNumber(endOffset);

        if (endLine > startLine && document.getLineStartOffset(endLine) == endOffset) {
            endLine--;
        }

        Block lastBlock = myBlocks.isEmpty() ? null : myBlocks.get(myBlocks.size() - 1);
        Block currentBlock;
        if (lastBlock == null || lastBlock.editor != editor || lastBlock.psiFile != file || startLine > (lastBlock.endLine + 1)) {
            currentBlock = new Block();
            currentBlock.editor = editor;
            currentBlock.psiFile = file;
            currentBlock.startLine = startLine;
            myBlocks.add(currentBlock);
        }
        else {
            currentBlock = lastBlock;
        }
        currentBlock.carets.add(caret);
        currentBlock.endLine = endLine;

        boolean wholeLinesSelected =
            !hasSelection || startOffset == document.getLineStartOffset(document.getLineNumber(startOffset)) && endOffset == document.getLineEndOffset(document.getLineNumber(endOffset - 1)) + 1;
        boolean startingNewLineComment =
            !hasSelection && isLineEmpty(document, document.getLineNumber(startOffset)) && !Comparing.equal(IdeActions.ACTION_COMMENT_LINE, ActionManagerEx.getInstanceEx().getPrevPreformedActionId());
        currentBlock.caretUpdate = startingNewLineComment ? CaretUpdate.PUT_AT_COMMENT_START : !hasSelection ? CaretUpdate.SHIFT_DOWN : wholeLinesSelected ? CaretUpdate.RESTORE_SELECTION : null;
    }

    @Override
    public void postInvoke() {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.comment.line");

        CodeStyleSettings codeStyleSettings = CodeStyleSettingsManager.getSettings(myProject);

        // second pass - determining whether we need to comment or to uncomment
        boolean allLinesCommented = true;
        for (Block block : myBlocks) {
            int startLine = block.startLine;
            int endLine = block.endLine;
            Document document = block.editor.getDocument();
            PsiFile psiFile = block.psiFile;
            block.startOffsets = new int[endLine - startLine + 1];
            block.endOffsets = new int[endLine - startLine + 1];
            block.commenters = new Commenter[endLine - startLine + 1];
            block.commenterStateMap = new HashMap<>();
            CharSequence chars = document.getCharsSequence();

            boolean singleline = startLine == endLine;
            int offset = document.getLineStartOffset(startLine);
            offset = CharArrayUtil.shiftForward(chars, offset, " \t");

            int endOffset = CharArrayUtil.shiftBackward(chars, document.getLineEndOffset(endLine), " \t\n");

            block.blockSuitableCommenter = getBlockSuitableCommenter(psiFile, offset, endOffset);
            Language lineStartLanguage = getLineStartLanguage(block.editor, psiFile, startLine);
            CommonCodeStyleSettings languageSettings = codeStyleSettings.getCommonSettings(lineStartLanguage);
            block.commentWithIndent = !languageSettings.LINE_COMMENT_AT_FIRST_COLUMN;
            block.addSpace = languageSettings.LINE_COMMENT_ADD_SPACE;

            for (int line = startLine; line <= endLine; line++) {
                Commenter commenter = block.blockSuitableCommenter != null ? block.blockSuitableCommenter : findCommenter(block.editor, psiFile, line);
                if (commenter == null || commenter.getLineCommentPrefix() == null && (commenter.getBlockCommentPrefix() == null || commenter.getBlockCommentSuffix() == null)) {
                    block.skip = true;
                    break;
                }

                if (commenter instanceof SelfManagingCommenter && block.commenterStateMap.get(commenter) == null) {
                    SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter) commenter;
                    CommenterDataHolder state = selfManagingCommenter.createLineCommentingState(startLine, endLine, document, psiFile);
                    if (state == null) {
                        state = SelfManagingCommenter.EMPTY_STATE;
                    }
                    block.commenterStateMap.put(selfManagingCommenter, state);
                }

                block.commenters[line - startLine] = commenter;
                if (!isLineCommented(block, line, commenter) && (singleline || !isLineEmpty(document, line))) {
                    allLinesCommented = false;
                    if (commenter instanceof IndentedCommenter) {
                        Boolean value = ((IndentedCommenter) commenter).forceIndentedLineComment();
                        if (value != null) {
                            block.commentWithIndent = value;
                        }
                    }
                    break;
                }
            }
        }
        boolean moveCarets = true;
        for (Block block : myBlocks) {
            if (block.carets.size() > 1 && block.startLine != block.endLine) {
                moveCarets = false;
                break;
            }
        }
        // third pass - actual change
        Collections.reverse(myBlocks);
        for (Block block : myBlocks) {
            if (!block.skip) {
                if (!allLinesCommented) {
                    if (!block.commentWithIndent) {
                        doDefaultCommenting(block);
                    }
                    else {
                        doIndentCommenting(block);
                    }
                }
                else {
                    for (int line = block.endLine; line >= block.startLine; line--) {
                        uncommentLine(block, line, block.addSpace);
                    }
                }
            }

            if (!moveCarets || block.caretUpdate == null) {
                continue;
            }
            Document document = block.editor.getDocument();
            for (Caret caret : block.carets) {
                switch (block.caretUpdate) {
                    case PUT_AT_COMMENT_START:
                        Commenter commenter = block.commenters[0];
                        if (commenter != null) {
                            String prefix;
                            if (commenter instanceof SelfManagingCommenter) {
                                prefix = ((SelfManagingCommenter) commenter).getCommentPrefix(block.startLine, document, block.commenterStateMap.get((SelfManagingCommenter) commenter));
                                if (prefix == null) {
                                    prefix = ""; // TODO
                                }
                            }
                            else {
                                prefix = commenter.getLineCommentPrefix();
                                if (prefix == null) {
                                    prefix = commenter.getBlockCommentPrefix();
                                }
                            }

                            int lineStart = document.getLineStartOffset(block.startLine);
                            lineStart = CharArrayUtil.shiftForward(document.getCharsSequence(), lineStart, " \t");
                            lineStart += prefix.length();
                            lineStart = CharArrayUtil.shiftForward(document.getCharsSequence(), lineStart, " \t");
                            if (lineStart > document.getTextLength()) {
                                lineStart = document.getTextLength();
                            }
                            caret.moveToOffset(lineStart);
                        }
                        break;
                    case SHIFT_DOWN:
                        // Don't tweak caret position if we're already located on the last document line.
                        LogicalPosition position = caret.getLogicalPosition();
                        if (position.line < document.getLineCount() - 1) {
                            int verticalShift = 1 + block.editor.getSoftWrapModel().getSoftWrapsForLine(position.line).size() - EditorUtil.getSoftWrapCountAfterLineStart(block.editor, position);
                            caret.moveCaretRelatively(0, verticalShift, false, true);
                        }
                        break;
                    case RESTORE_SELECTION:
                        caret.setSelection(document.getLineStartOffset(document.getLineNumber(caret.getSelectionStart())), caret.getSelectionEnd());
                }
            }
        }
    }

    private static Commenter getBlockSuitableCommenter(final PsiFile file, int offset, int endOffset) {
        Language languageSuitableForCompleteFragment;
        if (offset >= endOffset) {  // we are on empty line
            PsiElement element = file.findElementAt(offset);
            if (element != null) {
                languageSuitableForCompleteFragment = element.getParent().getLanguage();
            }
            else {
                languageSuitableForCompleteFragment = null;
            }
        }
        else {
            languageSuitableForCompleteFragment = PsiUtilBase.reallyEvaluateLanguageInRange(offset, endOffset, file);
        }


        Commenter blockSuitableCommenter = languageSuitableForCompleteFragment == null ? Commenter.forLanguage(file.getLanguage()) : null;
        if (blockSuitableCommenter == null && file.getFileType() instanceof CustomSyntaxTableFileType) {
            blockSuitableCommenter = new Commenter() {
                final SyntaxTable mySyntaxTable = ((CustomSyntaxTableFileType) file.getFileType()).getSyntaxTable();

                @Override
                @Nullable
                public String getLineCommentPrefix() {
                    return mySyntaxTable.getLineComment();
                }

                @Override
                @Nullable
                public String getBlockCommentPrefix() {
                    return mySyntaxTable.getStartComment();
                }

                @Override
                @Nullable
                public String getBlockCommentSuffix() {
                    return mySyntaxTable.getEndComment();
                }

                @Nonnull
                @Override
                public Language getLanguage() {
                    return Language.ANY;
                }

                @Override
                public String getCommentedBlockCommentPrefix() {
                    return null;
                }

                @Override
                public String getCommentedBlockCommentSuffix() {
                    return null;
                }
            };
        }

        return blockSuitableCommenter;
    }

    private static boolean isLineEmpty(Document document, int line) {
        CharSequence chars = document.getCharsSequence();
        int start = document.getLineStartOffset(line);
        int end = Math.min(document.getLineEndOffset(line), document.getTextLength() - 1);
        for (int i = start; i <= end; i++) {
            if (!Character.isWhitespace(chars.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLineCommented(Block block, int line, Commenter commenter) {
        boolean commented;
        int lineEndForBlockCommenting = -1;
        Document document = block.editor.getDocument();
        int lineStart = document.getLineStartOffset(line);
        CharSequence chars = document.getCharsSequence();
        lineStart = CharArrayUtil.shiftForward(chars, lineStart, " \t");

        if (commenter instanceof SelfManagingCommenter) {
            SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter) commenter;
            commented = selfManagingCommenter.isLineCommented(line, lineStart, document, block.commenterStateMap.get(selfManagingCommenter));
        }
        else {
            String prefix = commenter.getLineCommentPrefix();

            if (prefix != null) {
                commented = CharArrayUtil.regionMatches(chars, lineStart, StringUtil.trimTrailing(prefix));
            }
            else {
                prefix = commenter.getBlockCommentPrefix();
                String suffix = commenter.getBlockCommentSuffix();
                int textLength = document.getTextLength();
                lineEndForBlockCommenting = document.getLineEndOffset(line);
                if (lineEndForBlockCommenting == textLength) {
                    int shifted = CharArrayUtil.shiftBackward(chars, textLength - 1, " \t");
                    if (shifted < textLength - 1) {
                        lineEndForBlockCommenting = shifted;
                    }
                }
                else {
                    lineEndForBlockCommenting = CharArrayUtil.shiftBackward(chars, lineEndForBlockCommenting, " \t");
                }
                commented = lineStart == lineEndForBlockCommenting && block.startLine != block.endLine ||
                    CharArrayUtil.regionMatches(chars, lineStart, prefix) && CharArrayUtil.regionMatches(chars, lineEndForBlockCommenting - suffix.length(), suffix);
            }
        }

        if (commented) {
            block.startOffsets[line - block.startLine] = lineStart;
            block.endOffsets[line - block.startLine] = lineEndForBlockCommenting;
        }

        return commented;
    }

    @Nullable
    private static Commenter findCommenter(@Nonnull Editor editor, @Nonnull PsiFile file, int line) {
        FileType fileType = file.getFileType();
        if (fileType instanceof AbstractFileType) {
            return ((AbstractFileType) fileType).getCommenter();
        }
        Language lineStartLanguage = getLineStartLanguage(editor, file, line);
        Language lineEndLanguage = getLineEndLanguage(file, editor, line);
        return CommentByBlockCommentHandler.getCommenter(file, editor, lineStartLanguage, lineEndLanguage);
    }

    @Nonnull
    private static Language getLineStartLanguage(@Nonnull Editor editor, @Nonnull PsiFile file, int line) {
        Document document = editor.getDocument();
        int lineStartOffset = document.getLineStartOffset(line);
        lineStartOffset = Math.max(0, CharArrayUtil.shiftForward(document.getCharsSequence(), lineStartOffset, " \t"));
        return PsiUtilCore.getLanguageAtOffset(file, lineStartOffset);
    }

    @Nonnull
    private static Language getLineEndLanguage(@Nonnull PsiFile file, @Nonnull Editor editor, int line) {
        Document document = editor.getDocument();
        int lineEndOffset = document.getLineEndOffset(line) - 1;
        lineEndOffset = Math.max(0, CharArrayUtil.shiftBackward(document.getCharsSequence(), lineEndOffset < 0 ? 0 : lineEndOffset, " \t"));
        return PsiUtilCore.getLanguageAtOffset(file, lineEndOffset);
    }

    private IndentData computeMinIndent(Editor editor, PsiFile psiFile, int line1, int line2) {
        Document document = editor.getDocument();
        IndentData minIndent = CommentUtil.getMinLineIndent(document, line1, line2, psiFile);
        if (line1 > 0) {
            int commentOffset = getCommentStart(editor, psiFile, line1 - 1);
            if (commentOffset >= 0) {
                int lineStart = document.getLineStartOffset(line1 - 1);
                IndentData indent = IndentData.createFrom(document.getCharsSequence(), lineStart, commentOffset, CodeStyle.getIndentOptions(psiFile).TAB_SIZE);
                minIndent = IndentData.min(minIndent, indent);
            }
        }
        if (minIndent == null) {
            minIndent = new IndentData(0);
        }
        return minIndent;
    }

    private static int getCommentStart(Editor editor, PsiFile psiFile, int line) {
        int offset = editor.getDocument().getLineStartOffset(line);
        CharSequence chars = editor.getDocument().getCharsSequence();
        offset = CharArrayUtil.shiftForward(chars, offset, " \t");
        Commenter commenter = findCommenter(editor, psiFile, line);
        if (commenter == null) {
            return -1;
        }
        String prefix = commenter.getLineCommentPrefix();
        if (prefix == null) {
            prefix = commenter.getBlockCommentPrefix();
        }
        if (prefix == null) {
            return -1;
        }
        return CharArrayUtil.regionMatches(chars, offset, prefix) ? offset : -1;
    }

    public void doDefaultCommenting(Block block) {
        Document document = block.editor.getDocument();
        DocumentUtil.executeInBulk(document, block.endLine - block.startLine >= Registry.intValue("comment.by.line.bulk.lines.trigger"), () -> {
            for (int line = block.endLine; line >= block.startLine; line--) {
                int offset = document.getLineStartOffset(line);
                commentLine(block, line, offset);
            }
        });
    }

    private void doIndentCommenting(Block block) {
        Document document = block.editor.getDocument();
        CharSequence chars = document.getCharsSequence();
        IndentData minIndent = computeMinIndent(block.editor, block.psiFile, block.startLine, block.endLine);
        CommonCodeStyleSettings.IndentOptions indentOptions = CodeStyle.getIndentOptions(block.psiFile);

        DocumentUtil.executeInBulk(document, block.endLine - block.startLine > Registry.intValue("comment.by.line.bulk.lines.trigger"), () -> {
            for (int line = block.endLine; line >= block.startLine; line--) {
                int lineStart = document.getLineStartOffset(line);
                int offset = lineStart;
                StringBuilder buffer = new StringBuilder();
                while (true) {
                    IndentData indent = IndentData.createFrom(buffer, 0, buffer.length(), indentOptions.TAB_SIZE);
                    if (indent.getTotalSpaces() >= minIndent.getTotalSpaces()) {
                        break;
                    }
                    char c = chars.charAt(offset);
                    if (c != ' ' && c != '\t') {
                        String newSpace = minIndent.createIndentInfo().generateNewWhiteSpace(indentOptions);
                        document.replaceString(lineStart, offset, newSpace);
                        offset = lineStart + newSpace.length();
                        break;
                    }
                    buffer.append(c);
                    offset++;
                }
                commentLine(block, line, offset);
            }
        });
    }

    private static void uncommentRange(Document document, int startOffset, int endOffset, @Nonnull Commenter commenter) {
        String commentedSuffix = commenter.getCommentedBlockCommentSuffix();
        String commentedPrefix = commenter.getCommentedBlockCommentPrefix();
        String prefix = commenter.getBlockCommentPrefix();
        String suffix = commenter.getBlockCommentSuffix();
        if (prefix == null || suffix == null) {
            return;
        }
        if (endOffset >= suffix.length() && CharArrayUtil.regionMatches(document.getCharsSequence(), endOffset - suffix.length(), suffix)) {
            document.deleteString(endOffset - suffix.length(), endOffset);
            endOffset -= suffix.length();
        }
        if (commentedPrefix != null && commentedSuffix != null) {
            CommentByBlockCommentHandler.commentNestedComments(document, new TextRange(startOffset, endOffset), commenter);
        }
        document.deleteString(startOffset, startOffset + prefix.length());
    }

    private static void uncommentLine(Block block, int line, boolean removeSpace) {
        Document document = block.editor.getDocument();
        Commenter commenter = block.commenters[line - block.startLine];
        if (commenter == null) {
            commenter = findCommenter(block.editor, block.psiFile, line);
        }
        if (commenter == null) {
            return;
        }

        int startOffset = block.startOffsets[line - block.startLine];
        int endOffset = block.endOffsets[line - block.startLine];
        if (startOffset == endOffset) {
            return;
        }

        if (commenter instanceof SelfManagingCommenter) {
            SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter) commenter;
            selfManagingCommenter.uncommentLine(line, startOffset, document, block.commenterStateMap.get(selfManagingCommenter));
            return;
        }

        RangeMarker marker = endOffset > startOffset ? block.editor.getDocument().createRangeMarker(startOffset, endOffset) : null;
        try {
            if (doUncommentLine(line, document, commenter, startOffset, endOffset, removeSpace)) {
                return;
            }
            if (marker != null) {
                CommentByBlockCommentHandler.processDocument(document, marker, commenter, false);
            }
        }
        finally {
            if (marker != null) {
                marker.dispose();
            }
        }
    }

    private static boolean doUncommentLine(int line, Document document, Commenter commenter, int startOffset, int endOffset, boolean removeSpace) {
        String prefix = commenter.getLineCommentPrefix();
        if (prefix != null) {
            if (removeSpace) {
                prefix += ' ';
            }
            CharSequence chars = document.getCharsSequence();

            if (commenter instanceof CommenterWithLineSuffix) {
                CommenterWithLineSuffix commenterWithLineSuffix = (CommenterWithLineSuffix) commenter;
                String suffix = commenterWithLineSuffix.getLineCommentSuffix();


                int theEnd = endOffset > 0 ? endOffset : document.getLineEndOffset(line);
                while (theEnd > startOffset && Character.isWhitespace(chars.charAt(theEnd - 1))) {
                    theEnd--;
                }


                String lineText = document.getText(new TextRange(startOffset, theEnd));
                if (lineText.indexOf(suffix) != -1) {
                    int start = startOffset + lineText.indexOf(suffix);
                    document.deleteString(start, start + suffix.length());
                }
            }

            boolean matchesTrimmed = false;
            boolean commented = CharArrayUtil.regionMatches(chars, startOffset, prefix) || (matchesTrimmed = prefix.endsWith(" ") && CharArrayUtil.regionMatches(chars, startOffset, prefix.trim()));
            assert commented;

            int charsToDelete = matchesTrimmed ? prefix.trim().length() : prefix.length();
            document.deleteString(startOffset, startOffset + charsToDelete);

            // delete whitespace on line if that's all that left after uncommenting
            int lineStartOffset = document.getLineStartOffset(line);
            int lineEndOffset = document.getLineEndOffset(line);
            if (CharArrayUtil.isEmptyOrSpaces(chars, lineStartOffset, lineEndOffset)) {
                document.deleteString(lineStartOffset, lineEndOffset);
            }

            return true;
        }
        String text = document.getCharsSequence().subSequence(startOffset, endOffset).toString();

        prefix = commenter.getBlockCommentPrefix();
        String suffix = commenter.getBlockCommentSuffix();
        if (prefix == null || suffix == null) {
            return true;
        }

        IntList prefixes = IntLists.newArrayList();
        IntList suffixes = IntLists.newArrayList();
        for (int position = 0; position < text.length(); ) {
            int prefixPos = text.indexOf(prefix, position);
            if (prefixPos == -1) {
                break;
            }
            prefixes.add(prefixPos);
            position = prefixPos + prefix.length();
            int suffixPos = text.indexOf(suffix, position);
            if (suffixPos == -1) {
                suffixPos = text.length() - suffix.length();
            }
            suffixes.add(suffixPos);
            position = suffixPos + suffix.length();
        }

        assert prefixes.size() == suffixes.size();

        for (int i = prefixes.size() - 1; i >= 0; i--) {
            uncommentRange(document, startOffset + prefixes.get(i), Math.min(startOffset + suffixes.get(i) + suffix.length(), endOffset), commenter);
        }
        return false;
    }

    private static void commentLine(Block block, int line, int offset) {
        Commenter commenter = block.blockSuitableCommenter;
        Document document = block.editor.getDocument();
        if (commenter == null) {
            commenter = findCommenter(block.editor, block.psiFile, line);
        }
        if (commenter == null) {
            return;
        }
        if (commenter instanceof SelfManagingCommenter) {
            SelfManagingCommenter selfManagingCommenter = (SelfManagingCommenter) commenter;
            selfManagingCommenter.commentLine(line, offset, document, block.commenterStateMap.get(selfManagingCommenter));
            return;
        }

        int endOffset = document.getLineEndOffset(line);
        RangeMarker marker = document.createRangeMarker(offset, endOffset);
        marker.setGreedyToLeft(true);
        marker.setGreedyToRight(true);
        try {
            if (doCommentLine(block, line, offset, endOffset, commenter, document)) {
                return;
            }
            CommentByBlockCommentHandler.processDocument(document, marker, commenter, true);
        }
        finally {
            marker.dispose();
        }
    }

    private static boolean doCommentLine(Block block, int line, int offset, int endOffset, Commenter commenter, Document document) {
        String prefix = commenter.getLineCommentPrefix();
        int shiftedStartOffset = CharArrayUtil.shiftForward(document.getCharsSequence(), offset, " \t");
        if (prefix != null) {
            if (commenter instanceof CommenterWithLineSuffix) {
                endOffset = CharArrayUtil.shiftBackward(document.getCharsSequence(), endOffset, " \t");
                String lineSuffix = ((CommenterWithLineSuffix) commenter).getLineCommentSuffix();
                if (!CharArrayUtil.regionMatches(document.getCharsSequence(), shiftedStartOffset, prefix)) {
                    if (!CharArrayUtil.regionMatches(document.getCharsSequence(), endOffset - lineSuffix.length(), lineSuffix)) {
                        document.insertString(endOffset, lineSuffix);
                    }
                    document.insertString(offset, prefix);
                }
            }
            else {
                if (block.addSpace && shiftedStartOffset < document.getTextLength() && document.getCharsSequence().charAt(shiftedStartOffset) != '\n') {
                    prefix += ' ';
                }
                document.insertString(offset, prefix);
            }
        }
        else {
            prefix = commenter.getBlockCommentPrefix();
            String suffix = commenter.getBlockCommentSuffix();
            if (prefix == null || suffix == null) {
                return true;
            }
            if (endOffset == offset && block.startLine != block.endLine) {
                return true;
            }
            int textLength = document.getTextLength();
            CharSequence chars = document.getCharsSequence();
            offset = CharArrayUtil.shiftForward(chars, offset, " \t");
            if (endOffset == textLength) {
                int shifted = CharArrayUtil.shiftBackward(chars, textLength - 1, " \t") + 1;
                if (shifted < textLength) {
                    endOffset = shifted;
                }
            }
            else {
                endOffset = CharArrayUtil.shiftBackward(chars, endOffset, " \t");
            }
            if (endOffset < offset || offset == textLength - 1 && line != document.getLineCount() - 1) {
                return true;
            }
            String text = chars.subSequence(offset, endOffset).toString();
            IntList prefixes = IntLists.newArrayList();
            IntList suffixes = IntLists.newArrayList();
            String commentedSuffix = commenter.getCommentedBlockCommentSuffix();
            String commentedPrefix = commenter.getCommentedBlockCommentPrefix();
            for (int position = 0; position < text.length(); ) {
                int nearestPrefix = text.indexOf(prefix, position);
                if (nearestPrefix == -1) {
                    nearestPrefix = text.length();
                }
                int nearestSuffix = text.indexOf(suffix, position);
                if (nearestSuffix == -1) {
                    nearestSuffix = text.length();
                }
                if (Math.min(nearestPrefix, nearestSuffix) == text.length()) {
                    break;
                }
                if (nearestPrefix < nearestSuffix) {
                    prefixes.add(nearestPrefix);
                    position = nearestPrefix + prefix.length();
                }
                else {
                    suffixes.add(nearestSuffix);
                    position = nearestSuffix + suffix.length();
                }
            }
            if (!(commentedSuffix == null && !suffixes.isEmpty() && offset + suffixes.get(suffixes.size() - 1) + suffix.length() >= endOffset)) {
                document.insertString(endOffset, suffix);
            }
            int nearestPrefix = prefixes.size() - 1;
            int nearestSuffix = suffixes.size() - 1;
            while (nearestPrefix >= 0 || nearestSuffix >= 0) {
                if (nearestSuffix == -1 || nearestPrefix != -1 && prefixes.get(nearestPrefix) > suffixes.get(nearestSuffix)) {
                    int position = prefixes.get(nearestPrefix);
                    nearestPrefix--;
                    if (commentedPrefix != null) {
                        document.replaceString(offset + position, offset + position + prefix.length(), commentedPrefix);
                    }
                    else if (position != 0) {
                        document.insertString(offset + position, suffix);
                    }
                }
                else {
                    int position = suffixes.get(nearestSuffix);
                    nearestSuffix--;
                    if (commentedSuffix != null) {
                        document.replaceString(offset + position, offset + position + suffix.length(), commentedSuffix);
                    }
                    else if (offset + position + suffix.length() < endOffset) {
                        document.insertString(offset + position + suffix.length(), prefix);
                    }
                }
            }
            if (!(commentedPrefix == null && !prefixes.isEmpty() && prefixes.get(0) == 0)) {
                document.insertString(offset, prefix);
            }
        }
        return false;
    }

    private static class Block {
        private Editor editor;
        private PsiFile psiFile;
        private List<Caret> carets = new ArrayList<>();
        private int startLine;
        private int endLine;
        private int[] startOffsets;
        private int[] endOffsets;
        private Commenter blockSuitableCommenter;
        private Commenter[] commenters;
        private Map<SelfManagingCommenter, CommenterDataHolder> commenterStateMap;
        private boolean commentWithIndent;
        private CaretUpdate caretUpdate;
        private boolean skip;
        private boolean addSpace;
    }

    private enum CaretUpdate {
        PUT_AT_COMMENT_START,
        SHIFT_DOWN,
        RESTORE_SELECTION
    }
}
