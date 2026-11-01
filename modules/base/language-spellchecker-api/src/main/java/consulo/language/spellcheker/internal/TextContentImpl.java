/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.spellcheker.internal;

import consulo.language.spellcheker.text.TextContent;

import consulo.application.progress.ProgressManager;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.spellcheker.text.util.StrategyUtils;
import consulo.language.spellcheker.text.util.Text;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static consulo.language.spellcheker.text.util.HighlightingUtil.isSpace;

/**
 * Internal implementation of {@link TextContent}, do not use directly.
 */
public final class TextContentImpl extends UserDataHolderBase implements TextContent {
    private final TextDomain myDomain;
    public final List<TokenInfo> tokens;
    private volatile @Nullable String myText;
    private volatile int @Nullable [] myTokenOffsets;
    private volatile @Nullable Integer myHash;

    public TextContentImpl(TextDomain domain, List<TokenInfo> _tokens) {
        myDomain = domain;
        tokens = new ArrayList<>(_tokens.size());
        if (_tokens.isEmpty()) {
            throw new IllegalArgumentException("No tokens");
        }
        for (TokenInfo token : _tokens) {
            if (!tokens.isEmpty()) {
                TokenInfo merged = merge(tokens.getLast(), token);
                if (merged != null) {
                    tokens.set(tokens.size() - 1, merged);
                    continue;
                }
            }
            tokens.add(token);
        }
        if (tokens.getFirst() instanceof WSTokenInfo) {
            tokens.removeFirst();
        }
        if (tokens.getLast() instanceof WSTokenInfo) {
            tokens.removeLast();
        }
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("There should be at least one non-whitespace token");
        }

        PsiFile file = ((PsiToken) tokens.getFirst()).psi.getContainingFile();
        if (ContainerUtil.exists(tokens, t -> t instanceof PsiToken pt && pt.psi.getContainingFile() != file)) {
            throw new IllegalArgumentException("TextContent fragments should be from the same file");
        }

