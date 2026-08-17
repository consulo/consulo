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
import consulo.logging.Logger;
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
import io.qt.gui.QResizeEvent;
import io.qt.gui.QScreen;
import io.qt.widgets.QApplication;
import io.qt.widgets.QMainWindow;
import io.qt.widgets.QMenuBar;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

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

        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);

            resized(event.oldSize(), event.size());
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

    private static final Logger LOG = Logger.getInstance(DesktopQtWindowImpl.class);

    /**
     * What the awt frontend hands a frame which carries no size of its own, so a window of the qt frontend
     * comes up the same way.
     */
    private static final int ourDefaultWidth = 1400;
    private static final int ourDefaultHeight = 1000;
    private static final int ourScreenMarginX = 20;
    private static final int ourScreenMarginY = 40;

    /**
     * How many times in a row the stored size is pushed back at a display server which keeps answering with
     * another one. A server which simply refuses the size - a tiled frame is one - would otherwise be argued with
     * for as long as the ide runs.
     */
    private static final int ourMaxSizeCorrections = 5;

    /**
     * The longest a drag leaves between two sizes. A compositor resizing a frame interactively reports it every
     * few dozen milliseconds; one handing over a stored geometry reports it once.
     */
    private static final long ourDragGapNanos = TimeUnit.MILLISECONDS.toNanos(250);

    private QtComponentDelegate<?> myContent;

    private final QWidget myCentralWidget;

    private @Nullable Size2D mySize;

    /**
     * The size consulo believes the window has, and the one it puts back when something else changes it.
     */
    private @Nullable QSize myOwnedSize;

    private boolean myBoundsApplied;
    private boolean myCorrectionScheduled;
    private int myCorrections;

    private long myLastResizeNanos;
    private boolean myResizeStreaming;

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

        myOwnedSize = new QSize(size.width(), size.height());
        myComponent.resize(size.width(), size.height());
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
        myOwnedSize = new QSize(bounds.width(), bounds.height());
        myBoundsApplied = true;

        if (!isAlive()) {
            return;
        }

        myComponent.setGeometry(bounds.minX(), bounds.minY(), bounds.width(), bounds.height());
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

    /**
     * Reports every resize of the frame and puts back the size consulo owns when the resize came from nowhere.
     * <p/>
     * A compositor may keep a geometry per application and hand it to whatever window of that application it sees
     * next - the plasma "remember window positions" script does, on a repeating timer - so a frame which consulo
     * sized and a frame which the compositor sized are two different things, and only the second is worth undoing.
     * The size is put back rather than the whole geometry because a wayland top level is not told where it is: the
     * position qt reports is the origin of the screen the frame sits on, not the frame.
     * <p/>
     * The report itself is off unless the {@code #consulo.desktop.qt.ui.impl.DesktopQtWindowImpl} category is
     * turned on in the debug log settings.
     */
    private void resized(QSize oldSize, QSize newSize) {
        long previousResizeNanos = myLastResizeNanos;
        myLastResizeNanos = System.nanoTime();

        myResizeStreaming = previousResizeNanos != 0 && myLastResizeNanos - previousResizeNanos < ourDragGapNanos;

        QSize ownedSize = myOwnedSize;

        boolean unsolicited = myMainFrame
            && ownedSize != null
            && !ownedSize.equals(newSize)
            && !isStateManagedByUser()
            && isAlive()
            && myComponent.isVisible();

        if (LOG.isDebugEnabled()) {
            LOG.debug("frame resized " + describe(oldSize) + " -> " + describe(newSize)
                + ", owned=" + (ownedSize == null ? "none" : describe(ownedSize))
                + ", origin=" + (unsolicited ? (isPointerDown() ? "user" : "external") : "consulo")
                + ", maximized=" + myComponent.isMaximized()
                + ", fullScreen=" + myComponent.isFullScreen()
                + ", title=" + myComponent.windowTitle());
        }

        if (!unsolicited) {
            myCorrections = 0;
            return;
        }

        // the user dragging an edge of the frame is a new size to keep, not one to argue with. wayland gives the
        // drag to the compositor and only tells the frame its new size, so no button is held anywhere and
        // isPointerDown answers on x11 alone - a drag there was undone edge by edge. What a drag looks like on
        // either is a stream of sizes tens of milliseconds apart, while a stored geometry arrives on its own
        if (isPointerDown() || isResizeStreaming()) {
            myOwnedSize = newSize;
            myCorrections = 0;
            return;
        }

        if (myCorrections >= ourMaxSizeCorrections) {
            LOG.warn("frame size " + describe(ownedSize) + " refused by the display server, keeping " + describe(newSize));

            myOwnedSize = newSize;
            myCorrections = 0;
            return;
        }

        myCorrections++;

        scheduleSizeCorrection();
    }

    /**
     * The correction is queued rather than applied here - the widget is inside its own resize event, and resizing
     * it again from there runs the layout under the pass which is already running.
     * <p/>
     * It waits out a drag gap first, so the opening move of a resize the frontend could not recognise as one is
     * not undone before the moves that identify it have arrived. Whatever the frame ended up at by then is kept.
     */
    private void scheduleSizeCorrection() {
        if (myCorrectionScheduled) {
            return;
        }

        myCorrectionScheduled = true;

        QTimer.singleShot((int) TimeUnit.NANOSECONDS.toMillis(ourDragGapNanos), () -> {
            myCorrectionScheduled = false;

            QSize ownedSize = myOwnedSize;
            if (myDisposed || !isAlive() || ownedSize == null || isStateManagedByUser() || ownedSize.equals(myComponent.size())) {
                return;
            }

            // more sizes arrived while this waited, so the frame is being dragged after all
            if (isPointerDown() || isResizeStreaming()) {
                myOwnedSize = myComponent.size();
                myCorrections = 0;
                return;
            }

            myComponent.resize(ownedSize);
        });
    }

    /**
     * Whether sizes are arriving faster than anything but a drag produces them.
     */
    private boolean isResizeStreaming() {
        return myResizeStreaming || System.nanoTime() - myLastResizeNanos < ourDragGapNanos;
    }

    private static boolean isPointerDown() {
        return QGuiApplication.mouseButtons().value() != 0;
    }

    private static String describe(QSize size) {
        return size.width() + "x" + size.height();
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

            if (!myBoundsApplied) {
                centerOnScreen();
            }

            // whatever the window comes up with is what consulo owns from here on, so a geometry pushed at it
            // afterwards is recognisable as one it never asked for
            myOwnedSize = myComponent.size();
        }

        myComponent.show();
        myComponent.raise();
        myComponent.activateWindow();
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

        getListenerDispatcher(WindowCloseEvent.class).onEvent(new WindowCloseEvent(this));

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
