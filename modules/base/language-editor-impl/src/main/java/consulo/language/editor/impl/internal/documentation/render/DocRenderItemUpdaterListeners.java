// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.application.Application;
import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.event.FoldingListener;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.impl.FontInfo;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.ui.style.StyleManager;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
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

        MessageBusConnection connection = Application.get().getMessageBus().connect(disposable);
        connection.subscribe(EditorColorsListener.class, scheme -> DocRenderItemUpdater.updateRenderers(editor, true));
        Disposer.register(
            disposable,
            StyleManager.get().addChangeListener((oldStyle, newStyle) -> DocRenderItemUpdater.updateRenderers(editor, true))
        );

        DocRenderSelectionManager selectionManager = new DocRenderSelectionManager(editor);
        Disposer.register(disposable, selectionManager);
        DocRenderMouseEventBridge mouseEventBridge = new DocRenderMouseEventBridge(selectionManager);
        editor.addEditorMouseListener(mouseEventBridge, disposable);
        editor.addEditorMouseMotionListener(mouseEventBridge);
        Disposer.register(disposable, () -> editor.removeEditorMouseMotionListener(mouseEventBridge));
        IconVisibilityController iconVisibilityController = new IconVisibilityController();
        editor.addEditorMouseListener(iconVisibilityController, disposable);
        editor.addEditorMouseMotionListener(iconVisibilityController);
        Disposer.register(disposable, () -> editor.removeEditorMouseMotionListener(iconVisibilityController));
        editor.getScrollingModel().addVisibleAreaListener(iconVisibilityController, disposable);
        Disposer.register(disposable, iconVisibilityController);
        editor.getScrollingModel().addVisibleAreaListener(new MyVisibleAreaListener(editor), disposable);
        editor.getFoldingModel().addListener(new MyFoldingListener(), disposable);
        Disposer.register(disposable, () -> DocRenderer.clearCachedLoadingPane(editor));
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorReleased(EditorFactoryEvent event) {
                if (event.getEditor() == editor) {
                    // this ensures renderers are not kept for the released editors
                    Disposer.dispose(disposable);
                }
            }
        }, disposable);
        return disposable;
    }

    private static final class MyVisibleAreaListener implements VisibleAreaListener {
        private int lastWidth;
        private AffineTransform lastFrcTransform;

        MyVisibleAreaListener(Editor editor) {
            lastWidth = DocRenderer.calcWidth(editor);
            lastFrcTransform = getTransform(editor);
        }

        @Override
        public void visibleAreaChanged(VisibleAreaEvent e) {
            if (e.getNewRectangle().isEmpty()) {
                return; // ignore switching between tabs
            }
            Editor editor = e.getEditor();
            int newWidth = DocRenderer.calcWidth(editor);
            AffineTransform transform = getTransform(editor);
            if (newWidth != lastWidth || !transform.equals(lastFrcTransform)) {
                lastWidth = newWidth;
                lastFrcTransform = transform;
                DocRenderItemUpdater.updateRenderers(editor, false);
            }
        }

        private static AffineTransform getTransform(Editor editor) {
            return FontInfo.getFontRenderContext(editor.getContentComponent()).getTransform();
        }
    }

    private static final class MyFoldingListener implements FoldingListener {
        @Override
        public void beforeFoldRegionDisposed(FoldRegion region) {
            if (region instanceof CustomFoldRegion customFoldRegion
                && customFoldRegion.getRenderer() instanceof DocRenderer renderer) {
                renderer.dispose();
            }
        }
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
            RealEditor editor = (RealEditor) e.getEditor();
            if (editor.isCursorHidden()) {
                return;
            }
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
