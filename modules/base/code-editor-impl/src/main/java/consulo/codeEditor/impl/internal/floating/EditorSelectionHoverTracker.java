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
package consulo.codeEditor.impl.internal.floating;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.internal.EditorSelectionGestureListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
public final class EditorSelectionHoverTracker implements EditorMouseMotionListener, EditorMouseListener {
    private final Editor myEditor;
    private final EditorSelectionGestureListener myListener;

    private @Nullable Boolean myOverSelection;

    private EditorSelectionHoverTracker(Editor editor, EditorSelectionGestureListener listener) {
        myEditor = editor;
        myListener = listener;
    }

    public static void install(Editor editor, EditorSelectionGestureListener listener, Disposable parentDisposable) {
        EditorSelectionHoverTracker tracker = new EditorSelectionHoverTracker(editor, listener);
        editor.addEditorMouseListener(tracker, parentDisposable);
        editor.addEditorMouseMotionListener(tracker);
        Disposer.register(parentDisposable, () -> editor.removeEditorMouseMotionListener(tracker));
    }

    @Override
    @RequiredUIAccess
    public void mouseMoved(EditorMouseEvent e) {
        update(e.getArea() == EditorMouseEventArea.EDITING_AREA && isOverSelection(e.getVisualPosition()));
    }

    @Override
    public void mousePressed(EditorMouseEvent event) {
        myOverSelection = null;
    }

    @Override
    @RequiredUIAccess
    public void mouseExited(EditorMouseEvent event) {
        update(false);
    }

    @RequiredUIAccess
    private void update(boolean overSelection) {
        if (Boolean.valueOf(overSelection).equals(myOverSelection)) {
            return;
        }

        myOverSelection = overSelection;
        myListener.mouseOverSelectionChanged(overSelection);
    }

    private boolean isOverSelection(VisualPosition position) {
        for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
            if (caret.hasSelection()
                && caret.getSelectionEndPosition().after(position)
                && position.after(caret.getSelectionStartPosition())) {
                return true;
            }
        }
        return false;
    }
}
