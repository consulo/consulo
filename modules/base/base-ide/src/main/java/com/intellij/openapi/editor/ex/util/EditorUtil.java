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
package com.intellij.openapi.editor.ex.util;

import com.intellij.diagnostic.Dumpable;
import com.intellij.diagnostic.LogMessageEx;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.editor.ex.DocumentBulkUpdateListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.impl.ComplementaryFontsRegistry;
import com.intellij.openapi.editor.impl.DesktopEditorImpl;
import com.intellij.openapi.editor.impl.DesktopScrollingModelImpl;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.textarea.TextComponentEditor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.DocumentUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.messages.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.text.TextEditorProvider;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import org.intellij.lang.annotations.JdkConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Arrays;
import java.util.List;

public final class EditorUtil {
  private static final Logger LOG = Logger.getInstance(EditorUtil.class);

  private EditorUtil() {
  }

  /**
   * @return true if the editor is in fact an ordinary file editor;
   * false if the editor is part of EditorTextField, CommitMessage and etc.
   */
  public static boolean isRealFileEditor(@Nullable Editor editor) {
    return editor != null && TextEditorProvider.getInstance().getTextEditor(editor) instanceof TextEditorImpl;
  }

  public static boolean isPasswordEditor(@Nullable Editor editor) {
    return editor != null && editor.getContentComponent() instanceof JPasswordField;
  }

  @Nullable
  public static EditorEx getEditorEx(@Nullable FileEditor fileEditor) {
    Editor editor = fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() : null;
    return editor instanceof EditorEx ? (EditorEx)editor : null;
  }

