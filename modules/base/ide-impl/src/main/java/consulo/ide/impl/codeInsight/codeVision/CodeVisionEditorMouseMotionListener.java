// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.CodeVisionInlayRenderer;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;

/**
 * Tracks mouse motion over code-vision block inlays and dispatches hover events to the
 * renderer's {@link consulo.language.editor.inlay.InputHandler} methods.
 *
 * <p>Coordinate translation follows the JB {@code InlayEditorMouseMotionListener} pattern —
 * see {@link CodeVisionEditorMouseListener#getTranslated}.
 */
@ExtensionImpl
public class CodeVisionEditorMouseMotionListener implements EditorMouseMotionListener {
    private WeakReference<Inlay<?>> lastInlayUnderCursor;

    @Override
    public void mouseMoved(EditorMouseEvent e) {
        if (e.isConsumed()) return;
        if (e.getArea() != EditorMouseEventArea.EDITING_AREA) return;
        Inlay<?> currentInlay = e.getInlay();
        Inlay<?> lastInlay = lastInlayUnderCursor == null ? null : lastInlayUnderCursor.get();

        // Fire mouseExited on previous inlay if the cursor moved off it
        if (lastInlay != null && lastInlay != currentInlay) {
            Object r = lastInlay.getRenderer();
            if (r instanceof CodeVisionInlayRenderer cvRenderer) {
                cvRenderer.mouseExited();
            }
        }

        if (currentInlay == null) {
            lastInlayUnderCursor = null;
            return;
        }
        Object r = currentInlay.getRenderer();
        if (!(r instanceof CodeVisionInlayRenderer cvRenderer)) {
            lastInlayUnderCursor = null;
            return;
        }
        MouseEvent event = e.getMouseEvent();
        Point translated = CodeVisionEditorMouseListener.getTranslated(currentInlay, cvRenderer, event);
        if (translated != null) {
            cvRenderer.mouseMoved(event, translated);
        }
        lastInlayUnderCursor = new WeakReference<>(currentInlay);
    }

    @Override
    public void mouseDragged(EditorMouseEvent e) {
        // no-op
    }
}
