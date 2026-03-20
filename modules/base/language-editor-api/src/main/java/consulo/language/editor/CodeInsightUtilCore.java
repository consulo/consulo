/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.language.Language;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

public abstract class CodeInsightUtilCore extends FileModificationService {
    private static final Logger LOG = Logger.getInstance(CodeInsightUtilCore.class);

    @RequiredReadAction
    public static <T extends PsiElement> T findElementInRange(
        PsiFile file,
        int startOffset,
        int endOffset,
        Class<T> clazz,
        Language language
    ) {
        return findElementInRange(file, startOffset, endOffset, clazz, language, null);
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    private static <T extends PsiElement> @Nullable T findElementInRange(
        PsiFile file,
        int startOffset,
        int endOffset,
        Class<T> clazz,
        Language language,
        @Nullable PsiElement initialElement
    ) {
        PsiElement element1 = file.getViewProvider().findElementAt(startOffset, language);
        PsiElement element2 = file.getViewProvider().findElementAt(endOffset - 1, language);
        if (element1 instanceof PsiWhiteSpace ws1) {
            startOffset = ws1.getTextRange().getEndOffset();
            element1 = file.getViewProvider().findElementAt(startOffset, language);
        }
        if (element2 instanceof PsiWhiteSpace ws2) {
            endOffset = ws2.getTextRange().getStartOffset();
            element2 = file.getViewProvider().findElementAt(endOffset - 1, language);
        }
        if (element2 == null || element1 == null) {
            return null;
        }
        PsiElement commonParent = PsiTreeUtil.findCommonParent(element1, element2);
        T element = ReflectionUtil.isAssignable(clazz, commonParent.getClass())
            ? (T) commonParent
            : PsiTreeUtil.getParentOfType(commonParent, clazz);

        if (element == initialElement) {
            return element;
        }

        if (element == null
            || element.getTextRange().getStartOffset() != startOffset
            || element.getTextRange().getEndOffset() != endOffset) {
            return null;
        }
        return element;
    }

    @RequiredReadAction
    public static <T extends PsiElement> @Nullable T forcePsiPostprocessAndRestoreElement(T element) {
        return forcePsiPostprocessAndRestoreElement(element, false);
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> @Nullable T forcePsiPostprocessAndRestoreElement(T element, boolean useFileLanguage) {
        PsiFile psiFile = element.getContainingFile();
        Document document = psiFile.getViewProvider().getDocument();
        //if (document == null) return element;
        Language language = useFileLanguage ? psiFile.getLanguage() : PsiUtilCore.getDialect(element);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(psiFile.getProject());
        RangeMarker rangeMarker = document.createRangeMarker(element.getTextRange());
        documentManager.doPostponedOperationsAndUnblockDocument(document);
        documentManager.commitDocument(document);

        T elementInRange = findElementInRange(
            psiFile,
            rangeMarker.getStartOffset(),
            rangeMarker.getEndOffset(),
            (Class<? extends T>) element.getClass(),
            language,
            element
        );
        rangeMarker.dispose();
        return elementInRange;
    }

    public static boolean parseStringCharacters(String chars, StringBuilder out, @Nullable int[] sourceOffsets) {
        return parseStringCharacters(chars, out, sourceOffsets, true);
    }

    public static boolean parseStringCharacters(
        String chars,
        StringBuilder out,
        @Nullable int[] sourceOffsets,
        boolean textBlock
    ) {
        LOG.assertTrue(sourceOffsets == null || sourceOffsets.length == chars.length() + 1);
        if (noEscape(chars, sourceOffsets)) {
            out.append(chars);
            return true;
        }
        return parseStringCharactersWithEscape(chars, textBlock, out, sourceOffsets);
    }

    /**
     * Parse Java String literal and get the result, if literal is valid
     *
     * @param chars         String literal source
     * @param sourceOffsets optional output parameter:
     *                      sourceOffsets[i in returnValue.indices] represents
     *                      the index of returnValue[i] character in the source literal
     * @return String literal value, or null, if the literal is invalid
     */
    public static @Nullable CharSequence parseStringCharacters(String chars, @Nullable int[] sourceOffsets) {
        LOG.assertTrue(sourceOffsets == null || sourceOffsets.length == chars.length() + 1);
        if (noEscape(chars, sourceOffsets)) {
            return chars;
        }
        StringBuilder out = new StringBuilder(chars.length());
        return parseStringCharactersWithEscape(chars, true, out, sourceOffsets) ? out : null;
    }

    private static boolean noEscape(String chars, @Nullable int[] sourceOffsets) {
        if (chars.indexOf('\\') >= 0) {
            return false;
        }
        if (sourceOffsets != null) {
            Arrays.setAll(sourceOffsets, IntUnaryOperator.identity());
        }
        return true;
    }

    static boolean parseStringCharactersWithEscape(
        String chars,
        boolean textBlock,
        StringBuilder out,
        @Nullable int[] sourceOffsets
    ) {
        int index = 0;
        int outOffset = out.length();
        while (index < chars.length()) {
            char c = chars.charAt(index++);
            if (sourceOffsets != null) {
                sourceOffsets[out.length() - outOffset] = index - 1;
                sourceOffsets[out.length() + 1 - outOffset] = index;
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            index = parseEscapedSymbol(false, chars, index, textBlock, out);
            if (index == -1) {
                return false;
            }
            if (sourceOffsets != null) {
                sourceOffsets[out.length() - outOffset] = index;
            }
        }
        return true;
    }

    private static int parseEscapedSymbol(
        boolean isAfterEscapedBackslash,
        String chars,
        int index,
        boolean textBlock,
        StringBuilder out
    ) {
        if (index == chars.length()) {
            return -1;
        }
        char c = chars.charAt(index++);
        if (parseEscapedChar(c, textBlock, out)) {
            return index;
        }
        switch (c) {
            case '\\' -> {
                boolean isUnicodeSequenceStart = isAfterEscapedBackslash && index < chars.length() && chars.charAt(index) == 'u';
                if (isUnicodeSequenceStart) {
                    index = parseUnicodeEscape(true, chars, index, textBlock, out);
                }
                else {
                    out.append('\\');
                }
            }

            case '0', '1', '2', '3', '4', '5', '6', '7' -> {
                index = parseOctalEscape(c, chars, index, out);
            }

            case 'u' -> {
                if (isAfterEscapedBackslash) {
                    return -1;
                }
                index = parseUnicodeEscape(false, chars, index - 1, textBlock, out);
            }

            default -> {
                return -1;
            }
        }
        return index;
    }

    private static int parseUnicodeEscape(
        boolean isAfterEscapedBackslash,
        String s,
        int index,
        boolean textBlock,
        StringBuilder out
    ) {
        int len = s.length();
        // uuuuu1234 is valid too
        do {
            index++;
        }
        while (index < len && s.charAt(index) == 'u');

        if (index + 4 > len) {
            return -1;
        }

        try {
            char c = s.charAt(index);
            if (c == '+' || c == '-') {
                return -1;
            }
            int code = Integer.parseInt(s.substring(index, index + 4), 16);
            // unicode escaped line separators are invalid here when not a text block
            if (!textBlock && (code == 0x000A || code == 0x000D)) {
                return -1;
            }
            char escapedChar = (char) code;
            if (escapedChar == '\\') {
                if (isAfterEscapedBackslash) {
                    // \u005c\u005c
                    out.append('\\');
                    return index + 4;
                }
                else {
                    // u005cxyz
                    return parseEscapedSymbol(true, s, index + 4, textBlock, out);
                }
            }
            if (isAfterEscapedBackslash) {
                // e.g. \u005c\u006e is converted to newline
                if (parseEscapedChar(escapedChar, textBlock, out)) {
                    return index + 4;
                }
                return -1;
            }
            // just single unicode escape sequence
            out.append(escapedChar);
            return index + 4;
        }
        catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean parseEscapedChar(char c, boolean textBlock, StringBuilder out) {
        return switch (c) {
            case 'b' -> {
                out.append('\b');
                yield true;
            }
            case 'f' -> {
                out.append('\f');
                yield true;
            }
            case 'n' -> {
                out.append('\n');
                yield true;
            }
            case 'r' -> {
                out.append('\r');
                yield true;
            }
            case 's' -> {
                out.append(' ');
                yield true;
            }
            case 't' -> {
                out.append('\t');
                yield true;
            }
            case '\'' -> {
                out.append('\'');
                yield true;
            }
            case '\"' -> {
                out.append('"');
                yield true;
            }
            case '\n' -> textBlock; // escaped newline only valid inside text block
            default -> false;
        };
    }

    private static int parseOctalEscape(char c, String s, int index, StringBuilder out) {
        char startC = c;
        int v = c - '0', len = s.length();
        if (index < len) {
            c = s.charAt(index++);
            if ('0' <= c && c <= '7') {
                v <<= 3;
                v += c - '0';
                if (startC <= '3' && index < len) {
                    c = s.charAt(index++);
                    if ('0' <= c && c <= '7') {
                        v <<= 3;
                        v += c - '0';
                    }
                    else {
                        index--;
                    }
                }
            }
            else {
                index--;
            }
        }
        out.append((char) v);
        return index;
    }
}
