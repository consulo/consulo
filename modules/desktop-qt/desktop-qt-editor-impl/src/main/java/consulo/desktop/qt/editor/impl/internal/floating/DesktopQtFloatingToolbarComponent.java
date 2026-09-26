// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.desktop.qt.editor.impl.internal.floating;

import consulo.codeEditor.impl.internal.floating.EditorFloatingToolbarActivation;
import consulo.codeEditor.impl.internal.floating.TransparentComponent;
import consulo.codeEditor.impl.internal.floating.TransparentComponentAnimator;
import consulo.codeEditor.toolbar.floating.FloatingToolbarComponent;
import consulo.codeEditor.toolbar.floating.FloatingToolbarProvider;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorImpl;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.impl.internal.action.UnifiedActionToolbarImpl;
import consulo.ui.style.StyleManager;
import io.qt.core.QPoint;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QCursor;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QGraphicsOpacityEffect;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

public final class DesktopQtFloatingToolbarComponent extends QWidget implements FloatingToolbarComponent {
    private static final int LIGHT_BACKGROUND = 0xEDEDED;

    private static final int DARK_BACKGROUND = 0x454A4D;

    private static final double BACKGROUND_ALPHA = 0.55;

    private static final double BACKGROUND_ARC_RADIUS = 3;

    private final DesktopQtEditorFloatingToolbar myHost;

    private final Disposable myParentDisposable;

    private final UnifiedActionToolbarImpl myToolbar;

    private final QGraphicsOpacityEffect myOpacityEffect;

    private final ToolbarTransparentComponent myTransparentComponent = new ToolbarTransparentComponent();

    private final TransparentComponentAnimator myComponentAnimator;

    private final EditorFloatingToolbarActivation myActivation;

    private boolean myGone;

    @RequiredUIAccess
    DesktopQtFloatingToolbarComponent(
        DesktopQtEditorImpl editor,
        FloatingToolbarProvider provider,
        DesktopQtEditorFloatingToolbar host,
        Disposable parentDisposable
    ) {
        super(host.getLayer());
        myHost = host;
        myParentDisposable = parentDisposable;

        destroyed.connect(() -> myGone = true);

        setVisible(false);
        setCursor(new QCursor(Qt.CursorShape.ArrowCursor));
        setContextMenuPolicy(Qt.ContextMenuPolicy.PreventContextMenu);

        myOpacityEffect = new QGraphicsOpacityEffect(this);
        myOpacityEffect.setOpacity(0);
        setGraphicsEffect(myOpacityEffect);

        Component editorComponent = editor.getContentUIComponent();

        myToolbar = new UnifiedActionToolbarImpl(ActionPlaces.CONTEXT_TOOLBAR, provider.getActionGroup(), ActionToolbar.Style.HORIZONTAL);
        myToolbar.setTargetUIComponent(editorComponent);

        QHBoxLayout layout = new QHBoxLayout(this);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(0);

        QtComponentDelegate<?> toolbarComponent = (QtComponentDelegate<?>) myToolbar.getUIComponent();
        toolbarComponent.setParent(editorComponent);
        toolbarComponent.bind(this, null);

        QWidget toolbarWidget = toolbarComponent.toQtComponent();
        if (toolbarWidget != null) {
            layout.addWidget(toolbarWidget);
        }
        adjustSize();

        myComponentAnimator = new TransparentComponentAnimator(myTransparentComponent, parentDisposable);

        myToolbar.addActionsUpdatedListener(parentDisposable, myTransparentComponent::fireActionsUpdated);

        myActivation = new EditorFloatingToolbarActivation(editor, provider, this, parentDisposable);

        editor.addKeyNotConsumedListener(parentDisposable, this::keyNotConsumed);

        myToolbar.updateActionsAsync();
    }

    boolean isAlive() {
        return !myHost.isGone() && !myGone && !isDisposed();
    }

    @Override
    public boolean isAutoHideable() {
        return myComponentAnimator.isAutoHideable();
    }

    @Override
    @RequiredUIAccess
    public void setAutoHideable(boolean autoHideable) {
        myComponentAnimator.setAutoHideable(autoHideable);
    }

    @Override
    @RequiredUIAccess
    public void scheduleShow() {
        myComponentAnimator.scheduleShow();
    }

    @Override
    @RequiredUIAccess
    public void scheduleHide() {
        myComponentAnimator.scheduleHide();
    }

    @Override
    @RequiredUIAccess
    public void hideImmediately() {
        myComponentAnimator.hideImmediately();
    }

    @RequiredUIAccess
    private void keyNotConsumed(KeyCode keyCode) {
        if (KeyCode.ESCAPE.equals(keyCode) && isAlive()) {
            myActivation.escapePressed(getVisibleAreaOnScreen());
        }
    }

    private @Nullable Rectangle2D getVisibleAreaOnScreen() {
        if (!isVisible()) {
            return null;
        }
        QPoint location = mapToGlobal(new QPoint(0, 0));
        return new Rectangle2D(location.x(), location.y(), width(), height());
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setOpacity(BACKGROUND_ALPHA);
            painter.setPen(Qt.PenStyle.NoPen);
            painter.setBrush(backgroundColor());
            painter.drawRoundedRect(new QRectF(rect()), BACKGROUND_ARC_RADIUS, BACKGROUND_ARC_RADIUS);
        }
        finally {
            painter.end();
        }
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        event.accept();
    }

    @Override
    protected void mouseReleaseEvent(QMouseEvent event) {
        event.accept();
    }

    @Override
    protected void mouseDoubleClickEvent(QMouseEvent event) {
        event.accept();
    }

    private static QColor backgroundColor() {
        return QColor.fromRgb(StyleManager.get().getCurrentStyle().isDark() ? DARK_BACKGROUND : LIGHT_BACKGROUND);
    }

    private final class ToolbarTransparentComponent implements TransparentComponent {
        private boolean myVisible = false;

        @Override
        @RequiredUIAccess
        public boolean isComponentOnHold() {
            return isAlive() && myHost.isComponentOnHold();
        }

        @Override
        @RequiredUIAccess
        public void setOpacity(float opacity) {
            if (!isAlive()) {
                return;
            }
            myOpacityEffect.setOpacity(opacity);
            myOpacityEffect.setEnabled(opacity < 1);
        }

        @Override
        @RequiredUIAccess
        public void showComponent() {
            myVisible = true;
            updateActions();
            fireActionsUpdated();
        }

        @Override
        @RequiredUIAccess
        public void hideComponent() {
            if (!myVisible) {
                return;
            }
            myVisible = false;
            updateActions();
            fireActionsUpdated();
        }

        @Override
        @RequiredUIAccess
        public void repaintComponent() {
            if (isAlive()) {
                update();
            }
        }

        @RequiredUIAccess
        void fireActionsUpdated() {
            if (isAlive()) {
                setVisible(myVisible && !myToolbar.getActions().isEmpty());
            }
        }

        @RequiredUIAccess
        private void updateActions() {
            if (isAlive() && !Disposer.isDisposed(myParentDisposable)) {
                myToolbar.updateActionsAsync();
            }
        }
    }
}
