// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.codeEditor.impl.CodeEditorCaretBase;
import consulo.desktop.awt.editor.impl.view.EditorPainter;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import java.awt.*;

public class DesktopCaretImpl extends CodeEditorCaretBase {
  private static final Logger LOG = Logger.getInstance(DesktopCaretImpl.class);

  DesktopCaretImpl(@Nonnull DesktopEditorImpl editor, @Nonnull DesktopCaretModelImpl caretModel) {
    super(editor, caretModel);
  }

  @Override
  protected void requestRepaint(VerticalInfo oldVerticalInfo) {
    if (oldVerticalInfo == null) oldVerticalInfo = new VerticalInfo(0, 0, myEditor.getLineHeight());
    if (myVerticalInfo == null) myVerticalInfo = new VerticalInfo(0, 0, myEditor.getLineHeight());

    int oldY, oldHeight, newY, newHeight;
    if (oldVerticalInfo.logicalLineY == myVerticalInfo.logicalLineY && oldVerticalInfo.logicalLineHeight == myVerticalInfo.logicalLineHeight) {
      // caret moved within the same soft-wrapped line, repaint only original and target visual lines
      oldY = oldVerticalInfo.y;
      newY = myVerticalInfo.y;
      oldHeight = newHeight = myEditor.getLineHeight();
    }
    else {
      // caret moved between different (possible soft-wrapped) lines, repaint whole lines
      // (to repaint soft-wrap markers and line numbers in gutter)
      oldY = oldVerticalInfo.logicalLineY;
      oldHeight = oldVerticalInfo.logicalLineHeight;
      newY = myVerticalInfo.logicalLineY;
      newHeight = myVerticalInfo.logicalLineHeight;
    }

    Rectangle visibleArea = myEditor.getScrollingModel().getVisibleArea();
    EditorGutterComponentEx gutter = myEditor.getGutterComponentEx();
    EditorComponentImpl content = (EditorComponentImpl)myEditor.getContentComponent();

    int editorUpdateWidth = myEditor.getScrollPane().getHorizontalScrollBar().getValue() + visibleArea.width;
    int gutterUpdateWidth = gutter.getComponent().getWidth();
    int additionalRepaintHeight = this == myCaretModel.getPrimaryCaret() && Registry.is("editor.adjust.right.margin") && EditorPainter.isMarginShown(myEditor) ? 1 : 0;
    if ((oldY <= newY + newHeight) && (oldY + oldHeight >= newY)) { // repaint regions overlap
      int y = Math.min(oldY, newY);
      int height = Math.max(oldY + oldHeight, newY + newHeight) - y;
      content.repaintEditorComponent(0, y - additionalRepaintHeight, editorUpdateWidth, height + additionalRepaintHeight);
      gutter.getComponent().repaint(0, y, gutterUpdateWidth, height);
    }
    else {
      content.repaintEditorComponent(0, oldY - additionalRepaintHeight, editorUpdateWidth, oldHeight + additionalRepaintHeight);
      gutter.getComponent().repaint(0, oldY, gutterUpdateWidth, oldHeight);
      content.repaintEditorComponent(0, newY - additionalRepaintHeight, editorUpdateWidth, newHeight + additionalRepaintHeight);
      gutter.getComponent().repaint(0, newY, gutterUpdateWidth, newHeight);
    }
  }
}
