/*
 * Copyright 2013-2022 consulo.io
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
package consulo.codeEditor.impl.util;

import consulo.application.util.Dumpable;
import consulo.application.util.logging.LoggerUtil;
import consulo.codeEditor.*;
import consulo.codeEditor.impl.ComplementaryFontsRegistry;
import consulo.codeEditor.impl.FontInfo;
import consulo.codeEditor.RealEditor;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static consulo.codeEditor.util.EditorUtil.getTabSize;

/**
 * @author VISTALL
 * @since 20-Mar-22
 */
public class EditorImplUtil {
  private static final Logger LOG = Logger.getInstance(EditorImplUtil.class);

  public static int getLastVisualLineColumnNumber(@Nonnull Editor editor, final int line) {
    if (editor instanceof RealEditor) {
      LogicalPosition lineEndPosition = editor.visualToLogicalPosition(new VisualPosition(line, Integer.MAX_VALUE));
      int lineEndOffset = editor.logicalPositionToOffset(lineEndPosition);
      return editor.offsetToVisualPosition(lineEndOffset, true, true).column;
    }
    Document document = editor.getDocument();
    int lastLine = document.getLineCount() - 1;
    if (lastLine < 0) {
      return 0;
    }

    // Filter all lines that are not shown because of collapsed folding region.
    VisualPosition visStart = new VisualPosition(line, 0);
    LogicalPosition logStart = editor.visualToLogicalPosition(visStart);
    int lastLogLine = logStart.line;
    while (lastLogLine < document.getLineCount() - 1) {
      logStart = new LogicalPosition(logStart.line + 1, logStart.column);
      VisualPosition tryVisible = editor.logicalToVisualPosition(logStart);
      if (tryVisible.line != visStart.line) break;
      lastLogLine = logStart.line;
    }

    int resultLogLine = Math.min(lastLogLine, lastLine);
    VisualPosition resVisStart = editor.offsetToVisualPosition(document.getLineStartOffset(resultLogLine));
    VisualPosition resVisEnd = editor.offsetToVisualPosition(document.getLineEndOffset(resultLogLine));

    // Target logical line is not soft wrap affected.
    if (resVisStart.line == resVisEnd.line) {
      return resVisEnd.column;
    }

    int visualLinesToSkip = line - resVisStart.line;
    List<? extends SoftWrap> softWraps = editor.getSoftWrapModel().getSoftWrapsForLine(resultLogLine);
    for (int i = 0; i < softWraps.size(); i++) {
      SoftWrap softWrap = softWraps.get(i);
      CharSequence text = document.getCharsSequence();
      if (visualLinesToSkip <= 0) {
        VisualPosition visual = editor.offsetToVisualPosition(softWrap.getStart() - 1);
        int result = visual.column;
        int x = editor.visualPositionToXY(visual).x;
        // We need to add width of the next symbol because current result column points to the last symbol before the soft wrap.
        return result + textWidthInColumns(editor, text, softWrap.getStart() - 1, softWrap.getStart(), x);
      }

      int softWrapLineFeeds = StringUtil.countNewLines(softWrap.getText());
      if (softWrapLineFeeds < visualLinesToSkip) {
        visualLinesToSkip -= softWrapLineFeeds;
        continue;
      }

      // Target visual column is located on the last visual line of the current soft wrap.
      if (softWrapLineFeeds == visualLinesToSkip) {
        if (i >= softWraps.size() - 1) {
          return resVisEnd.column;
        }
        // We need to find visual column for line feed of the next soft wrap.
        SoftWrap nextSoftWrap = softWraps.get(i + 1);
        VisualPosition visual = editor.offsetToVisualPosition(nextSoftWrap.getStart() - 1);
        int result = visual.column;
        int x = editor.visualPositionToXY(visual).x;

        // We need to add symbol width because current column points to the last symbol before the next soft wrap;
        result += textWidthInColumns(editor, text, nextSoftWrap.getStart() - 1, nextSoftWrap.getStart(), x);

        int lineFeedIndex = StringUtil.indexOf(nextSoftWrap.getText(), '\n');
        result += textWidthInColumns(editor, nextSoftWrap.getText(), 0, lineFeedIndex, 0);
        return result;
      }

      // Target visual column is the one before line feed introduced by the current soft wrap.
      int softWrapStartOffset = 0;
      int softWrapEndOffset = 0;
      int softWrapTextLength = softWrap.getText().length();
      while (visualLinesToSkip-- > 0) {
        softWrapStartOffset = softWrapEndOffset + 1;
        if (softWrapStartOffset >= softWrapTextLength) {
          assert false;
          return resVisEnd.column;
        }
        softWrapEndOffset = StringUtil.indexOf(softWrap.getText(), '\n', softWrapStartOffset, softWrapTextLength);
        if (softWrapEndOffset < 0) {
          assert false;
          return resVisEnd.column;
        }
      }
      VisualPosition visual = editor.offsetToVisualPosition(softWrap.getStart() - 1);
      int result = visual.column; // Column of the symbol just before the soft wrap
      int x = editor.visualPositionToXY(visual).x;

      // Target visual column is located on the last visual line of the current soft wrap.
      result += textWidthInColumns(editor, text, softWrap.getStart() - 1, softWrap.getStart(), x);
      result += calcColumnNumber(editor, softWrap.getText(), softWrapStartOffset, softWrapEndOffset);
      return result;
    }

    CharSequence editorInfo = "editor's class: " +
                              editor.getClass() +
                              ", all soft wraps: " +
                              editor.getSoftWrapModel().getSoftWrapsForRange(0, document.getTextLength()) +
                              ", fold regions: " +
                              Arrays.toString(editor.getFoldingModel().getAllFoldRegions());
    LoggerUtil.error(LOG, "Can't calculate last visual column", String.format(
            "Target visual line: %d, mapped logical line: %d, visual lines range for the mapped logical line: [%s]-[%s], soft wraps for " + "the target logical line: %s. Editor info: %s", line,
            resultLogLine, resVisStart, resVisEnd, softWraps, editorInfo));

    return resVisEnd.column;
  }

