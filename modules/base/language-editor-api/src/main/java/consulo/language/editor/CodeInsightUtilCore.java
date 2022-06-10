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
package com.intellij.codeInsight;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import consulo.annotation.access.RequiredReadAction;
import consulo.logging.Logger;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.reflect.ReflectionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;

public abstract class CodeInsightUtilCore extends FileModificationService {
  private static final Logger LOG = Logger.getInstance(CodeInsightUtilCore.class);

  @RequiredReadAction
  public static <T extends PsiElement> T findElementInRange(@Nonnull PsiFile file, int startOffset, int endOffset, @Nonnull Class<T> klass, @Nonnull Language language) {
    return findElementInRange(file, startOffset, endOffset, klass, language, null);
  }

  @RequiredReadAction
  private static <T extends PsiElement> T findElementInRange(@Nonnull PsiFile file,
                                                             int startOffset,
                                                             int endOffset,
                                                             @Nonnull Class<T> klass,
                                                             @Nonnull Language language,
                                                             @Nullable PsiElement initialElement) {
    PsiElement element1 = file.getViewProvider().findElementAt(startOffset, language);
    PsiElement element2 = file.getViewProvider().findElementAt(endOffset - 1, language);
    if (element1 instanceof PsiWhiteSpace) {
      startOffset = element1.getTextRange().getEndOffset();
      element1 = file.getViewProvider().findElementAt(startOffset, language);
    }
    if (element2 instanceof PsiWhiteSpace) {
      endOffset = element2.getTextRange().getStartOffset();
      element2 = file.getViewProvider().findElementAt(endOffset - 1, language);
    }
    if (element2 == null || element1 == null) return null;
    final PsiElement commonParent = PsiTreeUtil.findCommonParent(element1, element2);
    final T element = ReflectionUtil.isAssignable(klass, commonParent.getClass()) ? (T)commonParent : PsiTreeUtil.getParentOfType(commonParent, klass);

    if (element == initialElement) {
      return element;
    }

    if (element == null || element.getTextRange().getStartOffset() != startOffset || element.getTextRange().getEndOffset() != endOffset) {
      return null;
    }
    return element;
  }

  @RequiredReadAction
  public static <T extends PsiElement> T forcePsiPostprocessAndRestoreElement(@Nonnull T element) {
    return forcePsiPostprocessAndRestoreElement(element, false);
  }

  @RequiredReadAction
  public static <T extends PsiElement> T forcePsiPostprocessAndRestoreElement(@Nonnull T element, boolean useFileLanguage) {
    final PsiFile psiFile = element.getContainingFile();
    final Document document = psiFile.getViewProvider().getDocument();
    //if (document == null) return element;
    final Language language = useFileLanguage ? psiFile.getLanguage() : PsiUtilCore.getDialect(element);
    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(psiFile.getProject());
    final RangeMarker rangeMarker = document.createRangeMarker(element.getTextRange());
    documentManager.doPostponedOperationsAndUnblockDocument(document);
    documentManager.commitDocument(document);

    T elementInRange = findElementInRange(psiFile, rangeMarker.getStartOffset(), rangeMarker.getEndOffset(), (Class<? extends T>)element.getClass(), language, element);
    rangeMarker.dispose();
    return elementInRange;
  }

  private static final char[] QUOTES = {'"', '\''};

  public static boolean parseStringCharacters(@Nonnull String chars, @Nonnull StringBuilder outChars, @Nullable int[] sourceOffsets) {
    return parseStringCharacters(chars, outChars, sourceOffsets, true, true, QUOTES);
  }

