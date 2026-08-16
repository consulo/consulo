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
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;
import io.qt.gui.QCursor;
import io.qt.gui.QPalette;
import io.qt.widgets.QMenuBar;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtMenuBar implements MenuBar {
    private final UIDataObject myDataObject = new UIDataObject();

    private final List<MenuItem> myItems = new ArrayList<>();

    private @Nullable QMenuBar myMenuBar;

    private boolean myEnabled = true;
    private boolean myVisible = true;

    private LocalizeValue myToolTipText = LocalizeValue.empty();
    private @Nullable ColorValue myForegroundColor;
    private @Nullable ColorValue myBackgroundColor;
    private @Nullable Cursor myCursor;

    @RequiredUIAccess
    public QMenuBar build() {
        if (myMenuBar == null) {
            QMenuBar menuBar = new QMenuBar();
            myMenuBar = menuBar;

            menuBar.setEnabled(myEnabled);

            // a parentless widget turns into a top level window the moment it is shown, and the window it is
            // handed to shows it anyway - so only an explicit hide is worth applying here
            if (!myVisible) {
                menuBar.setVisible(false);
            }

            for (MenuItem item : myItems) {
                render(menuBar, item);
            }
        }

        return myMenuBar;
    }

    public @Nullable QMenuBar toQtComponent() {
        return myMenuBar;
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        QMenuBar menuBar = myMenuBar;
        if (menuBar != null) {
            menuBar.clear();
        }

        for (MenuItem item : myItems) {
            if (item instanceof DesktopQtMenuItemImpl qtItem) {
                qtItem.disposeQt();
            }
        }

        myItems.clear();
    }

    @RequiredUIAccess
    @Override
    public MenuBar add(MenuItem menuItem) {
        myItems.add(menuItem);

        QMenuBar menuBar = myMenuBar;
        if (menuBar != null) {
            render(menuBar, menuItem);
        }

        return this;
    }

    public List<MenuItem> getItems() {
        return myItems;
    }

    @RequiredUIAccess
    private void render(QMenuBar menuBar, MenuItem menuItem) {
        if (menuItem instanceof DesktopQtMenuItemImpl qtItem) {
            qtItem.setParent(this);
            qtItem.render(menuBar);
        }
    }

    @Override
    public UIAccess getUIAccess() {
        return DesktopQtUIAccess.INSTANCE;
    }

    @RequiredUIAccess
    @Override
    public void addBorder(BorderPosition borderPosition, BorderStyle borderStyle, @Nullable ColorValue colorValue, int width) {
    }

    @RequiredUIAccess
    @Override
    public void removeBorder(BorderPosition borderPosition) {
    }

    @Override
    public boolean isVisible() {
        return myVisible;
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {
        myVisible = value;

        if (myMenuBar != null) {
            myMenuBar.setVisible(value);
        }
    }

    @Override
    public boolean isEnabled() {
        return myEnabled;
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        myEnabled = value;

        if (myMenuBar != null) {
            myMenuBar.setEnabled(value);
        }
    }

    @Override
    public @Nullable Component getParent() {
        return null;
    }

    @Override
    public void setToolTipText(LocalizeValue value) {
        myToolTipText = value;

        if (myMenuBar != null) {
            myMenuBar.setToolTip(value.get());
        }
    }

    @Override
    public LocalizeValue getToolTipText() {
        return myToolTipText;
    }

    @Override
    public @Nullable ColorValue getForegroundColor() {
        return myForegroundColor;
    }

    @Override
    public void setForegroundColor(@Nullable ColorValue foreground) {
        myForegroundColor = foreground;

        applyColors();
    }

    @Override
    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    @Override
    public void setBackgroundColor(@Nullable ColorValue background) {
        myBackgroundColor = background;

        applyColors();
    }

    private void applyColors() {
        QMenuBar menuBar = myMenuBar;
        if (menuBar == null) {
            return;
        }

        QPalette palette = new QPalette(menuBar.palette());

        if (myForegroundColor != null) {
            palette.setColor(QPalette.ColorRole.WindowText, QtComponentDelegate.toQColor(myForegroundColor));
            palette.setColor(QPalette.ColorRole.ButtonText, QtComponentDelegate.toQColor(myForegroundColor));
        }

        if (myBackgroundColor != null) {
            palette.setColor(QPalette.ColorRole.Window, QtComponentDelegate.toQColor(myBackgroundColor));

            menuBar.setAutoFillBackground(true);
        }

        menuBar.setPalette(palette);
    }

    @Override
    public @Nullable Cursor getCursor() {
        return myCursor;
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        myCursor = cursor;

        if (myMenuBar != null) {
            myMenuBar.setCursor(new QCursor(QtComponentDelegate.toCursorShape(cursor)));
        }
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        return myDataObject.getDispatcher(eventClass);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(
        Class<? extends E> eventClass,
        ComponentEventListener<C, E> listener
    ) {
        return myDataObject.addListener(eventClass, listener);
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        return myDataObject.getUserData(key);
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        myDataObject.putUserData(key, value);
    }
}