  public static int calcColumnNumber(@Nonnull Editor editor, @Nonnull CharSequence text, int start, int offset) {
    return calcColumnNumber(editor, text, start, offset, getTabSize(editor));
  }

  public static int calcColumnNumber(@Nullable Editor editor, @Nonnull CharSequence text, final int start, final int offset, final int tabSize) {
    if (editor instanceof TextComponentEditor) {
      return offset - start;
    }
    boolean useOptimization = true;
    if (editor != null) {
      SoftWrap softWrap = editor.getSoftWrapModel().getSoftWrap(start);
      useOptimization = softWrap == null;
    }
    if (useOptimization) {
      boolean hasNonTabs = false;
      for (int i = start; i < offset; i++) {
        if (text.charAt(i) == '\t') {
          if (hasNonTabs) {
            useOptimization = false;
            break;
          }
        }
        else {
          hasNonTabs = true;
        }
      }
    }

    if (editor != null && useOptimization) {
      Document document = editor.getDocument();
      if (start < offset - 1 && document.getLineNumber(start) != document.getLineNumber(offset - 1)) {
        String editorInfo = editor instanceof Dumpable ? ". Editor info: " + ((Dumpable)editor).dumpState() : "";
        String documentInfo;
        if (text instanceof Dumpable) {
          documentInfo = ((Dumpable)text).dumpState();
        }
        else {
          documentInfo = "Text holder class: " + text.getClass();
        }
        LoggerUtil.error(LOG, "detected incorrect offset -> column number calculation", "start: " + start + ", given offset: " + offset + ", given tab size: " + tabSize + ". " + documentInfo + editorInfo);
      }
    }

    int shift = 0;
    for (int i = start; i < offset; i++) {
      char c = text.charAt(i);
      if (c == '\t') {
        shift += getTabLength(i + shift - start, tabSize) - 1;
      }
    }
    return offset - start + shift;
  }

