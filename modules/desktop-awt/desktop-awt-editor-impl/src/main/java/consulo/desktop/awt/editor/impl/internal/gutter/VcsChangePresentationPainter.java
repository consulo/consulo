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
package consulo.desktop.awt.editor.impl.internal.gutter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.paint.RectanglePainter2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.versionControlSystem.internal.VcsChangePresentation;
import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * AWT look for {@link VcsChangePresentation}. Everything here is pixel constants and drawing calls — the
 * decisions about which presentations exist, in what order and in what colour live in
 * {@code VcsLineMarkerBuilder} on the AWT-free side.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionImpl
public class VcsChangePresentationPainter implements AwtLineMarkerPresentationPainter<VcsChangePresentation> {
    /**
     * Extra width revealed on hover, expanding leftwards while the right edge stays put.
     */
    private static final int HOVER_EXTRA = 3;

    @Override
    public Class<VcsChangePresentation> getPresentationType() {
        return VcsChangePresentation.class;
    }

    @Override
    public void paint(VcsChangePresentation presentation, Editor editor, Graphics2D g, Rectangle bounds) {
        // The bar is right-aligned inside the free painters area; the slack to its left is the
        // hover expansion margin.
        int barWidth = JBUI.scale(JBUI.getInt("Gutter.VcsChanges.width", 4));
        int endX = bounds.x + bounds.width;
        int x = endX - barWidth;
        if (presentation.hovered()) {
            x -= JBUI.scale(HOVER_EXTRA);
        }

        switch (presentation.kind()) {
            case CHANGE -> {
                paintRect(g, presentation.color(), null, x, bounds.y, endX, bounds.y + bounds.height);
                if (presentation.borderColor() != null) {
                    paintRect(g, null, presentation.borderColor(), x, bounds.y, endX, bounds.y + bounds.height);
                }
            }
            case OUTLINE -> paintRect(g, null, presentation.borderColor(), x, bounds.y, endX, bounds.y + bounds.height);
            case DELETION -> paintTriangle(g, editor, presentation.color(), presentation.borderColor(), x, endX, bounds.y);
        }
    }

    private static void paintRect(
        Graphics2D g,
        @Nullable ColorValue color,
        @Nullable ColorValue borderColor,
        int x1,
        int y1,
        int x2,
        int y2
    ) {
        double width = x2 - x1;
        if (color != null) {
            g.setColor(TargetAWT.to(color));
            RectanglePainter2D.FILL.paint(g, x1, y1 + 1, width, y2 - y1 - 2, width);
        }
        else if (borderColor != null) {
            g.setColor(TargetAWT.to(borderColor));
            RectanglePainter2D.DRAW.paint(g, x1, y1 + 1, width, y2 - y1 - 2, width);
        }
    }

    /**
     * Deleted lines have no height of their own, so the marker straddles the boundary. Clamped so
     * a deletion on the first line stays on screen.
     */
    private static void paintTriangle(
        Graphics2D g,
        Editor editor,
        @Nullable ColorValue color,
        @Nullable ColorValue borderColor,
        int x1,
        int x2,
        int y
    ) {
        int size = (int)JBUIScale.scale(5 * getEditorScale(editor));
        int paintY = Math.max(y, size);

        double width = x2 - x1;
        if (color != null) {
            g.setColor(TargetAWT.to(color));
            RectanglePainter2D.FILL.paint(g, x1, paintY - size + 1, width, 2 * size - 2, width);
        }
        else if (borderColor != null) {
            g.setColor(TargetAWT.to(borderColor));
            RectanglePainter2D.DRAW.paint(g, x1, paintY - size + 1, width, 2 * size - 2, width);
        }
    }

    private static float getEditorScale(Editor editor) {
        return editor instanceof RealEditor realEditor ? realEditor.getScale() : 1.0f;
    }
}
