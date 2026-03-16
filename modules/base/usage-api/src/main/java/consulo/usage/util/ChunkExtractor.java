/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.usage.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.RangeMarker;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.DefaultSyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.usage.*;
import consulo.util.collection.FactoryMap;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public final class ChunkExtractor {
    private static final Logger LOG = Logger.getInstance(ChunkExtractor.class);
    public static final int MAX_LINE_LENGTH_TO_SHOW = 200;
    public static final int OFFSET_BEFORE_TO_SHOW_WHEN_LONG_LINE = 1;
    public static final int OFFSET_AFTER_TO_SHOW_WHEN_LONG_LINE = 1;

    private final EditorColorsScheme myColorsScheme;

    private final Document myDocument;
    private long myDocumentStamp;
    private final SyntaxHighlighterOverEditorHighlighter myHighlighter;

    private abstract static class WeakFactory<T> {
        private WeakReference<T> myRef;

        
        protected abstract T create();

        
        public T getValue() {
            T cur = SoftReference.dereference(myRef);
            if (cur != null) {
                return cur;
            }
            T result = create();
            myRef = new WeakReference<>(result);
            return result;
        }
    }

    private static final ThreadLocal<WeakFactory<Map<PsiFile, ChunkExtractor>>> ourExtractors =
        new ThreadLocal<>() {
            @Override
            protected WeakFactory<Map<PsiFile, ChunkExtractor>> initialValue() {
                return new WeakFactory<>() {
                    
                    @Override
                    protected Map<PsiFile, ChunkExtractor> create() {
                        return FactoryMap.create(ChunkExtractor::new);
                    }
                };
            }
        };

    
    @RequiredReadAction
    public static TextChunk[] extractChunks(PsiFile file, UsageInfo2UsageAdapter usageAdapter) {
        return getExtractor(file).extractChunks(usageAdapter, file);
    }

    
    public static ChunkExtractor getExtractor(PsiFile file) {
        return ourExtractors.get().getValue().get(file);
    }

    private ChunkExtractor(PsiFile file) {
        myColorsScheme = UsageTreeColorsScheme.getInstance().getScheme();

        Project project = file.getProject();
        myDocument = PsiDocumentManager.getInstance(project).getDocument(file);
        LOG.assertTrue(myDocument != null);
        FileType fileType = file.getFileType();
        SyntaxHighlighter highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(fileType, project, file.getVirtualFile());
        highlighter = highlighter == null ? new DefaultSyntaxHighlighter() : highlighter;
        myHighlighter = new SyntaxHighlighterOverEditorHighlighter(highlighter, file.getVirtualFile(), project);
        myDocumentStamp = -1;
    }

    public static int getStartOffset(List<RangeMarker> rangeMarkers) {
        LOG.assertTrue(!rangeMarkers.isEmpty());
        int minStart = Integer.MAX_VALUE;
        for (RangeMarker rangeMarker : rangeMarkers) {
            if (!rangeMarker.isValid()) {
                continue;
            }
            int startOffset = rangeMarker.getStartOffset();
            if (startOffset < minStart) {
                minStart = startOffset;
            }
        }
        return minStart == Integer.MAX_VALUE ? -1 : minStart;
    }

    
    @RequiredReadAction
    private TextChunk[] extractChunks(UsageInfo2UsageAdapter usageInfo2UsageAdapter, PsiFile file) {
        int absoluteStartOffset = usageInfo2UsageAdapter.getNavigationOffset();
        if (absoluteStartOffset == -1) {
            return TextChunk.EMPTY_ARRAY;
        }

        Document visibleDocument = myDocument instanceof DocumentWindow docWindow ? docWindow.getDelegate() : myDocument;
        int visibleStartOffset =
            myDocument instanceof DocumentWindow docWindow ? docWindow.injectedToHost(absoluteStartOffset) : absoluteStartOffset;

        int lineNumber = myDocument.getLineNumber(absoluteStartOffset);
        int visibleLineNumber = visibleDocument.getLineNumber(visibleStartOffset);
        int visibleColumnNumber = visibleStartOffset - visibleDocument.getLineStartOffset(visibleLineNumber);
        List<TextChunk> result = new ArrayList<>();
        appendPrefix(result, visibleLineNumber, visibleColumnNumber);

        int fragmentToShowStart = myDocument.getLineStartOffset(lineNumber);
        int fragmentToShowEnd = fragmentToShowStart < myDocument.getTextLength() ? myDocument.getLineEndOffset(lineNumber) : 0;
        if (fragmentToShowStart > fragmentToShowEnd) {
            return TextChunk.EMPTY_ARRAY;
        }

        CharSequence chars = myDocument.getCharsSequence();
        if (fragmentToShowEnd - fragmentToShowStart > MAX_LINE_LENGTH_TO_SHOW) {
            int lineStartOffset = fragmentToShowStart;
            fragmentToShowStart = Math.max(lineStartOffset, absoluteStartOffset - OFFSET_BEFORE_TO_SHOW_WHEN_LONG_LINE);

            int lineEndOffset = fragmentToShowEnd;
            Segment segment = usageInfo2UsageAdapter.getUsageInfo().getSegment();
            int usage_length = segment != null ? segment.getEndOffset() - segment.getStartOffset() : 0;
            fragmentToShowEnd = Math.min(lineEndOffset, absoluteStartOffset + usage_length + OFFSET_AFTER_TO_SHOW_WHEN_LONG_LINE);

            // if we search something like a word, then expand shown context from one symbol before / after at least for word boundary
            // this should not cause restarts of the lexer as the tokens are usually words
            if (usage_length > 0 && StringUtil.isJavaIdentifierStart(chars.charAt(absoluteStartOffset)) && StringUtil.isJavaIdentifierStart(
                chars.charAt(absoluteStartOffset + usage_length - 1))) {
                while (fragmentToShowEnd < lineEndOffset && StringUtil.isJavaIdentifierStart(chars.charAt(fragmentToShowEnd - 1)))
                    ++fragmentToShowEnd;
                while (fragmentToShowStart > lineStartOffset && StringUtil.isJavaIdentifierStart(chars.charAt(fragmentToShowStart)))
                    --fragmentToShowStart;
                if (fragmentToShowStart != lineStartOffset) {
                    ++fragmentToShowStart;
                }
                if (fragmentToShowEnd != lineEndOffset) {
                    --fragmentToShowEnd;
                }
            }
        }
        if (myDocument instanceof DocumentWindow) {
            List<TextRange> editable = InjectedLanguageManager.getInstance(file.getProject())
                .intersectWithAllEditableFragments(file, new TextRange(fragmentToShowStart, fragmentToShowEnd));
            for (TextRange range : editable) {
                createTextChunks(usageInfo2UsageAdapter, chars, range.getStartOffset(), range.getEndOffset(), true, result);
            }
            return result.toArray(new TextChunk[result.size()]);
        }
        return createTextChunks(usageInfo2UsageAdapter, chars, fragmentToShowStart, fragmentToShowEnd, true, result);
    }

    
    @RequiredReadAction
    public TextChunk[] createTextChunks(
        UsageInfo2UsageAdapter usageInfo2UsageAdapter,
        CharSequence chars,
        int start,
        int end,
        boolean selectUsageWithBold,
        List<TextChunk> result
    ) {
        Lexer lexer = myHighlighter.getHighlightingLexer();
        SyntaxHighlighterOverEditorHighlighter highlighter = myHighlighter;

        LOG.assertTrue(start <= end);

        int i = StringUtil.indexOf(chars, '\n', start, end);
        if (i != -1) {
            end = i;
        }

        if (myDocumentStamp != myDocument.getModificationStamp()) {
            highlighter.restart(chars);
            myDocumentStamp = myDocument.getModificationStamp();
        }
        else if (lexer.getTokenType() == null || lexer.getTokenStart() > start) {
            highlighter.resetPosition(0);  // todo restart from nearest position with initial state
        }

        boolean isBeginning = true;

        for (; lexer.getTokenType() != null; lexer.advance()) {
            int hiStart = lexer.getTokenStart();
            int hiEnd = lexer.getTokenEnd();

            if (hiStart >= end) {
                break;
            }

            hiStart = Math.max(hiStart, start);
            hiEnd = Math.min(hiEnd, end);
            if (hiStart >= hiEnd) {
                continue;
            }

            if (isBeginning) {
                String text = chars.subSequence(hiStart, hiEnd).toString();
                if (text.trim().isEmpty()) {
                    continue;
                }
            }
            isBeginning = false;
            IElementType tokenType = lexer.getTokenType();
            TextAttributesKey[] tokenHighlights = highlighter.getTokenHighlights(tokenType);

            processIntersectingRange(usageInfo2UsageAdapter, chars, hiStart, hiEnd, tokenHighlights, selectUsageWithBold, result);
        }

        return result.toArray(new TextChunk[result.size()]);
    }

    @RequiredReadAction
    private void processIntersectingRange(
        UsageInfo2UsageAdapter usageInfo2UsageAdapter,
        CharSequence chars,
        int hiStart,
        int hiEnd,
        TextAttributesKey[] tokenHighlights,
        boolean selectUsageWithBold,
        List<TextChunk> result
    ) {
        TextAttributes originalAttrs = convertAttributes(tokenHighlights);
        if (selectUsageWithBold) {
            originalAttrs.setFontType(Font.PLAIN);
        }

        int[] lastOffset = {hiStart};
        usageInfo2UsageAdapter.processRangeMarkers(segment -> {
            int usageStart = segment.getStartOffset();
            int usageEnd = segment.getEndOffset();
            if (rangeIntersect(lastOffset[0], hiEnd, usageStart, usageEnd)) {
                addChunk(chars, lastOffset[0], Math.max(lastOffset[0], usageStart), originalAttrs, false, null, result);

                UsageType usageType = isHighlightedAsString(tokenHighlights)
                    ? UsageType.LITERAL_USAGE
                    : isHighlightedAsComment(tokenHighlights)
                    ? UsageType.COMMENT_USAGE
                    : null;
                addChunk(
                    chars,
                    Math.max(lastOffset[0], usageStart),
                    Math.min(hiEnd, usageEnd),
                    originalAttrs,
                    selectUsageWithBold,
                    usageType,
                    result
                );
                lastOffset[0] = usageEnd;
                if (usageEnd > hiEnd) {
                    return false;
                }
            }
            return true;
        });
        if (lastOffset[0] < hiEnd) {
            addChunk(chars, lastOffset[0], hiEnd, originalAttrs, false, null, result);
        }
    }

    public static boolean isHighlightedAsComment(TextAttributesKey... keys) {
        for (TextAttributesKey key : keys) {
            if (key == DefaultLanguageHighlighterColors.DOC_COMMENT
                || key == DefaultLanguageHighlighterColors.LINE_COMMENT
                || key == DefaultLanguageHighlighterColors.BLOCK_COMMENT) {
                return true;
            }
            if (key == null) {
                continue;
            }
            TextAttributesKey fallbackAttributeKey = key.getFallbackAttributeKey();
            if (fallbackAttributeKey != null && isHighlightedAsComment(fallbackAttributeKey)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHighlightedAsString(TextAttributesKey... keys) {
        for (TextAttributesKey key : keys) {
            if (key == DefaultLanguageHighlighterColors.STRING) {
                return true;
            }
            if (key == null) {
                continue;
            }
            TextAttributesKey fallbackAttributeKey = key.getFallbackAttributeKey();
            if (fallbackAttributeKey != null && isHighlightedAsString(fallbackAttributeKey)) {
                return true;
            }
        }
        return false;
    }

    private static void addChunk(
        CharSequence chars,
        int start,
        int end,
        TextAttributes originalAttrs,
        boolean bold,
        @Nullable UsageType usageType,
        List<TextChunk> result
    ) {
        if (start >= end) {
            return;
        }

        TextAttributes attrs = bold
            ? TextAttributes.merge(originalAttrs, new TextAttributes(null, null, null, null, Font.BOLD))
            : originalAttrs;
        result.add(new TextChunk(attrs, new String(CharArrayUtil.fromSequence(chars, start, end)), usageType));
    }

    private static boolean rangeIntersect(int s1, int e1, int s2, int e2) {
        return s2 < s1 && s1 < e2 || s2 < e1 && e1 < e2 || s1 < s2 && s2 < e1 || s1 < e2 && e2 < e1 || s1 == s2 && e1 == e2;
    }

    
    private TextAttributes convertAttributes(TextAttributesKey[] keys) {
        TextAttributes attrs = myColorsScheme.getAttributes(HighlighterColors.TEXT);

        for (TextAttributesKey key : keys) {
            TextAttributes attrs2 = myColorsScheme.getAttributes(key);
            if (attrs2 != null) {
                attrs = TextAttributes.merge(attrs, attrs2);
            }
        }

        attrs = attrs.clone();
        return attrs;
    }

    private void appendPrefix(List<TextChunk> result, int lineNumber, int columnNumber) {
        String prefix = "(" + (lineNumber + 1) + ": " + (columnNumber + 1) + ") ";
        TextChunk prefixChunk = new TextChunk(myColorsScheme.getAttributes(UsageTreeColors.USAGE_LOCATION), prefix);
        result.add(prefixChunk);
    }
}
