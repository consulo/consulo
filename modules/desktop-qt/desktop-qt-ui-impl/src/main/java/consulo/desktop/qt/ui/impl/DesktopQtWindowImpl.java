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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.titleless.DesktopQtTitleBarPlacement;
import consulo.desktop.qt.ui.impl.titleless.DesktopQtWindowFrame;
import consulo.disposer.Disposer;
import consulo.project.ui.impl.internal.wm.UnifiedWelcomeIdeFrame;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.WindowCloseEvent;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.TitlelessDecoratorService;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QCloseEvent;
import io.qt.gui.QGuiApplication;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QScreen;
import io.qt.widgets.QApplication;
import io.qt.widgets.QMainWindow;
import io.qt.widgets.QMenuBar;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtWindowImpl extends QtComponentDelegate<QMainWindow> implements Window {
    private class QtWindow extends QMainWindow {
        QtWindow(@Nullable QMainWindow parent) {
            super(parent);
        }

        @Override
        protected void closeEvent(QCloseEvent event) {
            super.closeEvent(event);

            closed();
        }

        /**
         * A window which draws its own decoration is translucent around its content, and what fills that margin -
         * the border and the shadow - is painted here, under everything the window holds.
         */
        @Override
        protected void paintEvent(QPaintEvent event) {
            DesktopQtWindowFrame titlelessFrame = myTitlelessFrame;
            if (titlelessFrame == null) {
                super.paintEvent(event);
                return;
            }

            titlelessFrame.paint();
        }
    }

    /**
     * What the awt frontend hands a frame which carries no size of its own, so a window of the qt frontend
     * comes up the same way.
     */
    private static final int ourDefaultWidth = 1400;
    private static final int ourDefaultHeight = 1000;
    private static final int ourScreenMarginX = 20;
    private static final int ourScreenMarginY = 40;

    private QtComponentDelegate<?> myContent;

    private final QWidget myCentralWidget;

    private @Nullable Size2D mySize;

    private boolean myBoundsApplied;

    private final boolean myResizable;

    private final @Nullable String myApplicationIdSuffix;

    private boolean myDisposed;
    private boolean myMainFrame;

    /**
     * Set once the window draws its own decoration, and null while the display server draws it.
     */
    private @Nullable DesktopQtWindowFrame myTitlelessFrame;

    private @Nullable QMenuBar myQtMenuBar;

    public DesktopQtWindowImpl(String title, WindowOptions options) {
        QMainWindow parent = null;
        Window owner = options.getOwner();
        if (owner instanceof DesktopQtWindowImpl qtWindow) {
            parent = qtWindow.toQtComponent();
        }

        myResizable = options.isResizable();
        myApplicationIdSuffix = options.getApplicationIdSuffix();

        myComponent = new QtWindow(parent);
        myComponent.setWindowTitle(title);

        if (parent != null) {
            myComponent.setWindowModality(Qt.WindowModality.ApplicationModal);
        }

        if (!options.isClosable()) {
            myComponent.setWindowFlag(Qt.WindowType.WindowCloseButtonHint, false);
        }

        myCentralWidget = new QWidget();
        QVBoxLayout layout = new QVBoxLayout();
        layout.setContentsMargins(0, 0, 0, 0);
        myCentralWidget.setLayout(layout);
        myComponent.setCentralWidget(myCentralWidget);

        TargetQt.register(myComponent, this);

        if (options.isUndecorated()) {
            installTitleBar(DesktopQtTitleBarPlacement.NONE);
        }
    }

    /**
     * Says that this is the window the ide itself lives in, and not one of the windows raised over it.
     * <p/>
     * Wayland gives a window no type of its own - what tells the compositor that a window is not another main
     * window of the same application is the parent it names. A compositor which keeps a geometry per window of
     * an application, as the plasma "remember window positions" script does, treats every parentless top level
     * of one application id as the same window: it stores what it last saw on the welcome screen or on a
     * settings window, and hands that geometry to the frame the next time one is mapped, which is why a
     * maximized frame drops to the size of a dialog while the ide is running.
     */
    public void markAsMainFrame() {
        myMainFrame = true;
    }

    /**
     * Drops the title bar of the display server and has the window draw its own, which is what puts the menu bar and
     * the window buttons in a header of the ide.
     * <p/>
     * Which windows this reaches is decided by {@code DesktopQtTitlelessDecoratorService} and nothing else.
     */
    @RequiredUIAccess
    public void installTitleBar(DesktopQtTitleBarPlacement placement) {
        if (myTitlelessFrame != null) {
            return;
        }

        DesktopQtWindowFrame frame = new DesktopQtWindowFrame(myComponent, myCentralWidget, placement);
        myTitlelessFrame = frame;

        frame.setMenuBar(myQtMenuBar);
    }

    @Override
    protected QMainWindow createQt(QWidget parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initialize(QMainWindow component) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Window getParent() {
        return (Window) super.getParent();
    }

    @RequiredUIAccess
    @Override
    public void setSize(Size2D size) {
        mySize = size;

        // resizing a maximized window is what takes it out of that state, and the geometry it then falls back
        // to is whatever it held before - which for a frame the user maximized is the size it opened at
        if (isStateManagedByUser()) {
            return;
        }

        if (!isAlive()) {
            return;
        }

        myComponent.resize(size.width(), size.height());

        applyFixedSize();
    }

    private void applyFixedSize() {
        if (myResizable) {
            return;
        }

        myComponent.setFixedSize(myComponent.size());
    }

    /**
     * Puts the window back where consulo last had it. The position is applied for the display servers which honour
     * one - wayland does not let a top level place itself, and the size is what matters there anyway.
     */
    @RequiredUIAccess
    public void setBounds(Rectangle2D bounds) {
        if (bounds.isEmpty()) {
            return;
        }

        mySize = bounds.size();
        myBoundsApplied = true;

        if (!isAlive()) {
            return;
        }

        myComponent.setGeometry(bounds.minX(), bounds.minY(), bounds.width(), bounds.height());

        applyFixedSize();
    }

    /**
     * The frame is asked for its bounds while the project closes, and qt has torn the window down by then - the
     * java object outlives the native one and answers every call with
     * {@link io.qt.QNoNativeResourcesException}. The size consulo last owned is the honest answer there.
     */
    public Rectangle2D getBounds() {
        if (!isAlive()) {
            return mySize == null ? new Rectangle2D(0, 0, 0, 0) : new Rectangle2D(0, 0, mySize.width(), mySize.height());
        }

        QRect geometry = myComponent.geometry();

        return new Rectangle2D(geometry.x(), geometry.y(), geometry.width(), geometry.height());
    }

    @RequiredUIAccess
    public void setMaximized(boolean maximized) {
        if (!isAlive()) {
            return;
        }

        myComponent.setWindowState(maximized ? Qt.WindowState.WindowMaximized : Qt.WindowState.WindowNoState);
    }

    public boolean isMaximized() {
        return isAlive() && myComponent.isMaximized();
    }

    public boolean isFullScreen() {
        return isAlive() && myComponent.isFullScreen();
    }

    private boolean isAlive() {
        return myComponent != null && !myComponent.isDisposed();
    }

    private boolean isStateManagedByUser() {
        return isMaximized() || isFullScreen();
    }

    @RequiredUIAccess
    @Override
    public void setTitle(String title) {
        myComponent.setWindowTitle(title);
    }

    @RequiredUIAccess
    @Override
    public void setContent(Component content) {
        myContent = (QtComponentDelegate<?>) content;
    }

    @RequiredUIAccess
    @Override
    public void setMenuBar(@Nullable MenuBar menuBar) {
        if (menuBar instanceof DesktopQtMenuBar qtMenuBar) {
            QMenuBar built = qtMenuBar.build();
            myQtMenuBar = built;

            DesktopQtWindowFrame titlelessFrame = myTitlelessFrame;
            if (titlelessFrame == null || !titlelessFrame.setMenuBar(built)) {
                myComponent.setMenuBar(built);
            }
        }
    }

    @RequiredUIAccess
    @Override
    public void show() {
        if (myContent != null && myContent.toQtComponent() == null) {
            myContent.setParent(this);
            myContent.bind(myCentralWidget, null);

            myCentralWidget.layout().addWidget(myContent.toQtComponent());
        }

        // qt packs a window it was never given a size for down to its size hint the first time it is shown,
        // and that packed geometry is what the window falls back to for the rest of its life whenever it
        // leaves the maximized state
        if (!myComponent.isVisible()) {
            applyDialogRole();
            applyDefaultSize();
            applyFixedSize();

            if (!myBoundsApplied) {
                centerOnScreen();
            }
        }

        showNative();
        myComponent.raise();
        myComponent.activateWindow();
    }

    private void showNative() {
        String applicationIdSuffix = myApplicationIdSuffix;
        if (applicationIdSuffix == null || myComponent.isVisible()) {
            myComponent.show();
            return;
        }

        String applicationId = QGuiApplication.desktopFileName();
        QGuiApplication.setDesktopFileName(applicationId + "-" + applicationIdSuffix);
        try {
            myComponent.show();
        }
        finally {
            QGuiApplication.setDesktopFileName(applicationId);
        }
    }

    /**
     * @see #markAsMainFrame()
     */
    private void applyDialogRole() {
        if (myMainFrame) {
            return;
        }

        myComponent.setWindowFlag(Qt.WindowType.Dialog, true);

        if (myComponent.parentWidget() != null) {
            return;
        }

        QWidget owner = activeWindowWidget();
        if (owner != null) {
            myComponent.setParent(owner, myComponent.windowFlags());
        }
    }

    /**
     * The window the user is working in, and only that - a popup is a top level widget of its own and would be
     * gone by the time whatever it raised is closed.
     */
    private static @Nullable QWidget activeWindowWidget() {
        QWidget active = QApplication.activeWindow();
        return TargetQt.from(active) instanceof Window ? active : null;
    }

    private void applyDefaultSize() {
        if (mySize != null || myComponent.testAttribute(Qt.WidgetAttribute.WA_Resized)) {
            return;
        }

        QRect available = availableGeometry();
        if (available == null) {
            myComponent.resize(ourDefaultWidth, ourDefaultHeight);
            return;
        }

        myComponent.resize(
            Math.min(ourDefaultWidth, available.width() - ourScreenMarginX),
            Math.min(ourDefaultHeight, available.height() - ourScreenMarginY)
        );
    }

    private void centerOnScreen() {
        if (isStateManagedByUser()) {
            return;
        }

        QRect available = availableGeometry();
        if (available == null) {
            return;
        }

        QSize size = myComponent.size();

        myComponent.move(
            available.x() + (available.width() - size.width()) / 2,
            available.y() + (available.height() - size.height()) / 2
        );
    }

    private static @Nullable QRect availableGeometry() {
        QScreen screen = QApplication.primaryScreen();
        return screen == null ? null : screen.availableGeometry();
    }

    @RequiredUIAccess
    @Override
    public void close() {
        if (myDisposed) {
            return;
        }

        myComponent.close();
    }

    /**
     * Runs for both an api close and a close of the native window, so anything registered through
     * {@link #addCloseListener} sees every close.
     */
    @RequiredUIAccess
    private void closed() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        getListenerDispatcher(WindowCloseEvent.class).onEvent(new WindowCloseEvent(this, DesktopQtCurrentInput.current(myComponent)));

        // the widget is still handling its own close event here, so deleting it now would pull the object out
        // from under the running event dispatch
        QTimer.singleShot(0, () -> Disposer.dispose(this));
    }

    @Override
    public boolean isActive() {
        return !myDisposed && myComponent.isActiveWindow();
    }

    @Override
    public void dispose() {
        myDisposed = true;

        disposeQt();
    }
}
