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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.Popup;
import consulo.ui.PopupOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.PopupCloseEvent;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QCloseEvent;
import io.qt.gui.QKeyEvent;
import io.qt.gui.QScreen;
import io.qt.widgets.QApplication;
import io.qt.widgets.QFrame;
import io.qt.widgets.QLabel;
import io.qt.widgets.QSizeGrip;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * A frameless surface which floats over the frame.
 * <p/>
 * It is a {@code Qt::ToolTip}, which is neither of the two obvious choices, because those are the two which do not
 * work on wayland. A {@code Qt::Popup} becomes a grabbing {@code xdg_popup} and the compositor dismisses one which
 * was not opened from the input event it holds the serial of - every popup here is raised from a queued ui-access
 * call, the click long over by then, so it was torn down the moment it went up. A {@code Qt::Tool} becomes an
 * ordinary top level, and a wayland top level is not allowed to place itself: the position is dropped and the
 * compositor puts the window where it likes, which on plasma is the middle of the window it belongs to - measured
 * against kwin, a {@code Qt::Tool} asked for the point under a button landed exactly on the centre of the frame.
 * {@code Qt::ToolTip} with a transient parent is the one type qt maps onto an ungrabbed {@code xdg_popup}: placed
 * against its parent, and left standing until it is closed.
 * <p/>
 * The price is that an ungrabbed popup is never handed the keyboard - it stays with the frame - and that qt routes
 * neither the mouse nor the keyboard to it the way it does to a {@code Qt::Popup}. So dismissing on a click
 * outside, dismissing on escape, and the keys the content lives on are all read from an application wide event
 * filter instead of the grab a popup would have had - the same thing {@link DesktopQtMenuImpl#popupDetached} does.
 * <p/>
 * {@link Popup} is sealed, so the two frontend popups implement their own side of it and share only what is here.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public abstract class DesktopQtPopupImpl extends QtComponentDelegate<QWidget> implements Disposable {
    private class QtPopup extends QFrame {
        QtPopup() {
            super(null, Qt.WindowType.ToolTip, Qt.WindowType.FramelessWindowHint, Qt.WindowType.WindowStaysOnTopHint);
        }

        @Override
        protected void closeEvent(QCloseEvent event) {
            super.closeEvent(event);

            closed();
        }
    }

    /**
     * Gives back the dismissal the mouse and keyboard grab of a {@code Qt::Popup} used to carry.
     */
    private class DismissWatcher extends QObject {
        @Override
        public boolean eventFilter(QObject watched, QEvent event) {
            QEvent.Type type = event.type();

            // a press reaches the QWindow of the frame before it reaches the widget it lands on, and a window
            // carries no widget to test - the widget the same press is handed to next answers this properly.
            // QApplication#widgetAt cannot be asked instead: wayland tells no one where the pointer is
            if (type == QEvent.Type.MouseButtonPress
                && myOptions.isCancelOnClickOutside()
                && watched instanceof QWidget widget
                && !ownsWidget(widget)) {
                close();
                return false;
            }

            if (type == QEvent.Type.KeyPress && event instanceof QKeyEvent keyEvent) {
                if (myOptions.isCancelOnEscape() && keyEvent.key() == Qt.Key.Key_Escape.value()) {
                    close();

                    // the innermost popup installed its filter last and a filter installed last is called first, so
                    // taking the key here is what leaves the popup which owns this one standing
                    return true;
                }

                // an ungrabbed xdg_popup is never handed the wayland keyboard, so the arrows and the return key the
                // content lives on are delivered to the frame and have to be carried over by hand. qt offers the
                // key twice, to the window and then to the widget it settled on - only the second carries a widget
                if (myOptions.isRequestFocus()
                    && isVisible()
                    && watched instanceof QWidget widget
                    && !ownsWidget(widget)) {
                    QWidget target = keyboardTarget();
                    if (target != null) {
                        QApplication.sendEvent(target, keyEvent);
                        return true;
                    }
                }
            }

            return super.eventFilter(watched, event);
        }
    }

    protected final PopupOptions myOptions;

    private final QVBoxLayout myLayout;

    private final DismissWatcher myDismissWatcher = new DismissWatcher();

    private @Nullable QLabel myTitleLabel;
    private @Nullable QtComponentDelegate<?> myContent;

    private boolean myDisposed;

    public DesktopQtPopupImpl(PopupOptions options) {
        myOptions = options;

        QtPopup popup = new QtPopup();

        // a frameless window carries no chrome of its own, and without a border of some kind the content bleeds
        // into whatever the popup floats over
        popup.setFrameShape(QFrame.Shape.StyledPanel);

        myComponent = popup;
        myComponent.setFocusPolicy(options.isRequestFocus() ? Qt.FocusPolicy.StrongFocus : Qt.FocusPolicy.NoFocus);

        // a popup which reports on what the user is doing somewhere else - the lookup is driven from the editor -
        // must not pull the window focus away from where they are working
        if (!options.isRequestFocus()) {
            myComponent.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating, true);
        }

        myLayout = new QVBoxLayout();
        myLayout.setContentsMargins(0, 0, 0, 0);
        myComponent.setLayout(myLayout);

        if (options.isResizable()) {
            myLayout.addWidget(new QSizeGrip(myComponent), 0, Qt.AlignmentFlag.AlignRight, Qt.AlignmentFlag.AlignBottom);
        }

        TargetQt.register(myComponent, this);
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initialize(QWidget component) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    public void setTitle(@Nullable String title) {
        QLabel label = myTitleLabel;

        if (title == null || title.isEmpty()) {
            if (label != null) {
                myLayout.removeWidget(label);
                label.dispose();
                myTitleLabel = null;
            }
            return;
        }

        if (label == null) {
            label = new QLabel(myComponent);
            myLayout.insertWidget(0, label);
            myTitleLabel = label;
        }

        label.setText(title);
    }

    @RequiredUIAccess
    public void setMinimumWidth(int width) {
        myComponent.setMinimumWidth(Math.max(width, 0));
    }

    @RequiredUIAccess
    public void setContent(Component content) {
        QtComponentDelegate<?> previous = myContent;
        if (previous != null) {
            QWidget widget = previous.toQtComponent();
            if (widget != null) {
                myLayout.removeWidget(widget);
            }
            previous.setParent(null);
        }

        QtComponentDelegate<?> delegate = (QtComponentDelegate<?>) content;
        delegate.setParent(this);
        delegate.bind(myComponent, null);

        myLayout.insertWidget(myTitleLabel == null ? 0 : 1, delegate.toQtComponent());

        myContent = delegate;

        // nothing lays a window out from above, so a popup is only ever the size it was told to take
        myComponent.adjustSize();
    }

    /**
     * Opens the popup at a point inside {@code target} rather than against the component as a whole - the popup
     * hangs under whatever the point belongs to, so a caret line is not covered by it.
     */
    @RequiredUIAccess
    protected void showAtComponent(Component target, int x, int y, int anchorHeight) {
        QWidget widget = toQtWidget(target);
        if (widget == null) {
            return;
        }

        setOwner(widget);

        showAtGlobal(widget.mapToGlobal(new QPoint(x, y + anchorHeight)));
    }

    /**
     * Hands the popup the window it belongs to, which is what makes it a popup at all: qt only maps a surface onto
     * an {@code xdg_popup} when it has a transient parent, and without one the popup is another top level - listed
     * as a window of its own, stacked on its own, and placed by the compositor rather than by consulo.
     */
    @RequiredUIAccess
    protected void setOwner(@Nullable QWidget owner) {
        QWidget window = owner == null ? QApplication.activeWindow() : owner.window();

        if (window == null || window == myComponent || myComponent.parentWidget() == window) {
            return;
        }

        myComponent.setParent(window, myComponent.windowFlags());

        // re-parenting builds the native window again, and the attributes which belong to it go with it
        if (!myOptions.isRequestFocus()) {
            myComponent.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating, true);
        }
    }

    /**
     * Whether the widget belongs to this popup, counting a popup which was opened against it - a submenu is not
     * something the popup which owns it should be dismissed by. A nested popup is re-parented onto the popup which
     * raised it, so walking the parents is what answers this.
     */
    /**
     * What a key carried over from the frame is handed to - the content itself where it has no inner focus of its
     * own, since a list answers the arrows and the return key from the view inside it.
     */
    private @Nullable QWidget keyboardTarget() {
        QtComponentDelegate<?> content = myContent;
        QWidget contentWidget = content == null ? null : content.toQtComponent();
        if (contentWidget == null) {
            return null;
        }

        QWidget focused = contentWidget.focusWidget();

        return focused == null ? contentWidget : focused;
    }

    private boolean ownsWidget(@Nullable QWidget widget) {
        for (QWidget current = widget; current != null; current = current.parentWidget()) {
            if (current == myComponent) {
                return true;
            }
        }

        return false;
    }

    @RequiredUIAccess
    protected void showAtGlobal(QPoint position) {
        checkNotDisposed();

        myComponent.adjustSize();
        myComponent.move(clampToScreen(position));
        myComponent.show();
        myComponent.raise();

        if (myOptions.isRequestFocus()) {
            myComponent.activateWindow();

            // the keyboard belongs to whatever the popup was built around - a list answers the arrows and the
            // return key itself, and the frame around it has nothing to do with either
            QtComponentDelegate<?> content = myContent;
            QWidget contentWidget = content == null ? null : content.toQtComponent();

            (contentWidget == null ? myComponent : contentWidget).setFocus();
        }

        QApplication.instance().installEventFilter(myDismissWatcher);
    }

    /**
     * A browser turns a popover around by itself when there is no room for it; qt places exactly where it is told,
     * so a popup opened near an edge would be drawn half off the screen.
     */
    private QPoint clampToScreen(QPoint position) {
        QScreen screen = QApplication.screenAt(position);
        if (screen == null) {
            screen = QApplication.primaryScreen();
        }

        if (screen == null) {
            return position;
        }

        QRect available = screen.availableGeometry();
        QSize size = myComponent.size();

        int x = Math.min(position.x(), available.x() + available.width() - size.width());
        int y = Math.min(position.y(), available.y() + available.height() - size.height());

        return new QPoint(Math.max(x, available.x()), Math.max(y, available.y()));
    }

    /**
     * The window the popup was hung under, once {@link #setOwner} has run.
     */
    protected @Nullable QWidget ownerWindow() {
        return myComponent.parentWidget();
    }

    protected static @Nullable QWidget toQtWidget(Component component) {
        return component instanceof QtComponentDelegate<?> delegate ? delegate.toQtComponent() : null;
    }

    @RequiredUIAccess
    protected void checkNotDisposed() {
        if (myDisposed) {
            throw new IllegalArgumentException("Popup already disposed");
        }
    }

    @RequiredUIAccess
    public void close() {
        if (myDisposed) {
            return;
        }

        myComponent.close();
    }

    /**
     * Runs for both an api close and a dismissal by the user, so anything registered as a close listener sees
     * every close.
     */
    @RequiredUIAccess
    private void closed() {
        if (myDisposed) {
            return;
        }

        myDisposed = true;

        QApplication.instance().removeEventFilter(myDismissWatcher);

        getListenerDispatcher(PopupCloseEvent.class).onEvent(new PopupCloseEvent((Popup) this));

        // the widget is still handling its own close event here, and the watcher may be inside the very filter
        // call which closed the popup - deleting either now would pull the object out from under the running
        // event dispatch
        QTimer.singleShot(0, () -> Disposer.dispose(this));
    }

    @Override
    public boolean isVisible() {
        return !myDisposed && myComponent != null && myComponent.isVisible();
    }

    @Override
    public void dispose() {
        myDisposed = true;

        QApplication.instance().removeEventFilter(myDismissWatcher);

        disposeQt();

        if (!myDismissWatcher.isDisposed()) {
            myDismissWatcher.dispose();
        }
    }
}
