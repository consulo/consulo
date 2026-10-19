// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.desktop.awt.editor.impl.internal.inlay;

import consulo.codeEditor.ComponentInlayAlignment;
import consulo.codeEditor.ComponentInlayRenderer;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.FoldingListener;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.util.EditorUtil;
import consulo.desktop.awt.ui.impl.DesktopLength;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Length;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for component inlays. Has one instance of the container per editor.
 * Conducting all synchronization between components and associated inlays, for components works as a regular
 * AWT container.
 * <p>
 * For components supports different alignment modes:
 * <ul>
 * <li>null - uses preferred width as an inlay width</li>
 * <li>{@link ComponentInlayAlignment#STRETCH_TO_CONTENT_WIDTH} - uses preferred width as an inlay width, sets
 * component width to editor content width during layout</li>
 * <li>{@link ComponentInlayAlignment#FIT_CONTENT_WIDTH} - uses min width as an inlay width, sets component width
 * to editor content width during layout</li>
 * <li>{@link ComponentInlayAlignment#FIT_VIEWPORT_WIDTH} and {@link ComponentInlayAlignment#FIT_VIEWPORT_X_SPAN} -
 * uses min width as an inlay width, sets component width to viewport width. For X Span shifts component to
 * viewport.</li>
 * </ul>
 * Inlay width incorporated during content preferred width calculation and for that reason we can't sync inlay
 * width with component actual width when alignment used, otherwise editor content won't shrink when text changed
 * or viewport changed.
 * I.e. we have component preferred width 10 and editor content width 100 (because of viewport) and
 * FIT_CONTENT_WIDTH alignment, after layout component's with will be 100 and if inlay width also 100 then after
 * shrinking viewport to 80 the content width still will be 100 (because of inlay). But if we set inlay width to
 * preferred component width 10 then it will adjust to viewport size and resize component to width 80.
 */
public final class ComponentInlaysContainer extends JComponent implements Disposable {
    private static final Key<ComponentInlaysContainer> INLAYS_CONTAINER = Key.create("INLAYS_CONTAINER");

    private final EditorEx myEditor;

    private int myVisibleAreaAwareInlaysCount;
    private int myContentSizeAwareInlayCount;

    private final List<Inlay<? extends ComponentInlayRenderer>> myInlays = new ArrayList<>();

    private final ComponentAdapter myContentResizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            revalidate();
            repaint();
        }
    };

    private final VisibleAreaListener myVisibleAreaListener = e -> {
        revalidate();
        repaint();
    };

    private final FoldingListener myFoldingListener = new FoldingListener() {
        @Override
        public void onFoldProcessingEnd() {
            revalidate();
            repaint();
        }
    };

    private ComponentInlaysContainer(EditorEx editor) {
        myEditor = editor;
        editor.getFoldingModel().addListener(myFoldingListener, this);
    }

    @SuppressWarnings("unchecked")
    public static void addInlay(Inlay<?> anyInlay) {
        Inlay<? extends ComponentInlayRenderer> inlay = (Inlay<? extends ComponentInlayRenderer>) anyInlay;
        EditorEx editor = (EditorEx)inlay.getEditor();
        if (editor.isDisposed()) {
            return;
        }
        ComponentInlaysContainer container = editor.getUserData(INLAYS_CONTAINER);
        if (container == null) {
            container = new ComponentInlaysContainer(editor);
            ComponentInlaysContainer created = container;
            editor.putUserData(INLAYS_CONTAINER, created);
            editor.getContentComponent().add(created);
            Disposer.register(created, () -> {
                editor.getContentComponent().remove(created);
                editor.putUserData(INLAYS_CONTAINER, null);
            });
            EditorUtil.disposeWithEditor(editor, created);
        }

        container.addInlayImpl(inlay);
        ComponentInlaysContainer target = container;
        Disposer.register(target, inlay);
        Disposer.register(inlay, () -> {
            // auto-dispose container when last inlay removed
            if (target.removeInlayImpl(inlay) && target.myInlays.isEmpty()) {
                Disposer.dispose(target);
            }
        });
    }

    private void addInlayImpl(Inlay<? extends ComponentInlayRenderer> inlay) {
        ComponentInlayRenderer renderer = inlay.getRenderer();
        Component component = TargetAWT.to(renderer.getComponent());
        myInlays.add(inlay);
        add(component);
        component.setVisible(inlay.getBounds() != null);

        ComponentInlayAlignment alignment = renderer.getAlignment();
        // optionally add listeners if any inlay aware of viewport or content size
        if (alignment == ComponentInlayAlignment.FIT_CONTENT_WIDTH
            || alignment == ComponentInlayAlignment.STRETCH_TO_CONTENT_WIDTH) {
            if (myContentSizeAwareInlayCount++ == 0) {
                myEditor.getContentComponent().addComponentListener(myContentResizeListener);
            }
        }
        else if (alignment == ComponentInlayAlignment.FIT_VIEWPORT_X_SPAN
            || alignment == ComponentInlayAlignment.FIT_VIEWPORT_WIDTH) {
            if (myVisibleAreaAwareInlaysCount++ == 0) {
                myEditor.getScrollingModel().addVisibleAreaListener(myVisibleAreaListener);
            }
        }

        if (component.isVisible()) {
            revalidate();
            repaint();
        }
    }

    private boolean removeInlayImpl(Inlay<? extends ComponentInlayRenderer> inlay) {
        if (!myInlays.remove(inlay)) {
            return false;
        }

        ComponentInlayRenderer renderer = inlay.getRenderer();
        ComponentInlayAlignment alignment = renderer.getAlignment();
        if (alignment == ComponentInlayAlignment.FIT_CONTENT_WIDTH
            || alignment == ComponentInlayAlignment.STRETCH_TO_CONTENT_WIDTH) {
            if (--myContentSizeAwareInlayCount == 0) {
                myEditor.getContentComponent().removeComponentListener(myContentResizeListener);
            }
        }
        else if (alignment == ComponentInlayAlignment.FIT_VIEWPORT_X_SPAN
            || alignment == ComponentInlayAlignment.FIT_VIEWPORT_WIDTH) {
            if (--myVisibleAreaAwareInlaysCount == 0) {
                myEditor.getScrollingModel().removeVisibleAreaListener(myVisibleAreaListener);
            }
        }
        remove(TargetAWT.to(renderer.getComponent()));
        if (!myEditor.isDisposed()) {
            revalidate();
            repaint();
        }
        return true;
    }

    @Override
    public void invalidate() {
        if (!isValid()) {
            return;
        }

        super.invalidate();
        // Effectively revalidate InlayComponent when invalidated.
        // Need to add to RepaintManager because otherwise component may not be validated until parent revalidate
        // happen. The way how Swing works it always invalidates parent component, but uses first validation root
        // in hierarchy for repaint. I.e. for editor -> scrollPane (validationRoot1) -> inlayComponent
        // (validationRoot2) for editor.revalidate() it will mark inlayComponent as invalid, but only validate
        // scrollPane on repaint. We then have to wait for another revalidate call in hierarchy starting from
        // inlayComponent.
        RepaintManager.currentManager(this).addInvalidComponent(this);
    }

    @Override
    public boolean isValidateRoot() {
        return true;
    }

    @Override
    public void doLayout() {
        if (myInlays.isEmpty()) {
            return;
        }

        JComponent content = myEditor.getContentComponent();
        int initialContentWidth = content.getWidth();
        JScrollPane scrollPane = myEditor.getScrollPane();
        int viewportReservedWidth = scrollPane.getVerticalScrollBar().getWidth() + content.getInsets().left;
        int viewportWidth = scrollPane.getViewport().getWidth();
        int contentXInViewport = scrollPane.getViewport().getViewPosition().x;

        // Step 1: Sync inlay size with preferred component size.
        // Step 1.1: Update inlay size, it may fail in batch mode so need to do it in separate loop
        for (Inlay<? extends ComponentInlayRenderer> inlay : myInlays) {
            ComponentInlayRenderer renderer = inlay.getRenderer();
            Component component = TargetAWT.to(renderer.getComponent());
            ComponentInlayAlignment alignment = renderer.getAlignment();
            Dimension size;
            if (alignment == ComponentInlayAlignment.FIT_CONTENT_WIDTH) {
                size = new Dimension(component.getMinimumSize().width, component.getPreferredSize().height);
            }
            else if (alignment == ComponentInlayAlignment.FIT_VIEWPORT_X_SPAN
                || alignment == ComponentInlayAlignment.FIT_VIEWPORT_WIDTH) {
                size = new Dimension(component.getMinimumSize().width + viewportReservedWidth,
                    component.getPreferredSize().height);
            }
            else {
                size = component.getPreferredSize();
            }
            renderer.setInlaySize(size.width, size.height);
        }

        // Step 1.2: Update all inlays in a single batch for performance reasons
        myEditor.getInlayModel().execute(true, () -> {
            for (Inlay<? extends ComponentInlayRenderer> inlay : myInlays) {
                ComponentInlayRenderer renderer = inlay.getRenderer();
                if (renderer.calcWidthInPixels(inlay) != inlay.getWidthInPixels()
                    || renderer.calcHeightInPixels(inlay) != inlay.getHeightInPixels()) {
                    inlay.update();
                }
            }
        });

        // Step 2: Way editor implemented now it will validate size after inlay update and set it to preferred
        // size which may be less than viewport. We ask parent to layout editor in that case to restore it's size.
        if (content.getWidth() < initialContentWidth && content.getWidth() < viewportWidth) {
            content.setSize(new Dimension(Math.min(initialContentWidth, viewportWidth), content.getHeight()));
        }

        // Step 3: Set bounds of container to bounds of inner area (without insets) of editor.
        // It defines a viewport for inlay components.
        setBounds(SwingUtilities.calculateInnerArea(content, null));

        // Step 4: Layout inlay components
        for (Inlay<? extends ComponentInlayRenderer> inlay : myInlays) {
            ComponentInlayRenderer renderer = inlay.getRenderer();
            Component component = TargetAWT.to(renderer.getComponent());
            Rectangle componentBounds = inlay.getBounds();

            if (componentBounds == null) {
                component.setVisible(false);
                continue;
            }

            component.setVisible(true);
            ComponentInlayAlignment alignment = renderer.getAlignment();

            if (alignment == ComponentInlayAlignment.FIT_VIEWPORT_X_SPAN) {
                // x in inlay bounds contains left gap of content, which we do not need
                componentBounds.x = contentXInViewport;
            }
            else {
                componentBounds.x = 0;
            }

            if (alignment == ComponentInlayAlignment.STRETCH_TO_CONTENT_WIDTH
                || alignment == ComponentInlayAlignment.FIT_CONTENT_WIDTH) {
                componentBounds.width = getBounds().width;
            }
            else if (alignment == ComponentInlayAlignment.FIT_VIEWPORT_WIDTH
                || alignment == ComponentInlayAlignment.FIT_VIEWPORT_X_SPAN) {
                componentBounds.width = Math.max(component.getMinimumSize().width, viewportWidth - viewportReservedWidth);
            }

            Length maxWidth = renderer.getMaxWidth();
            if (maxWidth != null) {
                componentBounds.width = Math.min(componentBounds.width, DesktopLength.toPixels(component, maxWidth));
            }

            component.setBounds(componentBounds);
        }
    }

    @Override
    public void dispose() {
    }
}
