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

import consulo.localize.LocalizeValue;
import consulo.ui.Menu;
import consulo.ui.MenuItem;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.gui.QAction;
import io.qt.widgets.QApplication;
import io.qt.widgets.QMenu;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtMenuImpl extends DesktopQtMenuItemImpl implements Menu {
    /**
     * Qt keeps a column down the left of a menu for the icon and the check mark of every entry and paints the
     * check as a framed box, where awt reserves nothing for an entry without an icon and paints a bare check
     * mark. Giving the entry its own padding hands that column back to the text; the sheet then owns the whole
     * of the entry, so the selection it used to draw is taken from the palette here.
     */
    private static final String STYLE_SHEET = """
        QMenu { padding: 4px 0px; }
        QMenu::item { padding: 4px 12px 4px 6px; }
        QMenu::item:selected { background-color: palette(highlight); color: palette(highlighted-text); }
        QMenu::icon { margin-left: 6px; }
        """;

    /**
     * Watches for a click which lands outside the menu and takes it down, doing by hand what the mouse grab of a
     * {@code Qt::Popup} would have done.
     */
    private class OutsideClickWatcher extends QObject {
        @Override
        public boolean eventFilter(QObject watched, QEvent event) {
            // a press reaches the QWindow of the frame before it reaches the widget it lands on, and a window
            // carries no widget to test - the widget the same press is handed to next answers this properly
            if (event.type() == QEvent.Type.MouseButtonPress && watched instanceof QWidget widget && !ownsWidget(widget)) {
                hideDetached();
            }

            return super.eventFilter(watched, event);
        }
    }

    private final List<MenuItem> myChildren = new ArrayList<>();

    private @Nullable QMenu myMenu;

    private @Nullable OutsideClickWatcher myOutsideClickWatcher;

    private boolean myHideConnected;

    public DesktopQtMenuImpl(LocalizeValue text) {
        super(text);
    }

    @RequiredUIAccess
    @Override
    public Menu add(MenuItem menuItem) {
        myChildren.add(menuItem);

        if (menuItem instanceof DesktopQtMenuItemImpl item) {
            item.setParent(this);

            QMenu menu = myMenu;
            if (menu != null) {
                item.render(menu);
            }
        }

        return this;
    }

    public List<MenuItem> getChildren() {
        return myChildren;
    }

    /**
     * Builds the menu without attaching it anywhere - what a context menu shown at a point needs.
     */
    @RequiredUIAccess
    public QMenu buildMenu(@Nullable QWidget parent) {
        buildAction(parent);

        return Objects.requireNonNull(myMenu);
    }

    public @Nullable QMenu toQtMenu() {
        return myMenu;
    }

    /**
     * Raises the menu at a point on screen, on its own rather than out of a menu bar.
     * <p/>
     * A {@link QMenu} is a {@code Qt::Popup}, which wayland turns into a grabbing {@code xdg_popup}: the compositor
     * dismisses one which was not opened from the input event it holds the serial of, and a menu of an action group
     * is only raised once the group has been expanded off the ui thread - long after the click is over. So the menu
     * is re-flagged into a {@code Qt::ToolTip}, the same way {@link DesktopQtPopupImpl} is, and the dismissal the
     * lost grab used to give is read from an application wide event filter instead.
     */
    @RequiredUIAccess
    public void popupDetached(@Nullable QWidget owner, QPoint globalPosition) {
        QMenu menu = myMenu;
        if (menu == null || menu.isDisposed()) {
            menu = buildMenu(owner);
        }

        applyDetachedFlags();

        if (!myHideConnected) {
            myHideConnected = true;

            menu.aboutToHide.connect(this::uninstallOutsideClickWatcher);
        }

        menu.popup(globalPosition);

        OutsideClickWatcher watcher = myOutsideClickWatcher;
        if (watcher == null) {
            watcher = new OutsideClickWatcher();
            myOutsideClickWatcher = watcher;
        }

        QApplication.instance().installEventFilter(watcher);
    }

    /**
     * {@code Qt::ToolTip} rather than {@code Qt::Tool}: a top level on wayland is not allowed to place itself, so a
     * menu flagged as one was dropped in the middle of the frame wherever it was asked to appear, while a
     * {@code Qt::ToolTip} with a transient parent is mapped onto an ungrabbed {@code xdg_popup} and is placed.
     */
    private void applyDetachedFlags() {
        QMenu menu = myMenu;
        if (menu != null && !menu.isDisposed()) {
            menu.setWindowFlags(
                Qt.WindowType.ToolTip,
                Qt.WindowType.FramelessWindowHint,
                Qt.WindowType.WindowStaysOnTopHint
            );
        }

        for (MenuItem child : myChildren) {
            if (child instanceof DesktopQtMenuImpl subMenu) {
                subMenu.applyDetachedFlags();
            }
        }
    }

    private void hideDetached() {
        QMenu menu = myMenu;
        if (menu != null && !menu.isDisposed()) {
            menu.hide();
        }
    }

    private void uninstallOutsideClickWatcher() {
        OutsideClickWatcher watcher = myOutsideClickWatcher;
        if (watcher == null) {
            return;
        }

        myOutsideClickWatcher = null;

        QApplication.instance().removeEventFilter(watcher);
        watcher.dispose();
    }

    /**
     * Whether the event went to the menu, counting the submenus - a click inside one of those is not a click
     * outside the menu it was opened from. The widget the event was sent to answers this where
     * {@code QApplication#widgetAt} does not: wayland tells no one where the pointer is.
     */
    private boolean ownsWidget(QWidget widget) {
        QMenu menu = myMenu;
        if (menu == null || menu.isDisposed()) {
            return false;
        }

        for (QWidget current = widget; current != null; current = current.parentWidget()) {
            if (current == menu) {
                return true;
            }
        }

        return false;
    }

    @RequiredUIAccess
    @Override
    protected QAction createAction(@Nullable QWidget parent) {
        QMenu menu = new QMenu(parent);
        menu.setStyleSheet(STYLE_SHEET);

        myMenu = menu;

        for (MenuItem child : myChildren) {
            if (child instanceof DesktopQtMenuItemImpl item) {
                item.render(menu);
            }
        }

        return menu.menuAction();
    }

    @Override
    public void disposeQt() {
        for (MenuItem child : myChildren) {
            if (child instanceof DesktopQtMenuItemImpl item) {
                item.disposeQt();
            }
        }

        uninstallOutsideClickWatcher();

        myHideConnected = false;

        QMenu menu = myMenu;
        myMenu = null;

        super.disposeQt();

        // the menu owns its entries and its own menu action, so nothing below it is disposed by hand
        if (menu != null && !menu.isDisposed()) {
            menu.dispose();
        }
    }
}