  private static int getTabLength(int colNumber, int tabSize) {
    if (tabSize <= 0) {
      tabSize = 1;
    }
    return tabSize - colNumber % tabSize;
  }

  public static int textWidthInColumns(@Nonnull Editor editor, @Nonnull CharSequence text, int start, int end, int x) {
    int startToUse = start;
    int lastTabSymbolIndex = -1;

    // Skip all lines except the last.
    loop:
    for (int i = end - 1; i >= start; i--) {
      switch (text.charAt(i)) {
        case '\n':
          startToUse = i + 1;
          break loop;
        case '\t':
          if (lastTabSymbolIndex < 0) lastTabSymbolIndex = i;
      }
    }

    // Tabulation is assumed to be the only symbol which representation may take various number of visual columns, hence,
    // we return eagerly if no such symbol is found.
    if (lastTabSymbolIndex < 0) {
      return end - startToUse;
    }

    int result = 0;
    int spaceSize = getSpaceWidth(Font.PLAIN, editor);

    // Calculate number of columns up to the latest tabulation symbol.
    for (int i = startToUse; i <= lastTabSymbolIndex; i++) {
      SoftWrap softWrap = editor.getSoftWrapModel().getSoftWrap(i);
      if (softWrap != null) {
        x = softWrap.getIndentInPixels();
      }
      char c = text.charAt(i);
      int prevX = x;
      switch (c) {
        case '\t':
          x = nextTabStop(x, editor);
          result += columnsNumber(x - prevX, spaceSize);
          break;
        case '\n':
          x = result = 0;
          break;
        default:
          x += charWidth(c, Font.PLAIN, editor);
          result++;
      }
    }

    // Add remaining tabulation-free columns.
    result += end - lastTabSymbolIndex - 1;
    return result;
  }

  public static int nextTabStop(int x, @Nonnull Editor editor) {
    int tabSize = getTabSize(editor);
    if (tabSize <= 0) {
      tabSize = 1;
    }
    return nextTabStop(x, editor, tabSize);
  }

  /**
   * Allows to answer how many visual columns are occupied by the given width.
   *
   * @param width          target width
   * @param plainSpaceSize width of the single space symbol within the target editor (in plain font style)
   * @return number of visual columns are occupied by the given width
   */
  public static int columnsNumber(int width, int plainSpaceSize) {
    int result = width / plainSpaceSize;
    if (width % plainSpaceSize > 0) {
      result++;
    }
    return result;
  }

  public static int nextTabStop(int x, @Nonnull Editor editor, int tabSize) {
    int leftInset = editor.getContentComponent().getInsets().left;
    return nextTabStop(x - leftInset, getSpaceWidth(Font.PLAIN, editor), tabSize) + leftInset;
  }

  public static int nextTabStop(int x, int plainSpaceWidth, int tabSize) {
    if (tabSize <= 0) {
      return x + plainSpaceWidth;
    }
    tabSize *= plainSpaceWidth;

    int nTabs = x / tabSize;
    return (nTabs + 1) * tabSize;
  }

  public static int getSpaceWidth(@JdkConstants.FontStyle int fontType, @Nonnull Editor editor) {
    int width = charWidth(' ', fontType, editor);
    return width > 0 ? width : 1;
  }

  public static int getPlainSpaceWidth(@Nonnull Editor editor) {
    return getSpaceWidth(Font.PLAIN, editor);
  }

  public static int charWidth(char c, @JdkConstants.FontStyle int fontType, @Nonnull Editor editor) {
    return fontForChar(c, fontType, editor).charWidth(c);
  }

  @Nonnull
  public static FontInfo fontForChar(final char c, @JdkConstants.FontStyle int style, @Nonnull Editor editor) {
    EditorColorsScheme colorsScheme = editor.getColorsScheme();
    return ComplementaryFontsRegistry.getFontAbleToDisplay(c, style, colorsScheme.getFontPreferences(), FontInfo.getFontRenderContext(editor.getContentComponent()));
  }
}