  public static boolean parseStringCharacters(@Nonnull String chars,
                                              @Nonnull StringBuilder outChars,
                                              @Nullable int[] sourceOffsets,
                                              boolean slashMustBeEscaped,
                                              boolean exitOnEscapingWrongSymbol,
                                              @Nonnull char... endChars) {
    LOG.assertTrue(sourceOffsets == null || sourceOffsets.length == chars.length() + 1);
    if (StringParser.noEscape(chars, sourceOffsets)) {
      outChars.append(chars);
      return true;
    }
    return StringParser.parseStringCharactersWithEscape(chars, outChars, sourceOffsets, slashMustBeEscaped, exitOnEscapingWrongSymbol, endChars);
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
  @Nullable
  public static CharSequence parseStringCharacters(@Nonnull String chars, @Nullable int[] sourceOffsets) {
    LOG.assertTrue(sourceOffsets == null || sourceOffsets.length == chars.length() + 1);
    if (StringParser.noEscape(chars, sourceOffsets)) {
      return chars;
    }
    StringBuilder outChars = new StringBuilder(chars.length());
    return StringParser.parseStringCharactersWithEscape(chars, outChars, sourceOffsets, true, true, QUOTES) ? outChars : null;
  }

  private static final class StringParser {
    private StringParser() {
    }

    private static boolean noEscape(@Nonnull String chars, @Nullable int[] sourceOffsets) {
      if (chars.indexOf('\\') < 0) {
        if (sourceOffsets != null) Arrays.setAll(sourceOffsets, IntUnaryOperator.identity());
        return true;
      }
      return false;
    }

    static boolean parseStringCharactersWithEscape(@Nonnull String chars,
                                                   @Nonnull StringBuilder outChars,
                                                   @Nullable int[] sourceOffsets,
                                                   boolean slashMustBeEscaped,
                                                   boolean exitOnEscapingWrongSymbol,
                                                   @Nonnull char[] endChars) {
      int index = 0;
      final int outOffset = outChars.length();
      while (index < chars.length()) {
        char c = chars.charAt(index++);
        if (sourceOffsets != null) {
          sourceOffsets[outChars.length() - outOffset] = index - 1;
          sourceOffsets[outChars.length() + 1 - outOffset] = index;
        }
        if (c != '\\') {
          outChars.append(c);
          continue;
        }
        index = parseEscapedSymbol(sourceOffsets, slashMustBeEscaped, exitOnEscapingWrongSymbol, endChars, chars, outChars, index, outOffset, false);
        if (index == -1) return false;
        if (sourceOffsets != null) {
          sourceOffsets[outChars.length() - outOffset] = index;
        }
      }
      return true;
    }

    private static int parseEscapedSymbol(@Nullable int[] sourceOffsets,
                                          boolean slashMustBeEscaped,
                                          boolean exitOnEscapingWrongSymbol,
                                          @Nonnull char[] endChars,
                                          @Nonnull String chars,
                                          @Nonnull StringBuilder outChars,
                                          int index,
                                          int outOffset,
                                          boolean isAfterEscapedBackslash) {
      if (index == chars.length()) return -1;
      char c = chars.charAt(index++);
      if (parseEscapedChar(c, outChars)) {
        return index;
      }
      switch (c) {
        case '\\':
          boolean isUnicodeSequenceStart = isAfterEscapedBackslash && index < chars.length() && chars.charAt(index) == 'u';
          if (isUnicodeSequenceStart) {
            index = parseUnicodeEscape(sourceOffsets, slashMustBeEscaped, exitOnEscapingWrongSymbol, endChars, chars, outChars, index, outOffset, true);
          }
          else {
            outChars.append('\\');
          }
          break;

        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
          index = parseOctalEscape(chars, outChars, c, index);
          break;

        case 'u':
          if (isAfterEscapedBackslash) {
            if (!handleUnexpectedChar(sourceOffsets, slashMustBeEscaped, exitOnEscapingWrongSymbol, endChars, outChars, index - 1, outOffset, c)) return -1;
          }
          else {
            index = parseUnicodeEscape(sourceOffsets, slashMustBeEscaped, exitOnEscapingWrongSymbol, endChars, chars, outChars, index - 1, outOffset, false);
          }
          break;

        default:
          if (!handleUnexpectedChar(sourceOffsets, slashMustBeEscaped, exitOnEscapingWrongSymbol, endChars, outChars, index - 1, outOffset, c)) return -1;
      }
      return index;
    }

    private static int parseUnicodeEscape(@Nullable int[] sourceOffsets,
                                          boolean slashMustBeEscaped,
                                          boolean exitOnEscapingWrongSymbol,
                                          @Nonnull char[] endChars,
                                          @Nonnull String s,
                                          @Nonnull StringBuilder outChars,
                                          int index,
                                          int outOffset,
                                          boolean isAfterEscapedBackslash) {
      int len = s.length();
      int start = index - 1;
      // uuuuu1234 is valid too
      do {
        index++;
      }
      while (index < len && s.charAt(index) == 'u');
      if (index + 4 > len) return -1;
      try {
        char c = s.charAt(index);
        if (c == '+' || c == '-') return -1;
        int code = Integer.parseInt(s.substring(index, index + 4), 16);
        // line separators are invalid here
        if (code == 0x000a || code == 0x000d) return -1;
        char escapedChar = (char)code;
        if (escapedChar == '\\') {
          if (isAfterEscapedBackslash) {
            // \u005c\u005c
            outChars.append('\\');
            return index + 4;
          }
          else {
            // u005cxyz
            return parseEscapedSymbol(sourceOffsets, slashMustBeEscaped, exitOnEscapingWrongSymbol, endChars, s, outChars, index + 4, outOffset, true);
          }
        }
        if (isAfterEscapedBackslash) {
          // e.g. \u005c\u006e is converted to newline
          if (parseEscapedChar(escapedChar, outChars)) return index + 4;
          if (handleUnexpectedChar(sourceOffsets, slashMustBeEscaped, exitOnEscapingWrongSymbol, endChars, outChars, start, outOffset, escapedChar)) return index + 4;
          return -1;
        }
        // just single unicode escape sequence
        outChars.append(escapedChar);
        return index + 4;
      }
      catch (NumberFormatException ignored) {
        return -1;
      }
    }

    private static boolean handleUnexpectedChar(@Nullable int[] sourceOffsets,
                                                boolean slashMustBeEscaped,
                                                boolean exitOnEscapingWrongSymbol,
                                                @Nonnull char[] endChars,
                                                @Nonnull StringBuilder outChars,
                                                int start,
                                                int outOffset,
                                                char c) {
      if (CharArrayUtil.indexOf(endChars, c, 0, endChars.length) != -1) {
        outChars.append(c);
      }
      else if (!exitOnEscapingWrongSymbol) {
        if (!slashMustBeEscaped) {
          outChars.append('\\');
          if (sourceOffsets != null) {
            sourceOffsets[outChars.length() - outOffset] = start;
          }
        }
        outChars.append(c);
      }
      else {
        return false;
      }
      return true;
    }

    private static boolean parseEscapedChar(char c, @Nonnull StringBuilder outChars) {
      switch (c) {
        case 'b':
          outChars.append('\b');
          return true;

        case 't':
          outChars.append('\t');
          return true;

        case 'n':
          outChars.append('\n');
          return true;

        case 'f':
          outChars.append('\f');
          return true;

        case 'r':
          outChars.append('\r');
          return true;

        case 's':
          outChars.append(' ');
          return true;

        case '\n':
          return true;
      }
      return false;
    }

    private static int parseOctalEscape(@Nonnull String s, @Nonnull StringBuilder outChars, char c, int index) {
      char startC = c;
      int v = (int)c - '0';
      if (index < s.length()) {
        c = s.charAt(index++);
        if ('0' <= c && c <= '7') {
          v <<= 3;
          v += c - '0';
          if (startC <= '3' && index < s.length()) {
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
      outChars.append((char)v);
      return index;
    }
  }
}
