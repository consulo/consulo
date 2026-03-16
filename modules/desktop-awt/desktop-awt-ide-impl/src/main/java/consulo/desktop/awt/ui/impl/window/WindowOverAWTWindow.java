/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.awt.ui.impl.window;

import consulo.desktop.awt.facade.ToSwingWindowWrapper;
import consulo.desktop.awt.ui.impl.DesktopFontImpl;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Size2D;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.font.Font;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public abstract class WindowOverAWTWindow implements Window, ToSwingWindowWrapper {
    protected final java.awt.Window myWindow;
    private final UIDataObject myUIDataObject = new UIDataObject();

    public WindowOverAWTWindow(java.awt.Window window) {
        myWindow = window;
        myWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                myUIDataObject.dispose();
            }
        });
    }

    @Override
    public void dispose() {
        myWindow.dispose();
    }

    @Override
    public java.awt.Window toAWTWindow() {
        return myWindow;
    }

    @Override
    public boolean isActive() {
        return myWindow.isActive();
    }

    @RequiredUIAccess
    @Override
    public void show() {
        setVisible(true);
    }

    @RequiredUIAccess
    @Override
    public void close() {
        setVisible(false);
    }

    @RequiredUIAccess
    @Override
    public void setContent(Component content) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public void setMenuBar(@Nullable MenuBar menuBar) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public void addBorder(BorderPosition borderPosition, BorderStyle borderStyle, ColorValue colorValue, int width) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public void removeBorder(BorderPosition borderPosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVisible() {
        return myWindow.isVisible();
    }

    @Override
    public Font getFont() {
        return new DesktopFontImpl(myWindow.getFont());
    }

    @Override
    public void setFont(Font font) {
        myWindow.setFont(TargetAWT.to(font));
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {
        SwingUtilities.invokeLater(() -> myWindow.setVisible(value));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
    }

    @Nullable
    @Override
    public Window getParent() {
        return TargetAWT.from((java.awt.Window) myWindow.getParent());
    }

    @RequiredUIAccess
    @Override
    public void setSize(Size2D size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        myUIDataObject.putUserData(key, value);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        return myUIDataObject.getDispatcher(eventClass);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(Class<? extends E> eventClass, ComponentEventListener<C, E> listener) {
        return myUIDataObject.addListener(eventClass, listener);
    }

    @Nullable
    @Override
    public <T> T getUserData(Key<T> key) {
        return myUIDataObject.getUserData(key);
    }
}
