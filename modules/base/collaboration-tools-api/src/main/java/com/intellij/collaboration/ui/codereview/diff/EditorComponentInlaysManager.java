// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import com.intellij.collaboration.ui.codereview.CodeReviewChatItemUIUtil;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.util.EditorUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @deprecated Deprecated in favour of using inlays directly -
 * com.intellij.collaboration.ui.codereview.editor.EditorComponentInlaysUtilKt.insertComponentAfter
 */
@Deprecated
public final class EditorComponentInlaysManager implements Disposable {

    private static final int PREFERRED_INLAY_WIDTH = CodeReviewChatItemUIUtil.TEXT_CONTENT_WIDTH + 52;

    private final @Nonnull EditorImpl editor;
    private final @Nonnull Map<ComponentWrapper, Disposable> managedInlays = new ConcurrentHashMap<>();
    private final @Nonnull EditorTextWidthWatcher editorWidthWatcher;

    public EditorComponentInlaysManager(@Nonnull EditorImpl editor) {
        this.editor = editor;
        this.editorWidthWatcher = new EditorTextWidthWatcher();

        editor.getScrollPane().getViewport().addComponentListener(editorWidthWatcher);
        Disposer.register(this, () -> editor.getScrollPane().getViewport().removeComponentListener(editorWidthWatcher));

        EditorUtil.disposeWithEditor(editor, this);
    }

    public @Nonnull EditorImpl getEditor() {
        return editor;
    }

    /**
     * @param priority impacts the visual order in which inlays are displayed. Components with higher priority will be shown higher
     */
    @RequiresEdt
    public @Nullable Inlay<?> insertAfter(int lineIndex, @Nonnull JComponent component) {
        return insertAfter(lineIndex, component, 0, null);
    }

    @RequiresEdt
    public @Nullable Inlay<?> insertAfter(int lineIndex, @Nonnull JComponent component, int priority) {
        return insertAfter(lineIndex, component, priority, null);
    }

    @RequiresEdt
    public @Nullable Inlay<?> insertAfter(
        int lineIndex,
        @Nonnull JComponent component,
        int priority,
        @Nullable RendererFactory rendererFactory
    ) {
        if (Disposer.isDisposed(this)) {
            return null;
        }

        ComponentWrapper wrappedComponent = new ComponentWrapper(component);
        int offset = editor.getDocument().getLineEndOffset(lineIndex);

        Inlay<?> inlay = EditorEmbeddedComponentManager.getInstance().addComponent(
            editor,
            wrappedComponent,
            new EditorEmbeddedComponentManager.Properties(
                EditorEmbeddedComponentManager.ResizePolicy.none(),
                rendererFactory,
                false,
                false,
                priority,
                offset
            )
        );
        if (inlay != null) {
            managedInlays.put(wrappedComponent, inlay);
            Disposer.register(inlay, () -> managedInlays.remove(wrappedComponent));
        }
        return inlay;
    }

    @Override
    public void dispose() {
        for (Disposable inlay : managedInlays.values()) {
            Disposer.dispose(inlay);
        }
    }

    private final class ComponentWrapper extends BorderLayoutPanel {
        private final @Nonnull JComponent component;

        ComponentWrapper(@Nonnull JComponent component) {
            this.component = component;
            setOpaque(false);
            setBorder(JBUI.Borders.empty());
            addToCenter(component);

            component.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    dispatchEvent(new ComponentEvent(component, ComponentEvent.COMPONENT_RESIZED));
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(editorWidthWatcher.editorTextWidth, component.getPreferredSize().height);
        }
    }

    private final class EditorTextWidthWatcher extends ComponentAdapter {
        int editorTextWidth = 0;
        private final boolean verticalScrollbarFlipped;

        EditorTextWidthWatcher() {
            Object scrollbarFlip = editor.getScrollPane().getClientProperty(JBScrollPane.Flip.class);
            verticalScrollbarFlipped = scrollbarFlip == JBScrollPane.Flip.HORIZONTAL || scrollbarFlip == JBScrollPane.Flip.BOTH;
            updateWidthForAllInlays();
        }

        @Override
        public void componentResized(ComponentEvent e) {
            updateWidthForAllInlays();
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            updateWidthForAllInlays();
        }

        @Override
        public void componentShown(ComponentEvent e) {
            updateWidthForAllInlays();
        }

        private void updateWidthForAllInlays() {
            int newWidth = calcWidth();
            if (editorTextWidth == newWidth) {
                return;
            }
            editorTextWidth = newWidth;

            for (ComponentWrapper wrapper : managedInlays.keySet()) {
                wrapper.dispatchEvent(new ComponentEvent(wrapper, ComponentEvent.COMPONENT_RESIZED));
                wrapper.invalidate();
            }
        }

        private int calcWidth() {
            int visibleEditorTextWidth = editor.getScrollPane().getViewport().getWidth() - getVerticalScrollbarWidth() - getGutterTextGap();
            return Math.min(Math.max(visibleEditorTextWidth, 0), JBUI.scale(PREFERRED_INLAY_WIDTH));
        }

        private int getVerticalScrollbarWidth() {
            int width = editor.getScrollPane().getVerticalScrollBar().getWidth();
            return verticalScrollbarFlipped ? width : width * 2;
        }

        private int getGutterTextGap() {
            if (verticalScrollbarFlipped) {
                var gutter = ((EditorEx) editor).getGutterComponentEx();
                return gutter.getWidth() - gutter.getWhitespaceSeparatorOffset();
            }
            return 0;
        }
    }
}
