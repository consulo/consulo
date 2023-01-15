/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.language.editor;

import consulo.codeEditor.*;
import consulo.codeEditor.markup.CustomHighlighterRenderer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.desktop.awt.editor.impl.DesktopEditorImpl;
import consulo.desktop.awt.editor.impl.view.EditorPainter;
import consulo.desktop.awt.editor.impl.view.VisualLinesIterator;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.daemon.impl.IndentsPass;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.awt.paint.LinePainter2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.CharArrayUtil;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 15/01/2023
 */
public class DesktopAWTIndentPass extends IndentsPass {
  private static final CustomHighlighterRenderer RENDERER = (editor, highlighter, g) -> {
    int startOffset = highlighter.getStartOffset();
    final Document doc = highlighter.getDocument();
    if (startOffset >= doc.getTextLength()) return;

    final int endOffset = highlighter.getEndOffset();

    int off;
    int startLine = doc.getLineNumber(startOffset);

    final CharSequence chars = doc.getCharsSequence();
    do {
      int start = doc.getLineStartOffset(startLine);
      int end = doc.getLineEndOffset(startLine);
      off = CharArrayUtil.shiftForward(chars, start, end, " \t");
      startLine--;
    }
    while (startLine > 1 && off < doc.getTextLength() && chars.charAt(off) == '\n');

    final VisualPosition startPosition = editor.offsetToVisualPosition(off);
    int indentColumn = startPosition.column;
    if (indentColumn <= 0) return;

    final FoldingModel foldingModel = editor.getFoldingModel();
    if (foldingModel.isOffsetCollapsed(off)) return;

    final FoldRegion headerRegion = foldingModel.getCollapsedRegionAtOffset(doc.getLineEndOffset(doc.getLineNumber(off)));
    final FoldRegion tailRegion = foldingModel.getCollapsedRegionAtOffset(doc.getLineStartOffset(doc.getLineNumber(endOffset)));

    if (tailRegion != null && tailRegion == headerRegion) return;

    final boolean selected;
    final IndentGuideDescriptor guide = editor.getIndentsModel().getCaretIndentGuide();
    if (guide != null) {
      final CaretModel caretModel = editor.getCaretModel();
      final int caretOffset = caretModel.getOffset();
      selected = caretOffset >= off && caretOffset < endOffset && caretModel.getLogicalPosition().column == indentColumn;
    }
    else {
      selected = false;
    }

    int lineHeight = editor.getLineHeight();
    Point start = editor.visualPositionToXY(startPosition);
    start.y += lineHeight;
    final VisualPosition endPosition = editor.offsetToVisualPosition(endOffset);
    Point end = editor.visualPositionToXY(endPosition);
    int maxY = end.y;
    if (endPosition.line == editor.offsetToVisualPosition(doc.getTextLength()).line) {
      maxY += lineHeight;
    }

    Rectangle clip = g.getClipBounds();
    if (clip != null) {
      if (clip.y >= maxY || clip.y + clip.height <= start.y) {
        return;
      }
      maxY = Math.min(maxY, clip.y + clip.height);
    }

    if (start.y >= maxY) return;

    int targetX = Math.max(0, start.x + EditorPainter.getIndentGuideShift(editor));
    final EditorColorsScheme scheme = editor.getColorsScheme();
    g.setColor(TargetAWT.to(scheme.getColor(selected ? EditorColors.SELECTED_INDENT_GUIDE_COLOR : EditorColors.INDENT_GUIDE_COLOR)));

    // There is a possible case that indent line intersects soft wrap-introduced text. Example:
    //     this is a long line <soft-wrap>
    // that| is soft-wrapped
    //     |
    //     | <- vertical indent
    //
    // Also it's possible that no additional intersections are added because of soft wrap:
    //     this is a long line <soft-wrap>
    //     |   that is soft-wrapped
    //     |
    //     | <- vertical indent
    // We want to use the following approach then:
    //     1. Show only active indent if it crosses soft wrap-introduced text;
    //     2. Show indent as is if it doesn't intersect with soft wrap-introduced text;
    List<? extends SoftWrap> softWraps = ((EditorEx)editor).getSoftWrapModel().getRegisteredSoftWraps();
    if (selected || softWraps.isEmpty()) {
      LinePainter2D.paint((Graphics2D)g, targetX, start.y, targetX, maxY - 1);
    }
    else {
      int startY = start.y;
      int startVisualLine = startPosition.line + 1;
      if (clip != null && startY < clip.y) {
        startY = clip.y;
        startVisualLine = editor.yToVisualLine(clip.y);
      }
      VisualLinesIterator it = new VisualLinesIterator((DesktopEditorImpl)editor, startVisualLine);
      while (!it.atEnd()) {
        int currY = it.getY();
        if (currY >= startY) {
          if (currY >= maxY) break;
          if (it.startsWithSoftWrap()) {
            SoftWrap softWrap = softWraps.get(it.getStartOrPrevWrapIndex());
            if (softWrap.getIndentInColumns() < indentColumn) {
              if (startY < currY) {
                LinePainter2D.paint((Graphics2D)g, targetX, startY, targetX, currY - 1);
              }
              startY = currY + lineHeight;
            }
          }
        }
        it.advance();
      }
      if (startY < maxY) {
        LinePainter2D.paint((Graphics2D)g, targetX, startY, targetX, maxY - 1);
      }
    }
  };

  public DesktopAWTIndentPass(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    super(project, editor, file);
  }

  @Override
  @Nonnull
  protected RangeHighlighter createHighlighter(MarkupModel mm, TextRange range) {
    final RangeHighlighter highlighter = mm.addRangeHighlighter(range.getStartOffset(), range.getEndOffset(), 0, null, HighlighterTargetArea.EXACT_RANGE);
    highlighter.setCustomRenderer(RENDERER);
    return highlighter;
  }
}
