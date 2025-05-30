// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.SystemInfo;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.LightweightHint;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class DeclarativeInlayHintsMouseMotionListener implements EditorMouseMotionListener {
    private InlayMouseArea areaUnderCursor;
    private WeakReference<Inlay<?>> inlayUnderCursor;
    private DeclarativeInlayHintsKeyListener inlayKeyListener;
    private boolean ctrlDown;
    private LightweightHint hint;

    @RequiredUIAccess
    @Override
    public void mouseMoved(EditorMouseEvent e) {
        Inlay<?> inlay = getInlay(e);
        DeclarativeInlayRendererBase<?> renderer = inlay == null ? null : getRenderer(inlay);
        InlayMouseArea mouseArea = (renderer == null || inlay == null)
            ? null
            : getMouseAreaUnderCursor(inlay, renderer, e.getMouseEvent());
        boolean ctrlDownNow = isControlDown(e.getMouseEvent());

        if (inlay != (inlayUnderCursor == null ? null : inlayUnderCursor.get())) {
            if (hint != null) hint.hide();
            if (renderer != null) {
                Rectangle bounds = inlay.getBounds();
                if (bounds != null) {
                    Point translated = new Point(e.getMouseEvent().getX() - bounds.x,
                        e.getMouseEvent().getY() - bounds.y);
                    hint = renderer.handleHover(e, translated);
                }
            }
            else {
                hint = null;
            }
        }

        boolean movedToAnother = mouseArea != areaUnderCursor;
        boolean ctrlChanged = ctrlDownNow != this.ctrlDown;
        if (movedToAnother || ctrlChanged) {
            List<InlayPresentationEntry> oldEntries =
                areaUnderCursor == null ? null : areaUnderCursor.getEntries();
            if (oldEntries != null && movedToAnother) {
                for (InlayPresentationEntry entry : oldEntries) {
                    entry.setHoveredWithCtrl(false);
                }
            }

            List<InlayPresentationEntry> newEntries =
                mouseArea == null ? Collections.emptyList() : mouseArea.getEntries();
            for (InlayPresentationEntry entry : newEntries) {
                entry.setHoveredWithCtrl(ctrlDownNow);
            }

            if (ctrlDownNow && !newEntries.isEmpty()) {
                if (e.getEditor() instanceof EditorEx) {
                    ((EditorEx) e.getEditor())
                        .setCustomCursor(DeclarativeInlayHintsMouseMotionListener.class,
                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
            else {
                if (e.getEditor() instanceof EditorEx) {
                    ((EditorEx) e.getEditor())
                        .setCustomCursor(DeclarativeInlayHintsMouseMotionListener.class, null);
                }
            }

            if (inlayUnderCursor != null && inlayUnderCursor.get() != null) {
                inlayUnderCursor.get().update();
            }
            if (inlay != null) {
                inlay.update();
            }

            this.areaUnderCursor = mouseArea;
            this.ctrlDown = ctrlDownNow;
        }

        if (inlay != (inlayUnderCursor == null ? null : inlayUnderCursor.get())) {
            inlayUnderCursor = inlay == null ? null : new WeakReference<>(inlay);
            if (inlayKeyListener != null) {
                Disposer.dispose(inlayKeyListener);
            }
            inlayKeyListener = null;

            if (inlay != null && inlay.getEditor() instanceof EditorEx) {
                EditorEx editor = (EditorEx) inlay.getEditor();
                DeclarativeInlayHintsKeyListener listener = new DeclarativeInlayHintsKeyListener(editor);
                editor.getContentComponent().addKeyListener(listener);
                EditorUtil.disposeWithEditor(editor, listener);
                inlayKeyListener = listener;
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void mouseDragged(EditorMouseEvent e) {
        // no-op
    }

    private boolean isControlDown(InputEvent e) {
        return (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown();
    }

    private DeclarativeInlayRendererBase<?> getRenderer(Inlay<?> inlay) {
        Object r = inlay.getRenderer();
        return r instanceof DeclarativeInlayRendererBase
            ? (DeclarativeInlayRendererBase<?>) r
            : null;
    }

    private Inlay<?> getInlay(EditorMouseEvent e) {
        if (e.isConsumed()) return null;
        if (e.getArea() != EditorMouseEventArea.EDITING_AREA) return null;
        return e.getInlay();
    }

    private InlayMouseArea getMouseAreaUnderCursor(Inlay<?> inlay,
                                                   DeclarativeInlayRendererBase<?> renderer,
                                                   MouseEvent event) {
        Rectangle bounds = inlay.getBounds();
        if (bounds == null) return null;
        Point origin = new Point(bounds.x, bounds.y);
        Point translated = new Point(event.getX() - origin.x,
            event.getY() - origin.y);
        return renderer.getMouseArea(translated);
    }

    private class DeclarativeInlayHintsKeyListener extends KeyAdapter implements Disposable {
        private final EditorEx editor;

        DeclarativeInlayHintsKeyListener(EditorEx editor) {
            this.editor = editor;
        }

        @Override
        public void dispose() {
            editor.getContentComponent().removeKeyListener(this);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e != null && !isControlDown(e)) {
                editor.setCustomCursor(DeclarativeInlayHintsMouseMotionListener.class, null);

                if (areaUnderCursor != null) {
                    for (InlayPresentationEntry entry : areaUnderCursor.getEntries()) {
                        entry.setHoveredWithCtrl(false);
                    }
                    if (inlayUnderCursor != null && inlayUnderCursor.get() != null) {
                        inlayUnderCursor.get().update();
                    }
                }
            }
        }

        private boolean isControlDown(InputEvent e) {
            return (SystemInfo.isMac && e.isMetaDown()) || e.isControlDown();
        }
    }
}
