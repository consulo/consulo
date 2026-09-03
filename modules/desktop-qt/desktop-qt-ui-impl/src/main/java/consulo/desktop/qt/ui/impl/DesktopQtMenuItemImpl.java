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

import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconRefresher;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.MenuItem;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.BorderBuilder;
import consulo.ui.PaddingBuilder;
import consulo.ui.impl.BorderBuilderImpl;
import consulo.ui.impl.PaddingBuilderImpl;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.image.Image;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;
import io.qt.gui.QAction;
import io.qt.gui.QIcon;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtMenuItemImpl implements MenuItem, DesktopQtIconOwner {
    protected final UIDataObject myDataObject = new UIDataObject();

    protected @Nullable QAction myAction;

    private LocalizeValue myText;
    private @Nullable Image myIcon;
    private LocalizeValue myShortcutText = LocalizeValue.empty();
    private LocalizeValue myToolTipText = LocalizeValue.empty();
    private @Nullable ColorValue myForegroundColor;
    private @Nullable ColorValue myBackgroundColor;
    private @Nullable Cursor myCursor;
    private @Nullable Boolean myChecked;
    private boolean myEnabled = true;
    private boolean myVisible = true;
    private @Nullable Component myParent;

    public DesktopQtMenuItemImpl(LocalizeValue text) {
        myText = Objects.requireNonNull(text);
    }

    /**
     * Adds this item to a {@code QMenuBar} or a {@code QMenu} - both hold their entries as actions, and an action
     * carrying a menu is what qt draws as a submenu, so there is nothing to branch on here.
     */
    @RequiredUIAccess
    public void render(QWidget target) {
        target.addAction(buildAction(target));
    }

    @RequiredUIAccess
    public final QAction buildAction(@Nullable QWidget parent) {
        QAction action = createAction(parent);

        myAction = action;

        DesktopQtIconRefresher.register(this);

        applyState();

        return action;
    }

    @Override
    public void refreshIcons() {
        QAction action = myAction;
        if (action != null && !action.isDisposed()) {
            applyState();
        }
    }

    @RequiredUIAccess
    protected QAction createAction(@Nullable QWidget parent) {
        QAction action = new QAction(parent);

        action.triggered.connect(() -> getListenerDispatcher(ClickEvent.class)
            .onEvent(new ClickEvent(DesktopQtMenuItemImpl.this, DesktopQtInputDetails.mouseAtCursor(parent)))
        );

        return action;
    }

    protected void applyState() {
        QAction action = myAction;
        if (action == null) {
            return;
        }

        action.setText(buildActionText());
        action.setEnabled(myEnabled);
        action.setVisible(myVisible);

        action.setIcon(myIcon instanceof DesktopQtImage qtImage ? qtImage.toQIcon() : new QIcon());

        action.setCheckable(myChecked != null);
        if (myChecked != null) {
            action.setChecked(myChecked);
        }

        action.setToolTip(myToolTipText.get());
    }

    @Override
    public void setToolTipText(LocalizeValue value) {
        myToolTipText = value;

        applyState();
    }

    @Override
    public LocalizeValue getToolTipText() {
        return myToolTipText;
    }

    /**
     * An entry of a menu is a {@code QAction} and not a widget, so it carries no palette and no cursor of its
     * own - the values are kept so a caller reading them back sees what it set.
     */
    @Override
    public @Nullable ColorValue getForegroundColor() {
        return myForegroundColor;
    }

    @Override
    public void setForegroundColor(@Nullable ColorValue foreground) {
        myForegroundColor = foreground;
    }

    @Override
    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    @Override
    public void setBackgroundColor(@Nullable ColorValue background) {
        myBackgroundColor = background;
    }

    @Override
    public @Nullable Cursor getCursor() {
        return myCursor;
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        myCursor = cursor;
    }

    /**
     * Qt reads everything after a tab as the shortcut column of the entry, so the shortcut is folded into the
     * same string the mnemonic marked text is carried by.
     */
    private String buildActionText() {
        String text = QtMnemonic.withMnemonic(myText);

        if (myShortcutText.isNotEmpty()) {
            text = text + '\t' + myShortcutText.get();
        }

        return text;
    }

    public void disposeQt() {
        myAction = null;
    }

    public void setParent(@Nullable Component parent) {
        myParent = parent;
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    public void setText(LocalizeValue text) {
        myText = text;

        applyState();
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;

        applyState();
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public void setShortcutText(LocalizeValue shortcutText) {
        myShortcutText = shortcutText;
    }

    @Override
    public void setChecked(@Nullable Boolean checked) {
        myChecked = checked;
    }

    @Override
    public UIAccess getUIAccess() {
        return DesktopQtUIAccess.INSTANCE;
    }

    @RequiredUIAccess
    @Override
    public BorderBuilder borderBuilder() {
        return BorderBuilderImpl.NOOP;
    }

    @RequiredUIAccess
    @Override
    public PaddingBuilder paddingBuilder() {
        return PaddingBuilderImpl.NOOP;
    }

    @Override
    public boolean isVisible() {
        return myVisible;
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {
        myVisible = value;

        QAction action = myAction;
        if (action != null) {
            action.setVisible(value);
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

        QAction action = myAction;
        if (action != null) {
            action.setEnabled(value);
        }
    }

    @Override
    public @Nullable Component getParent() {
        return myParent;
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
