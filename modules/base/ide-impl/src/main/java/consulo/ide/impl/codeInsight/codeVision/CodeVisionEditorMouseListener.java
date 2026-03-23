// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;

import java.awt.event.MouseEvent;

/**
 * Dispatches mouse-click events on code-vision block inlays to the owning renderer.
 */
@ExtensionImpl
public class CodeVisionEditorMouseListener implements EditorMouseListener {
    @Override
    public void mouseClicked(EditorMouseEvent e) {
        if (e.isConsumed()) return;
        if (e.getArea() != EditorMouseEventArea.EDITING_AREA) return;
        Inlay<?> inlay = e.getInlay();
        if (inlay == null) return;
        Object renderer = inlay.getRenderer();
        if (!(renderer instanceof CodeVisionBlockInlayRenderer cvRenderer)) return;
        MouseEvent event = e.getMouseEvent();
        cvRenderer.handleClick(event, e.getEditor());
        e.consume();
    }
}
