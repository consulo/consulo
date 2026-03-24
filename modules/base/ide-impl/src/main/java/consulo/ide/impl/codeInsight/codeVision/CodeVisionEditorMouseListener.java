// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.CodeVisionInlayRenderer;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * Dispatches mouse press/release events on code-vision block inlays to the renderer's
 * {@link consulo.language.editor.inlay.InputHandler} methods.
 *
 * <p>Coordinate translation follows the JB {@code InlayEditorMouseMotionListener} pattern:
 * <pre>
 *   Point painterOrigin = renderer.translatePoint(new Point(bounds.x, bounds.y));
 *   Point translated    = new Point(event.x - painterOrigin.x, event.y - painterOrigin.y);
 * </pre>
 * i.e. {@code translatePoint} is called with the inlay's <em>bounds origin</em> to obtain
 * the painter's visual start on screen, then the event is made relative to that origin.
 */
@ExtensionImpl
public class CodeVisionEditorMouseListener implements EditorMouseListener {
    @Override
    public void mousePressed(EditorMouseEvent e) {
        CodeVisionInlayRenderer renderer = getRendererAt(e);
        if (renderer == null) return;
        Point translated = getTranslated(e.getInlay(), renderer, e.getMouseEvent());
        if (translated == null) return;
        renderer.mousePressed(e.getMouseEvent(), translated);
    }

    @Override
    public void mouseReleased(EditorMouseEvent e) {
        CodeVisionInlayRenderer renderer = getRendererAt(e);
        if (renderer == null) return;
        Point translated = getTranslated(e.getInlay(), renderer, e.getMouseEvent());
        if (translated == null) return;
        renderer.mouseReleased(e.getMouseEvent(), translated);
    }

    private CodeVisionInlayRenderer getRendererAt(EditorMouseEvent e) {
        if (e.isConsumed()) return null;
        if (e.getArea() != EditorMouseEventArea.EDITING_AREA) return null;
        Inlay<?> inlay = e.getInlay();
        if (inlay == null) return null;
        Object renderer = inlay.getRenderer();
        return renderer instanceof CodeVisionInlayRenderer ? (CodeVisionInlayRenderer) renderer : null;
    }

    /**
     * JB pattern from {@code InlayEditorMouseMotionListener}:
     * translate the inlay bounds origin through {@code translatePoint} to get the painter's
     * visual start, then return the event position relative to that origin.
     */
    static @org.jspecify.annotations.Nullable Point getTranslated(Inlay<?> inlay,
                                                                    CodeVisionInlayRenderer renderer,
                                                                    MouseEvent event) {
        Rectangle bounds = inlay.getBounds();
        if (bounds == null) return null;
        Point painterOrigin = renderer.translatePoint(new Point(bounds.x, bounds.y));
        return new Point(event.getX() - painterOrigin.x, event.getY() - painterOrigin.y);
    }
}
