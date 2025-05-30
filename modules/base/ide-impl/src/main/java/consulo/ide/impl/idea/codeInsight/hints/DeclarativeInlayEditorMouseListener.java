// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.SystemInfo;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

@ExtensionImpl
public class DeclarativeInlayEditorMouseListener implements EditorMouseListener {
    @Override
    public void mouseClicked(EditorMouseEvent e) {
        if (e.isConsumed()) return;
        MouseEvent event = e.getMouseEvent();
        if (e.getArea() != EditorMouseEventArea.EDITING_AREA) return;
        Inlay<?> inlay = e.getInlay();
        if (inlay == null) return;
        Object renderer = inlay.getRenderer();
        if (!(renderer instanceof DeclarativeInlayRendererBase<?>)) return;
        DeclarativeInlayRendererBase<?> baseRenderer = (DeclarativeInlayRendererBase<?>) renderer;
        Rectangle bounds = inlay.getBounds();
        if (bounds == null) return;
        Point inlayPoint = new Point(bounds.x, bounds.y);
        Point translated = new Point(event.getX() - inlayPoint.x, event.getY() - inlayPoint.y);
        if (SwingUtilities.isRightMouseButton(event) && !SwingUtilities.isLeftMouseButton(event)) {
            baseRenderer.handleRightClick(e, translated);
            return;
        }
        boolean controlDown = isControlDown(event);
        baseRenderer.handleLeftClick(e, translated, controlDown);
        inlay.update();
    }

    @Override
    public void mouseReleased(EditorMouseEvent e) {
        if (e.getEditor() instanceof EditorEx editorEx) {
            editorEx.setCustomCursor(DeclarativeInlayHintsMouseMotionListener.class, null);
        }
    }

    private boolean isControlDown(MouseEvent e) {
        return (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown();
    }
}
