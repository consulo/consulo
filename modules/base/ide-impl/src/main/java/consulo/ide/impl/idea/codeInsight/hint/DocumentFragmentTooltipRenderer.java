/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.hint;

import consulo.codeEditor.*;
import consulo.document.Document;
import consulo.document.DocumentFragment;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.internal.hint.TooltipGroup;
import consulo.language.editor.impl.internal.hint.TooltipRenderer;
import consulo.language.editor.ui.internal.EditorFragmentComponent;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.util.ScreenUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author cdr
 */
public class DocumentFragmentTooltipRenderer implements TooltipRenderer {
  private final DocumentFragment myDocumentFragment;

  public DocumentFragmentTooltipRenderer(DocumentFragment documentFragment) {
    myDocumentFragment = documentFragment;
  }

  @Override
  public LightweightHint show(@Nonnull Editor editor, @Nonnull Point p, boolean alignToRight, @Nonnull TooltipGroup group, @Nonnull HintHint intInfo) {
    LightweightHint hint;

    JComponent editorComponent = editor.getComponent();

    TextRange range = myDocumentFragment.getTextRange();
    int startOffset = range.getStartOffset();
    int endOffset = range.getEndOffset();
    Document doc = myDocumentFragment.getDocument();
    int endLine = doc.getLineNumber(endOffset);
    int startLine = doc.getLineNumber(startOffset);

    JLayeredPane layeredPane = editorComponent.getRootPane().getLayeredPane();

    // There is a possible case that collapsed folding region is soft wrapped, hence, we need to anchor
    // not logical but visual line start.
    VisualPosition visual = editor.offsetToVisualPosition(startOffset);
    p = editor.visualPositionToXY(visual);
    p = SwingUtilities.convertPoint(
            ((EditorEx)editor).getGutterComponentEx().getComponent(),
            p,
            layeredPane
    );

    p.x -= 3;
    p.y += editor.getLineHeight();

    Point screenPoint = new Point(p);
    SwingUtilities.convertPointToScreen(screenPoint, layeredPane);
    int maxLineCount = (ScreenUtil.getScreenRectangle(screenPoint).height - screenPoint.y) / editor.getLineHeight();

    if (endLine - startLine > maxLineCount) {
      endOffset = doc.getLineEndOffset(Math.max(0, Math.min(startLine + maxLineCount, doc.getLineCount() - 1)));
    }
    if (endOffset < startOffset) return null;

    FoldingModel foldingModel = editor.getFoldingModel();
    foldingModel.setFoldingEnabled(false);
    TextRange textRange = new TextRange(startOffset, endOffset);
    hint = EditorFragmentComponent.showEditorFragmentHintAt(editor, textRange, p.y, false, false, true, true, true);
    foldingModel.setFoldingEnabled(true);
    return hint;
  }
}