        List<TextRange> ranges = getRangesInFile();
        if (!ContainerUtil.sorted(ranges, Segment.BY_START_OFFSET_THEN_END_OFFSET).equals(ranges)) {
            throw new IllegalArgumentException("TextContent fragments should be ordered by the offset ascending: " + ranges);
        }
    }

    private int findTokenIndex(int textOffset, int[] tokenOffsets, boolean leanForward) {
        if (textOffset < 0 || textOffset > length()) {
            throw new IllegalArgumentException("Text offset " + textOffset + " should be between 0 and " + length());
        }
        int index = Arrays.binarySearch(tokenOffsets, textOffset);
        if (index < 0) {
            return -index - 2;
        }

        if (leanForward) {
            while (index < tokens.size() - 1 && tokens.get(index).length() == 0) {
                index++;
            }
            if (tokens.get(index) instanceof WSTokenInfo) {
                index--;
            }
        }
        else {
            if (index > 0 && !(tokens.get(index - 1) instanceof WSTokenInfo)) {
                index--;
            }
        }

        return index;
    }

    private int findPsiTokenIndex(int fileOffset) {
        return ObjectUtil.binarySearch(0, tokens.size(), mid -> {
            int psiTokenIndex = mid;
            while (!(tokens.get(psiTokenIndex) instanceof PsiToken)) {
                psiTokenIndex--;
            }
            TextRange tokenRange = ((PsiToken) tokens.get(psiTokenIndex)).rangeInFile;
            if (tokenRange.containsOffset(fileOffset)) {
                return mid == psiTokenIndex ? 0 : 1;
            }
            return tokenRange.getEndOffset() < fileOffset ? -1 : 1;
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextContentImpl that)) {
            return false;
        }
        return myDomain == that.myDomain && tokens.equals(that.tokens);
    }

    @Override
    public int hashCode() {
        Integer hash = myHash;
        if (hash == null) {
            myHash = hash = Objects.hash(myDomain, tokens);
        }
        return hash;
    }

    @Override
    public TextDomain getDomain() {
        return myDomain;
    }

    @Override
    public String toString() {
        String text = myText;
        if (text == null) {
            myText = text = StringUtil.join(tokens, t -> t.text, "");
        }
        return text;
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public int textOffsetToFile(int textOffset) {
        String text = toString();
        char prev = textOffset <= 0 ? ' ' : text.charAt(textOffset - 1);
        char next = textOffset >= text.length() ? ' ' : text.charAt(textOffset);
        boolean leanForward = (Character.isWhitespace(prev) || !Character.isWhitespace(next)) &&
            (!Character.isLetterOrDigit(prev) || Character.isLetterOrDigit(next));
        return textOffsetToFile(textOffset, leanForward);
    }

    @Override
    public int textOffsetToFile(int textOffset, boolean leanForward) {
        int[] offsets = getTokenOffsets();
        int tokenIndex = findTokenIndex(textOffset, offsets, leanForward);
        return ((PsiToken) tokens.get(tokenIndex)).psiStart() + (textOffset - offsets[tokenIndex]);
    }

    @Override
    public @Nullable Integer fileOffsetToText(int fileOffset) {
        int index = findPsiTokenIndex(fileOffset);
        return index < 0 ? null : getTokenOffsets()[index] + fileOffset - ((PsiToken) tokens.get(index)).rangeInFile.getStartOffset();
    }

    @Override
    public @Nullable TextRange fileRangeToText(TextRange fileRange) {
        Integer start = fileOffsetToText(fileRange.getStartOffset());
        Integer end = fileOffsetToText(fileRange.getEndOffset());
        return start == null || end == null ? null : new TextRange(start, end);
    }

    @Override
    public PsiElement getCommonParent() {
        return Objects.requireNonNull(
            PsiTreeUtil.findCommonParent(
                new ArrayList<>(ContainerUtil.map2SetNotNull(tokens, t -> t instanceof PsiToken ? ((PsiToken) t).psi : null))
            )
        );
    }

    @Override
    public PsiElement findPsiElementAt(int textOffset) {
        int fileOffset = textOffsetToFile(textOffset);
        PsiFile file = getContainingFile();
        PsiElement leaf = file.findElementAt(fileOffset);
        if (leaf == null) {
            if (fileOffset == file.getTextLength()) {
                return PsiTreeUtil.getDeepestLast(file);
            }
            throw new RuntimeException("Cannot find offset " + fileOffset + " in file of " + file.getClass() + ", length " + file.getTextLength());
        }
        return leaf;
    }

    @Override
    public List<TextRange> getRangesInFile() {
        List<TextRange> result = new ArrayList<>();
        for (TokenInfo token : tokens) {
            if (token instanceof PsiToken psiToken && !psiToken.rangeInFile.isEmpty()) {
                result.add(psiToken.rangeInFile);
            }
        }
        return result;
    }

    @Override
    public PsiFile getContainingFile() {
        for (TokenInfo token : tokens) {
            if (token instanceof PsiToken) {
                return ((PsiToken) token).psi.getContainingFile();
            }
        }
        throw new IllegalStateException("No PSI tokens");
    }

    @Override
    public boolean hasUnknownFragmentsIn(TextRange rangeInText) {
        int[] offsets = getTokenOffsets();
        int start = findTokenIndex(rangeInText.getStartOffset(), offsets, false);
        int end = findTokenIndex(rangeInText.getEndOffset(), offsets, true);
        for (int i = start; i <= end; i++) {
            TokenInfo token = tokens.get(i);
            if (token instanceof PsiToken && ((PsiToken) token).kind == TokenKind.unknown && rangeInText.containsOffset(offsets[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TextContent excludeRange(TextRange rangeInText) {
        return rangeInText.getLength() == 0 ? this : excludeRanges(List.of(Exclusion.exclude(rangeInText)));
    }

    @Override
    public TextContent markUnknown(TextRange rangeInText) {
        return excludeRanges(List.of(Exclusion.markUnknown(rangeInText)));
    }

    @Override
    public boolean intersectsRange(TextRange rangeInFile) {
        int start = findPsiTokenIndex(rangeInFile.getStartOffset());
        if (start < 0) {
            start = -start - 1;
        }
        for (int i = start; i < tokens.size(); i++) {
            TokenInfo token = tokens.get(i);
            if (!(token instanceof PsiToken)) {
                continue;
            }

            TextRange tokenRange = ((PsiToken) token).rangeInFile;
            if (tokenRange.intersectsStrict(rangeInFile)) {
                return true;
            }
            if (tokenRange.getStartOffset() >= rangeInFile.getEndOffset()) {
                break;
            }
        }
        return false;
    }

    @Override
    public TextContent excludeRanges(List<Exclusion> ranges) {
        ProgressManager.checkCanceled();
        if (ranges.isEmpty()) {
            return this;
        }

        if (ranges.getFirst().start < 0 || ranges.getLast().end > length()) {
            throw new IllegalArgumentException("Text ranges " + ranges + " should be between 0 and " + length());
        }
        for (int i = 1; i < ranges.size(); i++) {
            if (ranges.get(i - 1).end > ranges.get(i).start) {
                throw new IllegalArgumentException("Ranges should be sorted and non-intersecting: " + ranges);
            }
        }

        int[] offsets = getTokenOffsets();
        List<Exclusion>[] affectingExclusions = getAffectingExclusions(ranges, offsets);

        List<TokenInfo> newTokens = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            List<Exclusion> affecting = affectingExclusions[i];
            TokenInfo token = tokens.get(i);
            if (affecting == null) {
                newTokens.add(token);
            }
            else if (token instanceof PsiToken) {
                newTokens.addAll(((PsiToken) token).splitToken(offsets[i], affecting));
            }
        }

        if (newTokens.isEmpty()) {
            PsiToken first = (PsiToken) tokens.getFirst();
            TokenKind kind1 = first.kind;
            TokenKind kind2 = ((PsiToken) tokens.getLast()).kind;
            TokenKind mostUnknown = kind1.compareTo(kind2) > 0 ? kind1 : kind2;
            newTokens.add(new PsiToken("", first.psi, TextRange.from(first.rangeInPsi.getStartOffset(), 0), mostUnknown));
        }

        return new TextContentImpl(myDomain, newTokens);
    }

    private @Nullable List<Exclusion>[] getAffectingExclusions(List<Exclusion> ranges, int[] offsets) {
        @SuppressWarnings("unchecked") List<Exclusion>[] affectingExclusions = new List[tokens.size()];

        for (Exclusion range : ranges) {
            boolean emptyRange = range.start == range.end;
            if (emptyRange && range.kind == ExclusionKind.exclude) {
                continue;
            }

            int i1 = findTokenIndex(range.start, offsets, true);
            int i2 = findTokenIndex(range.end, offsets, emptyRange);
            while (i2 > 0 && tokens.get(i2).length() == 0 && i2 > i1) {
                i2--;
            }

            for (int j = i1; j <= i2; j++) {
                List<Exclusion> affecting = affectingExclusions[j];
                if (affecting == null) {
                    affectingExclusions[j] = affecting = new ArrayList<>();
                }
                affecting.add(range);
            }
        }
        return affectingExclusions;
    }

    private int[] getTokenOffsets() {
        int[] offsets = myTokenOffsets;
        if (offsets == null) {
            myTokenOffsets = offsets = calcTokenOffsets();
        }
        return offsets;
    }

    private int[] calcTokenOffsets() {
        int[] offsets = new int[tokens.size()];
        int tokenStart = 0;
        for (int i = 0; i < tokens.size(); i++) {
            TokenInfo info = tokens.get(i);
            offsets[i] = tokenStart;
            tokenStart += info.length();
        }
        return offsets;
    }

    @Override
    public @Nullable TextContent trimWhitespace() {
        String text = toString();
        int start = 0;
        int end = text.length();
        while (start < end && isSpace(text.charAt(start))) {
            start++;
        }
        while (start < end && isSpace(text.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return null;
        }
        if (start > 0 || end < text.length()) {
            return excludeRange(new TextRange(end, text.length())).excludeRange(new TextRange(0, start));
        }
        return this;
    }

    @Override
    public TextContent removeIndents(Set<Character> indentChars) {
        Set<TextRange> ranges = StrategyUtils.indentIndexes(this, indentChars);
        List<Exclusion> exclusions = ranges.stream()
            .sorted(Comparator.comparingInt(TextRange::getStartOffset))
            .map(range -> {
                int end = range.getEndOffset();
                return new Exclusion(range.getStartOffset(), end, hasUnknownFragmentsIn(new TextRange(range.getStartOffset(), end)));
            })
            .toList();
        return excludeRanges(exclusions);
    }

    @Override
    public TextContent removeLineSuffixes(Set<Character> suffixChars) {
        if (suffixChars.isEmpty()) {
            return this;
        }

        Pattern pattern = Pattern.compile("(" + StringUtil.join(suffixChars, c -> Pattern.quote(String.valueOf(c)), "|") + ")(?=\n)");
        return excludeRanges(ContainerUtil.map(Text.allOccurrences(pattern, this), Exclusion::exclude));
    }

    @Override
    public int[] markupOffsets() {
        return excludedOffsets(TokenKind.markup);
    }

    @Override
    public int[] unknownOffsets() {
        return excludedOffsets(TokenKind.unknown);
    }

    private int[] excludedOffsets(TokenKind kind) {
        List<Integer> result = new ArrayList<>();
        int offset = 0;
        for (TokenInfo token : tokens) {
            if (token instanceof PsiToken pt && pt.kind == kind) {
                result.add(offset);
            }
            offset += token.length();
        }
        return result.stream().mapToInt(i -> i).toArray();
    }

    @Override
    public WithMarkup replaceMarkupWith(char c) {
        StringBuilder sb = new StringBuilder(toString());
        int[] offsets = markupOffsets();
        for (int i = offsets.length - 1; i >= 0; i--) {
            sb.insert(offsets[i], c);
        }
        return new WithMarkup() {
            @Override
            public int offsetToOriginal(int offsetWithMarkup) {
                int result = offsetWithMarkup;
                for (int offset : offsets) {
                    if (offset >= result) {
                        break;
                    }
                    result--;
                }
                return result;
            }

            @Override
            public int length() {
                return sb.length();
            }

            @Override
            public char charAt(int index) {
                return sb.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return sb.subSequence(start, end);
            }

            @Override
            public String toString() {
                return sb.toString();
            }
        };
    }

    private static @Nullable TokenInfo merge(TokenInfo t1, TokenInfo t2) {
        if (t1 instanceof WSTokenInfo && t2 instanceof WSTokenInfo) {
            return t1;
        }
        if (t1 instanceof PsiToken && t2 instanceof PsiToken) {
            return mergePsiTokens((PsiToken) t1, (PsiToken) t2);
        }
        return null;
    }

    private static @Nullable TokenInfo mergePsiTokens(PsiToken t1, PsiToken t2) {
        if (t1.kind != TokenKind.text && t2.kind != TokenKind.text) {
            return t1.kind.compareTo(t2.kind) > 0 ? t1 : t2;
        }
        if (t1.kind == TokenKind.text && t2.kind == TokenKind.text) {
            if (t1.length() == 0) {
                return t2;
            }
            if (t2.length() == 0) {
                return t1;
            }
            if (t1.psi == t2.psi && t1.rangeInPsi.getStartOffset() + t1.length() == t2.rangeInPsi.getStartOffset()) {
                return new PsiToken(t1.text + t2.text, t1.psi, t1.rangeInPsi.union(t2.rangeInPsi), TokenKind.text);
            }
        }
        return null;
    }

    public abstract static class TokenInfo {
        public final String text;

        TokenInfo(String text) {
            this.text = text;
        }

        public int length() {
            return text.length();
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public enum TokenKind {
        text,
        markup,
        unknown
    }

    public static final class PsiToken extends TokenInfo {
        public final PsiElement psi;
        public final TextRange rangeInPsi;
        public final TextRange rangeInFile;
        public final TokenKind kind;

        public PsiToken(String text, PsiElement psi, TextRange rangeInPsi, TokenKind kind) {
            super(text);
            this.psi = psi;
            this.rangeInPsi = rangeInPsi;
            this.rangeInFile = rangeInPsi.shiftRight(psi.getTextRange().getStartOffset());
            this.kind = kind;
            assert rangeInPsi.getLength() == text.length();
            assert kind == TokenKind.text || rangeInPsi.getLength() == 0;
        }

        private int psiStart() {
            return rangeInFile.getStartOffset();
        }

        private PsiToken withRange(TextRange range) {
            assert range.getLength() > 0;
            assert kind == TokenKind.text;
            return new PsiToken(range.shiftLeft(rangeInPsi.getStartOffset()).substring(text), psi, range, kind);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PsiToken psiToken)) {
                return false;
            }
            return kind == psiToken.kind
                && psi.equals(psiToken.psi)
                && (kind != TokenKind.text || (rangeInPsi.equals(psiToken.rangeInPsi) && rangeInFile.equals(psiToken.rangeInFile)))
                && text.equals(psiToken.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(psi, rangeInPsi, kind, text);
        }

        @Override
        public String toString() {
            return kind == TokenKind.markup ? "*" : kind == TokenKind.unknown ? "?" : super.toString();
        }

        private List<PsiToken> splitToken(int tokenStart, List<Exclusion> affecting) {
            int tokenEnd = tokenStart + length();
            if (affecting.size() == 1 && affecting.getFirst().start < tokenStart && affecting.getFirst().end >= tokenEnd) {
                return Collections.emptyList();
            }

            List<PsiToken> shreds = new ArrayList<>();
            int startInPsi = rangeInPsi.getStartOffset();
            int prevEnd = tokenStart;
            for (Exclusion range : affecting) {
                if (range.start > prevEnd) {
                    shreds.add(withRange(TextRange.from(startInPsi + prevEnd - tokenStart, range.start - prevEnd)));
                }
                if (range.kind != ExclusionKind.exclude) {
                    TokenKind tokenKind = range.kind == ExclusionKind.markup ? TextContentImpl.TokenKind.markup : TextContentImpl.TokenKind.unknown;
                    shreds.add(new PsiToken("", psi, TextRange.from(startInPsi + range.start - tokenStart, 0), tokenKind));
                }
                prevEnd = range.end;
            }
            Exclusion lastRange = affecting.getLast();
            if (tokenEnd > lastRange.end) {
                shreds.add(withRange(new TextRange(startInPsi + lastRange.end - tokenStart, startInPsi + length())));
            }
            return shreds;
        }
    }

    public static final class WSTokenInfo extends TokenInfo {
        public WSTokenInfo(char ws) {
            super(String.valueOf(ws));
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof WSTokenInfo && ((WSTokenInfo) obj).text.equals(text);
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }
    }
}
