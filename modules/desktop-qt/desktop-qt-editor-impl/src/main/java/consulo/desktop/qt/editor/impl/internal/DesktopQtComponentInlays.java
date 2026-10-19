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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.ComponentInlayRenderer;
import consulo.codeEditor.Inlay;
import consulo.desktop.qt.ui.impl.DesktopQtLength;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.document.Document;
import consulo.ui.Length;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QTimer;
import io.qt.widgets.QLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The live components of component inlays: each stands in the viewport over the room its inlay takes, as wide as the
 * viewport allows, and the inlay takes as much room as the component asks for at that width.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class DesktopQtComponentInlays {
    private final DesktopQtEditorImpl myEditor;
    private final Map<Inlay<?>, QWidget> myWidgets = new LinkedHashMap<>();

    private boolean myLayingOut;
    private boolean myLayoutScheduled;

    public DesktopQtComponentInlays(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    /**
     * Hosts the component inlays added before the editor had a surface to host them on.
     */
    public void bound() {
        Document document = myEditor.getDocument();
        for (Inlay<?> inlay : myEditor.getInlayModel().getBlockElementsInRange(0, document.getTextLength())) {
            if (!myWidgets.containsKey(inlay)) {
                added(inlay);
            }
        }
    }

    public void scheduleLayout() {
        if (myLayoutScheduled) {
            return;
        }
        myLayoutScheduled = true;
        QTimer.singleShot(0, () -> {
            myLayoutScheduled = false;
            if (!myEditor.isDisposed()) {
                layout();
            }
        });
    }

    public void added(Inlay<?> inlay) {
        if (!(inlay.getRenderer() instanceof ComponentInlayRenderer renderer)) {
            return;
        }
        DesktopQtEditorWidget surface = myEditor.getSurface();
        if (surface == null || !(renderer.getComponent() instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        QWidget viewport = surface.viewport();
        delegate.setParent(myEditor.getContentUIComponent());
        delegate.bind(viewport, null);

        QWidget widget = delegate.toQtComponent();
        if (widget == null) {
            return;
        }

        // the component asks for a new size when its content changes - a thread unfolding its replies
        widget.installEventFilter(new QObject(widget) {
            @Override
            public boolean eventFilter(@Nullable QObject watched, @Nullable QEvent event) {
                if (event != null && event.type() == QEvent.Type.LayoutRequest) {
                    scheduleLayout();
                }
                return false;
            }
        });

        myWidgets.put(inlay, widget);
        widget.show();
        widget.raise();
        layout();
    }

    public void removed(Inlay<?> inlay) {
        myWidgets.remove(inlay);
        if (inlay.getRenderer() instanceof ComponentInlayRenderer renderer
            && renderer.getComponent() instanceof QtComponentDelegate<?> delegate) {
            delegate.disposeQt();
        }
    }

    /**
     * Measures every component against the width it gets, gives its inlay that much room, and puts it over the room.
     */
    public void layout() {
        if (myLayingOut || myWidgets.isEmpty()) {
            return;
        }
        DesktopQtEditorWidget surface = myEditor.getSurface();
        if (surface == null) {
            return;
        }

        myLayingOut = true;
        try {
            int viewportWidth = surface.viewport().width();
            boolean resized = false;

            for (Map.Entry<Inlay<?>, QWidget> entry : myWidgets.entrySet()) {
                Inlay<?> inlay = entry.getKey();
                QWidget widget = entry.getValue();
                if (!inlay.isValid() || widget.isDisposed() || !(inlay.getRenderer() instanceof ComponentInlayRenderer renderer)) {
                    continue;
                }

                int width = Math.max(widget.minimumSizeHint().width(), viewportWidth);
                Length maxWidth = renderer.getMaxWidth();
                if (maxWidth != null) {
                    width = Math.min(width, DesktopQtLength.toPixels(widget, maxWidth));
                }
                // laid out at the width it gets before it is asked how tall it is - wrapping text answers for the
                // width it has, and a widget not laid out yet has the narrow one
                if (widget.width() != width) {
                    widget.resize(width, Math.max(1, widget.height()));
                }
                QLayout widgetLayout = widget.layout();
                if (widgetLayout != null) {
                    widgetLayout.activate();
                }
                int height = Math.max(1, widget.hasHeightForWidth() ? widget.heightForWidth(width) : widget.sizeHint().height());

                if (renderer.calcWidthInPixels(inlay) != width || renderer.calcHeightInPixels(inlay) != height) {
                    renderer.setInlaySize(width, height);
                    inlay.update();
                    resized = true;
                }
            }

            if (resized) {
                myEditor.getRowLayout().invalidate();
                surface.updateScrollRanges();
                surface.viewport().update();
                surface.updateGutter();
            }

            int scrollY = surface.verticalScrollBar().value();
            for (Map.Entry<Inlay<?>, QWidget> entry : myWidgets.entrySet()) {
                Inlay<?> inlay = entry.getKey();
                QWidget widget = entry.getValue();
                if (widget.isDisposed()) {
                    continue;
                }

                Integer y = inlay.isValid() ? myEditor.getRowLayout().inlayY(inlay) : null;
                if (y == null) {
                    widget.hide();
                    continue;
                }

                widget.setGeometry(0, y - scrollY, inlay.getWidthInPixels(), inlay.getHeightInPixels());
                widget.show();
            }
        }
        finally {
            myLayingOut = false;
        }
    }

    public void dispose() {
        for (Inlay<?> inlay : myWidgets.keySet().toArray(new Inlay<?>[0])) {
            removed(inlay);
        }
    }
}
