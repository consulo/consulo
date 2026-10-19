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
package consulo.desktop.qt.editor.impl.internal.gutter;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.ui.color.ColorValue;
import consulo.versionControlSystem.internal.VcsChangePresentation;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QPainter;
import io.qt.gui.QPen;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionImpl
public class VcsChangeQtLineMarkerPresentationPainter implements QtLineMarkerPresentationPainter<VcsChangePresentation> {
    private static final int BAR_WIDTH = 4;

    private static final int HOVER_EXTRA = 3;

    private static final int DELETION_SIZE = 5;

    @Override
    public Class<VcsChangePresentation> getPresentationType() {
        return VcsChangePresentation.class;
    }

    @Override
    public void paint(VcsChangePresentation presentation, Editor editor, QPainter painter, QRect bounds) {
        int endX = bounds.x() + bounds.width();
        int x = endX - BAR_WIDTH;
        if (presentation.hovered()) {
            x -= HOVER_EXTRA;
        }

        int y1 = bounds.y();
        int y2 = bounds.y() + bounds.height();

        switch (presentation.kind()) {
            case CHANGE -> {
                paintRect(painter, presentation.color(), null, x, y1 + 1, endX, y2 - 1);
                if (presentation.borderColor() != null) {
                    paintRect(painter, null, presentation.borderColor(), x, y1 + 1, endX, y2 - 1);
                }
            }
            case OUTLINE -> paintRect(painter, null, presentation.borderColor(), x, y1 + 1, endX, y2 - 1);
            case DELETION -> {
                int paintY = Math.max(y1, DELETION_SIZE);
                paintRect(painter, presentation.color(), presentation.color() == null ? presentation.borderColor() : null,
                    x, paintY - DELETION_SIZE + 1, endX, paintY + DELETION_SIZE - 1);
            }
        }
    }

    private static void paintRect(
        QPainter painter,
        @Nullable ColorValue color,
        @Nullable ColorValue borderColor,
        int x1,
        int y1,
        int x2,
        int y2
    ) {
        double width = x2 - x1;
        double height = y2 - y1;
        if (width <= 0 || height <= 0) {
            return;
        }

        double arc = width / 2;

        boolean antialiasing = painter.testRenderHint(QPainter.RenderHint.Antialiasing);
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
        try {
            if (color != null) {
                painter.setPen(Qt.PenStyle.NoPen);
                painter.setBrush(new QBrush(TargetQt.to(color)));
                painter.drawRoundedRect(new QRectF(x1, y1, width, height), arc, arc);
            }
            else if (borderColor != null) {
                painter.setPen(new QPen(TargetQt.to(borderColor)));
                painter.setBrush(Qt.BrushStyle.NoBrush);
                painter.drawRoundedRect(new QRectF(x1 + 0.5, y1 + 0.5, width - 1, height - 1), arc, arc);
            }
        }
        finally {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, antialiasing);
        }
    }
}
