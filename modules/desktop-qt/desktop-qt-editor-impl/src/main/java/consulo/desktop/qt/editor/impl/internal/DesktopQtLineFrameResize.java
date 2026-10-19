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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.impl.LineFrameEdgeHit;
import consulo.codeEditor.impl.LineFrameResizeGesture;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.core.QPoint;
import io.qt.gui.QMouseEvent;
import io.qt.widgets.QToolTip;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public final class DesktopQtLineFrameResize {
    private final DesktopQtEditorImpl myEditor;
    private final LineFrameResizeGesture myGesture;

    private boolean myTooltipShown;

    public DesktopQtLineFrameResize(DesktopQtEditorImpl editor) {
        myEditor = editor;
        myGesture = new LineFrameResizeGesture(editor);
    }

    public boolean isDragging() {
        return myGesture.isDragging();
    }

    public boolean isTooltipShown() {
        return myTooltipShown;
    }

    @RequiredUIAccess
    public boolean pressed(int y) {
        if (!myGesture.pressed(y)) {
            return false;
        }
        hideTooltip();
        return true;
    }

    @RequiredUIAccess
    public boolean dragged(int y) {
        return myGesture.dragged(y);
    }

    @RequiredUIAccess
    public boolean released() {
        return myGesture.released();
    }

    @RequiredUIAccess
    public void scrolled(int deltaY) {
        myGesture.scrolled(deltaY);
    }

    @RequiredUIAccess
    public @Nullable LineFrameEdgeHit moved(QWidget widget, QMouseEvent event, int y) {
        LineFrameEdgeHit hit = myGesture.moved(y);
        if (hit == null) {
            hideTooltip();
            return null;
        }
        QPoint global = event.globalPosition().toPoint();
        QToolTip.showText(new QPoint(global.x(), global.y() + myEditor.getLineHeight()), hit.resize().tooltip().get(), widget);
        myTooltipShown = true;
        return hit;
    }

    @RequiredUIAccess
    public void exited() {
        myGesture.exited();
        hideTooltip();
    }

    private void hideTooltip() {
        if (myTooltipShown) {
            myTooltipShown = false;
            QToolTip.hideText();
        }
    }
}
