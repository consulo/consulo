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
package consulo.desktop.awt.editor.impl;

import consulo.codeEditor.LogicalPosition;
import org.jspecify.annotations.Nullable;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Specialized TextUI intended *only* for accessibility usage. Not all the methods are called; only viewToModel, not modelToView.
 */
final class EditorAccessibilityTextUI extends TextUI {
    private final DesktopEditorImpl myEditor;

    EditorAccessibilityTextUI(DesktopEditorImpl editor) {
        myEditor = editor;
    }

    @Override
    public @Nullable Rectangle modelToView(JTextComponent tc, int offset) throws BadLocationException {
        LogicalPosition pos = myEditor.offsetToLogicalPosition(offset);
        Point point = myEditor.logicalPositionToXY(pos);
        FontMetrics fontMetrics = myEditor.getFontMetrics(Font.PLAIN);
        char c = myEditor.getDocument().getCharsSequence().subSequence(offset, offset + 1).charAt(0);
        return new Rectangle(point.x, point.y, fontMetrics.charWidth(c), fontMetrics.getHeight());
    }

    @Override
    public int viewToModel(JTextComponent tc, Point pt) {
        LogicalPosition logicalPosition = myEditor.xyToLogicalPosition(pt);
        return myEditor.logicalPositionToOffset(logicalPosition);
    }

    @Override
    public @Nullable Rectangle modelToView(JTextComponent tc, int pos, Position.Bias ignored) throws BadLocationException {
        return modelToView(tc, pos);
    }

    @Override
    public int viewToModel(JTextComponent tc, Point pt, Position.Bias[] ignored) {
        return viewToModel(tc, pt);
    }

    @Override
    public int getNextVisualPositionFrom(
        JTextComponent t,
        int pos,
        Position.Bias b,
        int direction,
        Position.Bias[] biasRet
    ) throws BadLocationException {
        DesktopEditorContentUIComponent.notSupported();
        return 0;
    }

    @Override
    public void damageRange(JTextComponent t, int p0, int p1) {
        myEditor.repaint(p0, p1);
    }

    @Override
    public void damageRange(JTextComponent t, int p0, int p1, Position.Bias ignored1, Position.Bias ignored2) {
        damageRange(t, p0, p1);
    }

    @Override
    public @Nullable EditorKit getEditorKit(JTextComponent t) {
        DesktopEditorContentUIComponent.notSupported();
        return null;
    }

    @Override
    public @Nullable View getRootView(JTextComponent t) {
        DesktopEditorContentUIComponent.notSupported();
        return null;
    }
}
