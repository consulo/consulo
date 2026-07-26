// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public final class DocRenderItemUpdaterListeners {
    private static final Key<Disposable> LISTENERS_DISPOSABLE = Key.create("doc.render.listeners.disposable");

    private DocRenderItemUpdaterListeners() {
    }

    public static void disposeListeners(Editor editor) {
        Disposable existingDisposable = editor.getUserData(LISTENERS_DISPOSABLE);
        if (existingDisposable == null) {
            return;
        }
        Disposer.dispose(existingDisposable);
        editor.putUserData(LISTENERS_DISPOSABLE, null);
    }

    public static @Nullable Disposable setupListeners(Editor editor) {
        Disposable existingDisposable = editor.getUserData(LISTENERS_DISPOSABLE);
        if (existingDisposable != null) {
            return null;
        }

        Disposable disposable = Disposable.newDisposable("DocRenderItemUpdaterListeners");
        editor.putUserData(LISTENERS_DISPOSABLE, disposable);

        IconVisibilityController iconVisibilityController = new IconVisibilityController();
        editor.addEditorMouseListener(iconVisibilityController, disposable);
        editor.addEditorMouseMotionListener(iconVisibilityController);
        Disposer.register(disposable, () -> editor.removeEditorMouseMotionListener(iconVisibilityController));
        editor.getScrollingModel().addVisibleAreaListener(iconVisibilityController, disposable);
        Disposer.register(disposable, iconVisibilityController);
        return disposable;
    }

    private static final class IconVisibilityController
        implements EditorMouseListener, EditorMouseMotionListener, VisibleAreaListener, Disposable {
        private DocRenderItem myCurrentItem;
        private Editor myQueuedEditor;

        @Override
        public void mouseMoved(EditorMouseEvent e) {
            doUpdate(e.getEditor(), e);
        }

        @Override
        public void mouseExited(EditorMouseEvent e) {
            doUpdate(e.getEditor(), e);
        }

        @Override
        public void visibleAreaChanged(VisibleAreaEvent e) {
            Editor editor = e.getEditor();
            if (myQueuedEditor == null) {
                myQueuedEditor = editor;
                // delay update: multiple visible area updates within same EDT event will cause only one icon update,
                // and we'll not observe the item in inconsistent state during toggling
                SwingUtilities.invokeLater(() -> {
                    if (myQueuedEditor != null && !myQueuedEditor.isDisposed()) {
                        doUpdate(myQueuedEditor, null);
                    }
                    myQueuedEditor = null;
                });
            }
        }

        private void doUpdate(Editor editor, @Nullable EditorMouseEvent event) {
            int y = 0;
            int offset = -1;
            if (event == null) {
                PointerInfo info = MouseInfo.getPointerInfo();
                if (info != null) {
                    Point screenPoint = info.getLocation();
                    Component component = editor.getComponent();
                    Point componentPoint = new Point(screenPoint);
                    SwingUtilities.convertPointFromScreen(componentPoint, component);
                    if (new Rectangle(component.getSize()).contains(componentPoint)) {
                        Point editorPoint = new Point(screenPoint);
                        SwingUtilities.convertPointFromScreen(editorPoint, editor.getContentComponent());
                        y = editorPoint.y;
                        offset = editor.visualPositionToOffset(new VisualPosition(editor.yToVisualLine(y), 0));
                    }
                }
            }
            else {
                y = event.getMouseEvent().getY();
                offset = event.getOffset();
            }
            DocRenderItem item = offset < 0 ? null : findItem(editor, y, offset);
            if (item != myCurrentItem) {
                if (myCurrentItem != null) {
                    myCurrentItem.setIconVisible(false);
                }
                myCurrentItem = item;
                if (myCurrentItem != null) {
                    myCurrentItem.setIconVisible(true);
                }
            }
        }

        @Override
        public void dispose() {
            myCurrentItem = null;
            myQueuedEditor = null;
        }

        private static @Nullable DocRenderItem findItem(Editor editor, int y, int neighborOffset) {
            Document document = editor.getDocument();
            int lineNumber = document.getLineNumber(neighborOffset);
            int searchStartOffset = document.getLineStartOffset(Math.max(0, lineNumber - 1));
            int searchEndOffset = document.getLineEndOffset(lineNumber);
            Collection<DocRenderItemImpl> items = DocRenderItemManager.getInstance().getItems(editor);
            if (items == null) {
                return null;
            }
            for (DocRenderItemImpl item : items) {
                RangeHighlighter highlighter = item.getHighlighter();
                if (highlighter.isValid()
                    && highlighter.getStartOffset() <= searchEndOffset
                    && highlighter.getEndOffset() >= searchStartOffset) {
                    int itemStartY = 0;
                    int itemEndY = 0;
                    CustomFoldRegion foldRegion = item.getFoldRegion();
                    if (foldRegion == null) {
                        RealEditor realEditor = (RealEditor) editor;
                        itemStartY = editor.visualLineToY(realEditor.offsetToVisualLine(highlighter.getStartOffset(), false));
                        itemEndY = editor.visualLineToY(realEditor.offsetToVisualLine(highlighter.getEndOffset(), true))
                            + editor.getLineHeight();
                    }
                    else {
                        Point location = foldRegion.getLocation();
                        if (location != null) {
                            itemStartY = location.y;
                            itemEndY = itemStartY + foldRegion.getHeightInPixels();
                        }
                    }
                    if (y >= itemStartY && y < itemEndY) {
                        return item;
                    }
                    break;
                }
            }
            return null;
        }
    }
}