  public static int getLastVisualLineColumnNumber(@Nonnull Editor editor, final int line) {
    if (editor instanceof DesktopEditorImpl) {
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
    LogMessageEx.error(LOG, "Can't calculate last visual column", String.format(
            "Target visual line: %d, mapped logical line: %d, visual lines range for the mapped logical line: [%s]-[%s], soft wraps for " + "the target logical line: %s. Editor info: %s", line,
            resultLogLine, resVisStart, resVisEnd, softWraps, editorInfo));

    return resVisEnd.column;
  }

  public static int getVisualLineEndOffset(@Nonnull Editor editor, int line) {
    VisualPosition endLineVisualPosition = new VisualPosition(line, getLastVisualLineColumnNumber(editor, line));
    LogicalPosition endLineLogicalPosition = editor.visualToLogicalPosition(endLineVisualPosition);
    return editor.logicalPositionToOffset(endLineLogicalPosition);
  }

  public static float calcVerticalScrollProportion(@Nonnull Editor editor) {
    Rectangle viewArea = editor.getScrollingModel().getVisibleAreaOnScrollingFinished();
    if (viewArea.height == 0) {
      return 0;
    }
    LogicalPosition pos = editor.getCaretModel().getLogicalPosition();
    Point location = editor.logicalPositionToXY(pos);
    return (location.y - viewArea.y) / (float)viewArea.height;
  }

  public static void setVerticalScrollProportion(@Nonnull Editor editor, float proportion) {
    Rectangle viewArea = editor.getScrollingModel().getVisibleArea();
    LogicalPosition caretPosition = editor.getCaretModel().getLogicalPosition();
    Point caretLocation = editor.logicalPositionToXY(caretPosition);
    int yPos = caretLocation.y;
    yPos -= viewArea.height * proportion;
    editor.getScrollingModel().scrollVertically(yPos);
  }

  public static int calcRelativeCaretPosition(@Nonnull Editor editor) {
    int caretY = editor.getCaretModel().getVisualPosition().line * editor.getLineHeight();
    int viewAreaPosition = editor.getScrollingModel().getVisibleAreaOnScrollingFinished().y;
    return caretY - viewAreaPosition;
  }

  public static void setRelativeCaretPosition(@Nonnull Editor editor, int position) {
    int caretY = editor.getCaretModel().getVisualPosition().line * editor.getLineHeight();
    editor.getScrollingModel().scrollVertically(caretY - position);
  }

  public static void fillVirtualSpaceUntilCaret(@Nonnull Editor editor) {
    final LogicalPosition position = editor.getCaretModel().getLogicalPosition();
    fillVirtualSpaceUntil(editor, position.column, position.line);
  }

  public static void fillVirtualSpaceUntil(@Nonnull final Editor editor, int columnNumber, int lineNumber) {
    final int offset = editor.logicalPositionToOffset(new LogicalPosition(lineNumber, columnNumber));
    final String filler = EditorModificationUtil.calcStringToFillVirtualSpace(editor);
    if (!filler.isEmpty()) {
      WriteAction.run(() -> {
        editor.getDocument().insertString(offset, filler);
        editor.getCaretModel().moveToOffset(offset + filler.length());
      });
    }
  }

  private static int getTabLength(int colNumber, int tabSize) {
    if (tabSize <= 0) {
      tabSize = 1;
    }
    return tabSize - colNumber % tabSize;
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
        String editorInfo = editor instanceof DesktopEditorImpl ? ". Editor info: " + ((DesktopEditorImpl)editor).dumpState() : "";
        String documentInfo;
        if (text instanceof Dumpable) {
          documentInfo = ((Dumpable)text).dumpState();
        }
        else {
          documentInfo = "Text holder class: " + text.getClass();
        }
        LogMessageEx.error(LOG, "detected incorrect offset -> column number calculation",
                           "start: " + start + ", given offset: " + offset + ", given tab size: " + tabSize + ". " + documentInfo + editorInfo);
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

  public static void setHandCursor(@Nonnull Editor view) {
    Cursor c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    // XXX: Workaround, simply view.getContentComponent().setCursor(c) doesn't work
    if (view.getContentComponent().getCursor() != c) {
      view.getContentComponent().setCursor(c);
    }
  }

  @Nonnull
  public static FontInfo fontForChar(final char c, @JdkConstants.FontStyle int style, @Nonnull Editor editor) {
    EditorColorsScheme colorsScheme = editor.getColorsScheme();
    return ComplementaryFontsRegistry.getFontAbleToDisplay(c, style, colorsScheme.getFontPreferences(), FontInfo.getFontRenderContext(editor.getContentComponent()));
  }

  public static Image scaleIconAccordingEditorFont(@Nonnull Image icon, Editor editor) {
    if (Registry.is("editor.scale.gutter.icons") && editor instanceof DesktopEditorImpl) {
      float scale = ((DesktopEditorImpl)editor).getScale();
      if (Math.abs(1f - scale) > 0.1f) {
        return ImageEffects.resize(icon, scale);
      }
    }
    return icon;
  }

  public static int charWidth(char c, @JdkConstants.FontStyle int fontType, @Nonnull Editor editor) {
    return fontForChar(c, fontType, editor).charWidth(c);
  }

  public static int getSpaceWidth(@JdkConstants.FontStyle int fontType, @Nonnull Editor editor) {
    int width = charWidth(' ', fontType, editor);
    return width > 0 ? width : 1;
  }

  public static int getPlainSpaceWidth(@Nonnull Editor editor) {
    return getSpaceWidth(Font.PLAIN, editor);
  }

  public static int getTabSize(@Nonnull Editor editor) {
    return editor.getSettings().getTabSize(editor.getProject());
  }

  public static int nextTabStop(int x, @Nonnull Editor editor) {
    int tabSize = getTabSize(editor);
    if (tabSize <= 0) {
      tabSize = 1;
    }
    return nextTabStop(x, editor, tabSize);
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

  public static float nextTabStop(float x, float plainSpaceWidth, int tabSize) {
    if (tabSize <= 0) {
      return x + plainSpaceWidth;
    }
    tabSize *= plainSpaceWidth;

    int nTabs = (int)(x / tabSize);
    return (nTabs + 1) * tabSize;
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

  public static int columnsNumber(float width, float plainSpaceSize) {
    return (int)Math.ceil(width / plainSpaceSize);
  }

  /**
   * Allows to answer what width in pixels is required to draw fragment of the given char array from <code>[start; end)</code> interval
   * at the given editor.
   * <p>
   * Tabulation symbols is processed specially, i.e. it's ta
   * <p>
   * <b>Note:</b> it's assumed that target text fragment remains to the single line, i.e. line feed symbols within it are not
   * treated specially.
   *
   * @param editor   editor that will be used for target text representation
   * @param text     target text holder
   * @param start    offset within the given char array that points to target text start (inclusive)
   * @param end      offset within the given char array that points to target text end (exclusive)
   * @param fontType font type to use for target text representation
   * @param x        <code>'x'</code> coordinate that should be used as a starting point for target text representation.
   *                 It's necessity is implied by the fact that IDEA editor may represent tabulation symbols in any range
   *                 from <code>[1; tab size]</code> (check {@link #nextTabStop(int, Editor)} for more details)
   * @return width in pixels required for target text representation
   */
  public static int textWidth(@Nonnull Editor editor, @Nonnull CharSequence text, int start, int end, @JdkConstants.FontStyle int fontType, int x) {
    int result = 0;
    for (int i = start; i < end; i++) {
      char c = text.charAt(i);
      if (c != '\t') {
        FontInfo font = fontForChar(c, fontType, editor);
        result += font.charWidth(c);
        continue;
      }

      result += nextTabStop(x + result, editor) - result - x;
    }
    return result;
  }

  /**
   * Delegates to the {@link #calcSurroundingRange(Editor, VisualPosition, VisualPosition)} with the
   * {@link CaretModel#getVisualPosition() caret visual position} as an argument.
   *
   * @param editor target editor
   * @return surrounding logical positions
   * @see #calcSurroundingRange(Editor, VisualPosition, VisualPosition)
   */
  public static Pair<LogicalPosition, LogicalPosition> calcCaretLineRange(@Nonnull Editor editor) {
    return calcSurroundingRange(editor, editor.getCaretModel().getVisualPosition(), editor.getCaretModel().getVisualPosition());
  }

  public static Pair<LogicalPosition, LogicalPosition> calcCaretLineRange(@Nonnull Caret caret) {
    return calcSurroundingRange(caret.getEditor(), caret.getVisualPosition(), caret.getVisualPosition());
  }

  /**
   * Calculates logical positions that surround given visual positions and conform to the following criteria:
   * <pre>
   * <ul>
   *   <li>located at the start or the end of the visual line;</li>
   *   <li>doesn't have soft wrap at the target offset;</li>
   * </ul>
   * </pre>
   * Example:
   * <pre>
   *   first line [soft-wrap] some [start-position] text [end-position] [fold-start] fold line 1
   *   fold line 2
   *   fold line 3[fold-end] [soft-wrap] end text
   * </pre>
   * The very first and the last positions will be returned here.
   *
   * @param editor target editor to use
   * @param start  target start coordinate
   * @param end    target end coordinate
   * @return pair of the closest surrounding non-soft-wrapped logical positions for the visual line start and end
   * @see #getNotFoldedLineStartOffset(Editor, int)
   * @see #getNotFoldedLineEndOffset(Editor, int)
   */
  @SuppressWarnings("AssignmentToForLoopParameter")
  public static Pair<LogicalPosition, LogicalPosition> calcSurroundingRange(@Nonnull Editor editor, @Nonnull VisualPosition start, @Nonnull VisualPosition end) {
    final Document document = editor.getDocument();
    final FoldingModel foldingModel = editor.getFoldingModel();

    LogicalPosition first = editor.visualToLogicalPosition(new VisualPosition(start.line, 0));
    for (int line = first.line, offset = document.getLineStartOffset(line); offset >= 0; offset = document.getLineStartOffset(line)) {
      final FoldRegion foldRegion = foldingModel.getCollapsedRegionAtOffset(offset);
      if (foldRegion == null) {
        first = new LogicalPosition(line, 0);
        break;
      }
      final int foldEndLine = document.getLineNumber(foldRegion.getStartOffset());
      if (foldEndLine <= line) {
        first = new LogicalPosition(line, 0);
        break;
      }
      line = foldEndLine;
    }


    LogicalPosition second = editor.visualToLogicalPosition(new VisualPosition(end.line, 0));
    for (int line = second.line, offset = document.getLineEndOffset(line); offset <= document.getTextLength(); offset = document.getLineEndOffset(line)) {
      final FoldRegion foldRegion = foldingModel.getCollapsedRegionAtOffset(offset);
      if (foldRegion == null) {
        second = new LogicalPosition(line + 1, 0);
        break;
      }
      final int foldEndLine = document.getLineNumber(foldRegion.getEndOffset());
      if (foldEndLine <= line) {
        second = new LogicalPosition(line + 1, 0);
        break;
      }
      line = foldEndLine;
    }

    if (second.line >= document.getLineCount()) {
      second = editor.offsetToLogicalPosition(document.getTextLength());
    }
    return Pair.create(first, second);
  }

  /**
   * Finds the start offset of visual line at which given offset is located, not taking soft wraps into account.
   */
  public static int getNotFoldedLineStartOffset(@Nonnull Editor editor, int offset) {
    while (true) {
      offset = DocumentUtil.getLineStartOffset(offset, editor.getDocument());
      FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(offset - 1);
      if (foldRegion == null || foldRegion.getStartOffset() >= offset) {
        break;
      }
      offset = foldRegion.getStartOffset();
    }
    return offset;
  }

  /**
   * Finds the end offset of visual line at which given offset is located, not taking soft wraps into account.
   */
  public static int getNotFoldedLineEndOffset(@Nonnull Editor editor, int offset) {
    while (true) {
      offset = getLineEndOffset(offset, editor.getDocument());
      FoldRegion foldRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(offset);
      if (foldRegion == null || foldRegion.getEndOffset() <= offset) {
        break;
      }
      offset = foldRegion.getEndOffset();
    }
    return offset;
  }

  private static int getLineEndOffset(int offset, Document document) {
    if (offset >= document.getTextLength()) {
      return offset;
    }
    int lineNumber = document.getLineNumber(offset);
    return document.getLineEndOffset(lineNumber);
  }

  public static void scrollToTheEnd(@Nonnull Editor editor) {
    scrollToTheEnd(editor, false);
  }

  public static void scrollToTheEnd(@Nonnull Editor editor, boolean preferVerticalScroll) {
    editor.getSelectionModel().removeSelection();
    Document document = editor.getDocument();
    int lastLine = Math.max(0, document.getLineCount() - 1);
    boolean caretWasAtLastLine = editor.getCaretModel().getLogicalPosition().line == lastLine;
    editor.getCaretModel().moveToOffset(document.getTextLength());
    ScrollingModel scrollingModel = editor.getScrollingModel();
    if (preferVerticalScroll && document.getLineStartOffset(lastLine) == document.getLineEndOffset(lastLine)) {
      // don't move 'focus' to empty last line
      int scrollOffset;
      if (editor instanceof EditorEx) {
        JScrollBar verticalScrollBar = ((EditorEx)editor).getScrollPane().getVerticalScrollBar();
        scrollOffset = verticalScrollBar.getMaximum() - verticalScrollBar.getModel().getExtent();
      }
      else {
        scrollOffset = editor.getContentComponent().getHeight() - scrollingModel.getVisibleArea().height;
      }
      scrollingModel.scrollVertically(scrollOffset);
    }
    else if (!caretWasAtLastLine) {
      // don't scroll to the end of the last line (IDEA-124688)...
      scrollingModel.scrollTo(new LogicalPosition(lastLine, 0), ScrollType.RELATIVE);
    }
    else {
      // ...unless the caret was already on the last line - then scroll to the end of it.
      scrollingModel.scrollToCaret(ScrollType.RELATIVE);
    }
  }

  public static boolean isChangeFontSize(@Nonnull MouseWheelEvent e) {
    if (e.getWheelRotation() == 0) return false;
    return SystemInfo.isMac ? !e.isControlDown() && e.isMetaDown() && !e.isAltDown() && !e.isShiftDown() : e.isControlDown() && !e.isMetaDown() && !e.isAltDown() && !e.isShiftDown();
  }

  public static boolean inVirtualSpace(@Nonnull Editor editor, @Nonnull LogicalPosition logicalPosition) {
    return !editor.offsetToLogicalPosition(editor.logicalPositionToOffset(logicalPosition)).equals(logicalPosition);
  }

  public static void reinitSettings() {
    EditorFactory.getInstance().refreshAllEditors();
  }

  @Nonnull
  public static TextRange getSelectionInAnyMode(Editor editor) {
    SelectionModel selection = editor.getSelectionModel();
    int[] starts = selection.getBlockSelectionStarts();
    int[] ends = selection.getBlockSelectionEnds();
    int start = starts.length > 0 ? starts[0] : selection.getSelectionStart();
    int end = ends.length > 0 ? ends[ends.length - 1] : selection.getSelectionEnd();
    return TextRange.create(start, end);
  }

  public static int yPositionToLogicalLine(@Nonnull Editor editor, @Nonnull MouseEvent event) {
    return yPositionToLogicalLine(editor, event.getY());
  }

  public static int yPositionToLogicalLine(@Nonnull Editor editor, @Nonnull Point point) {
    return yPositionToLogicalLine(editor, point.y);
  }

  public static int yPositionToLogicalLine(@Nonnull Editor editor, int y) {
    int line = editor instanceof DesktopEditorImpl ? editor.yToVisualLine(y) : y / editor.getLineHeight();
    return line > 0 ? editor.visualToLogicalPosition(new VisualPosition(line, 0)).line : 0;
  }

  public static boolean isAtLineEnd(@Nonnull Editor editor, int offset) {
    Document document = editor.getDocument();
    if (offset < 0 || offset > document.getTextLength()) {
      return false;
    }
    int line = document.getLineNumber(offset);
    return offset == document.getLineEndOffset(line);
  }

  /**
   * Setting selection using {@link SelectionModel#setSelection(int, int)} or {@link Caret#setSelection(int, int)} methods can result
   * in resulting selection range to be larger than requested (in case requested range intersects with collapsed fold regions).
   * This method will make sure interfering collapsed regions are expanded first, so that resulting selection range is exactly as
   * requested.
   */
  public static void setSelectionExpandingFoldedRegionsIfNeeded(@Nonnull Editor editor, int startOffset, int endOffset) {
    FoldingModel foldingModel = editor.getFoldingModel();
    FoldRegion startFoldRegion = foldingModel.getCollapsedRegionAtOffset(startOffset);
    if (startFoldRegion != null && (startFoldRegion.getStartOffset() == startOffset || startFoldRegion.isExpanded())) {
      startFoldRegion = null;
    }
    FoldRegion endFoldRegion = foldingModel.getCollapsedRegionAtOffset(endOffset);
    if (endFoldRegion != null && (endFoldRegion.getStartOffset() == endOffset || endFoldRegion.isExpanded())) {
      endFoldRegion = null;
    }
    if (startFoldRegion != null || endFoldRegion != null) {
      final FoldRegion finalStartFoldRegion = startFoldRegion;
      final FoldRegion finalEndFoldRegion = endFoldRegion;
      foldingModel.runBatchFoldingOperation(() -> {
        if (finalStartFoldRegion != null) finalStartFoldRegion.setExpanded(true);
        if (finalEndFoldRegion != null) finalEndFoldRegion.setExpanded(true);
      });
    }
    editor.getSelectionModel().setSelection(startOffset, endOffset);
  }

  public static Font getEditorFont() {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    int size = UISettings.getInstance().getPresentationMode() ? UISettings.getInstance().getPresentationModeFontSize() - 4 : scheme.getEditorFontSize();
    return new Font(scheme.getEditorFontName(), Font.PLAIN, size);
  }

  public static int getDefaultCaretWidth() {
    return Registry.intValue("editor.caret.width", 2);
  }

  /**
   * Number of virtual soft wrap introduced lines on a current logical line before the visual position that corresponds
   * to the current logical position.
   *
   * @see LogicalPosition#softWrapLinesOnCurrentLogicalLine
   */
  public static int getSoftWrapCountAfterLineStart(@Nonnull Editor editor, @Nonnull LogicalPosition position) {
    if (position.visualPositionAware) {
      return position.softWrapLinesOnCurrentLogicalLine;
    }
    int startOffset = editor.getDocument().getLineStartOffset(position.line);
    int endOffset = editor.logicalPositionToOffset(position);
    return editor.getSoftWrapModel().getSoftWrapsForRange(startOffset, endOffset).size();
  }

  public static boolean attributesImpactFontStyleOrColor(@Nullable TextAttributes attributes) {
    return attributes == TextAttributes.ERASE_MARKER || (attributes != null && (attributes.getFontType() != Font.PLAIN || attributes.getForegroundColor() != null));
  }

  public static boolean isCurrentCaretPrimary(@Nonnull Editor editor) {
    return editor.getCaretModel().getCurrentCaret() == editor.getCaretModel().getPrimaryCaret();
  }

  public static void disposeWithEditor(@Nonnull Editor editor, @Nonnull Disposable disposable) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (Disposer.isDisposed(disposable)) return;
    if (editor.isDisposed()) {
      Disposer.dispose(disposable);
      return;
    }
    // for injected editors disposal will happen only when host editor is disposed,
    // but this seems to be the best we can do (there are no notifications on disposal of injected editor)
    Editor hostEditor = editor instanceof EditorWindow ? ((EditorWindow)editor).getDelegate() : editor;
    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      @Override
      public void editorReleased(@Nonnull EditorFactoryEvent event) {
        if (event.getEditor() == hostEditor) {
          Disposer.dispose(disposable);
        }
      }
    }, disposable);
  }

  public static void runBatchFoldingOperationOutsideOfBulkUpdate(@Nonnull Editor editor, @Nonnull Runnable operation) {
    DocumentEx document = ObjectUtils.tryCast(editor.getDocument(), DocumentEx.class);
    if (document != null && document.isInBulkUpdate()) {
      MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
      disposeWithEditor(editor, connection::disconnect);
      connection.subscribe(DocumentBulkUpdateListener.TOPIC, new DocumentBulkUpdateListener.Adapter() {
        @Override
        public void updateFinished(@Nonnull Document doc) {
          if (doc == editor.getDocument()) {
            editor.getFoldingModel().runBatchFoldingOperation(operation);
            connection.disconnect();
          }
        }
      });
    }
    else {
      editor.getFoldingModel().runBatchFoldingOperation(operation);
    }
  }

  public static void runWithAnimationDisabled(@Nonnull Editor editor, @Nonnull Runnable taskWithScrolling) {
    ScrollingModel scrollingModel = editor.getScrollingModel();
    if (!(scrollingModel instanceof DesktopScrollingModelImpl)) {
      taskWithScrolling.run();
    }
    else {
      boolean animationWasEnabled = ((DesktopScrollingModelImpl)scrollingModel).isAnimationEnabled();
      scrollingModel.disableAnimation();
      try {
        taskWithScrolling.run();
      }
      finally {
        if (animationWasEnabled) scrollingModel.enableAnimation();
      }
    }
  }

  public static boolean isPrimaryCaretVisible(@Nonnull Editor editor) {
    Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    Caret caret = editor.getCaretModel().getPrimaryCaret();
    Point caretPoint = editor.visualPositionToXY(caret.getVisualPosition());
    return visibleArea.contains(caretPoint);
  }

  /**
   * Performs inlay-aware conversion of offset to visual position in editor. If there are inlays at given position, their
   * 'related to preceding text' property will be taken account to determine resulting position. Specifically, resulting position will
   * match caret's visual position if it's moved to the given offset using {@link Caret#moveToOffset(int)} call.
   * <p>
   * NOTE: if editor is an {@link EditorWindow}, corresponding offset is treated as an offset in injected editor, but returned position
   * is always related to host editor.
   *
   * @see Inlay#isRelatedToPrecedingText()
   */
  @Nonnull
  public static VisualPosition inlayAwareOffsetToVisualPosition(@Nonnull Editor editor, int offset) {
    LogicalPosition logicalPosition = editor.offsetToLogicalPosition(offset);
    if (editor instanceof EditorWindow) {
      logicalPosition = ((EditorWindow)editor).injectedToHost(logicalPosition);
      editor = ((EditorWindow)editor).getDelegate();
    }
    VisualPosition pos = editor.logicalToVisualPosition(logicalPosition);
    Inlay inlay;
    while ((inlay = editor.getInlayModel().getInlineElementAt(pos)) != null) {
      if (inlay.isRelatedToPrecedingText()) break;
      pos = new VisualPosition(pos.line, pos.column + 1);
    }
    return pos;
  }

  public static int getTotalInlaysHeight(@Nonnull List<? extends Inlay> inlays) {
    int sum = 0;
    for (Inlay inlay : inlays) {
      sum += inlay.getHeightInPixels();
    }
    return sum;
  }

  /**
   * Virtual space (after line end, and after end of text), inlays and space between visual lines (where block inlays are located) is
   * excluded
   */
  public static boolean isPointOverText(@Nonnull Editor editor, @Nonnull Point point) {
    VisualPosition visualPosition = editor.xyToVisualPosition(point);
    int visualLineStartY = editor.visualLineToY(visualPosition.line);
    if (point.y < visualLineStartY || point.y >= visualLineStartY + editor.getLineHeight()) return false; // block inlay space
    if (editor.getSoftWrapModel().isInsideOrBeforeSoftWrap(visualPosition)) return false; // soft wrap
    LogicalPosition logicalPosition = editor.visualToLogicalPosition(visualPosition);
    int offset = editor.logicalPositionToOffset(logicalPosition);
    if (!logicalPosition.equals(editor.offsetToLogicalPosition(offset))) return false; // virtual space
    List<Inlay> inlays = editor.getInlayModel().getInlineElementsInRange(offset, offset);
    if (!inlays.isEmpty()) {
      VisualPosition inlaysStart = editor.offsetToVisualPosition(offset);
      if (inlaysStart.line == visualPosition.line) {
        int relX = point.x - editor.visualPositionToXY(inlaysStart).x;
        if (relX >= 0 && relX < inlays.stream().mapToInt(i -> i.getWidthInPixels()).sum()) return false; // inline inlay
      }
    }
    return true;
  }

  /**
   * This is similar to {@link SelectionModel#addSelectionListener(SelectionListener, Disposable)}, but when selection changes happen within
   * the scope of {@link CaretModel#runForEachCaret(CaretAction)} call, there will be only one notification at the end of iteration over
   * carets.
   */
  public static void addBulkSelectionListener(@Nonnull Editor editor, @Nonnull SelectionListener listener, @Nonnull Disposable disposable) {
    Ref<Pair<int[], int[]>> selectionBeforeBulkChange = new Ref<>();
    Ref<Boolean> selectionChangedDuringBulkChange = new Ref<>();
    editor.getSelectionModel().addSelectionListener(new SelectionListener() {
      @Override
      public void selectionChanged(@Nonnull SelectionEvent e) {
        if (selectionBeforeBulkChange.isNull()) {
          listener.selectionChanged(e);
        }
        else {
          selectionChangedDuringBulkChange.set(Boolean.TRUE);
        }
      }
    }, disposable);
    editor.getCaretModel().addCaretActionListener(new CaretActionListener() {
      @Override
      public void beforeAllCaretsAction() {
        selectionBeforeBulkChange.set(getSelectionOffsets());
        selectionChangedDuringBulkChange.set(null);
      }

      @Override
      public void afterAllCaretsAction() {
        if (!selectionChangedDuringBulkChange.isNull()) {
          Pair<int[], int[]> beforeBulk = selectionBeforeBulkChange.get();
          Pair<int[], int[]> afterBulk = getSelectionOffsets();
          listener.selectionChanged(new SelectionEvent(editor, beforeBulk.first, beforeBulk.second, afterBulk.first, afterBulk.second));
        }
        selectionBeforeBulkChange.set(null);
      }

      private Pair<int[], int[]> getSelectionOffsets() {
        return Pair.create(editor.getSelectionModel().getBlockSelectionStarts(), editor.getSelectionModel().getBlockSelectionEnds());
      }
    }, disposable);
  }


  @Nonnull
  public static DataContext getEditorDataContext(@Nonnull Editor editor) {
    DataContext context = DataManager.getInstance().getDataContext(editor.getContentComponent());
    if (context.getData(CommonDataKeys.PROJECT) == editor.getProject()) {
      return context;
    }
    return new DataContext() {
      @Nullable
      @Override
      public <T> T getData(@Nonnull Key<T> dataId) {
        if (CommonDataKeys.PROJECT == dataId) {
          return (T)editor.getProject();
        }
        return context.getData(dataId);
      }
    };
  }
}
