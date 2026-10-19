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

import consulo.desktop.awt.ui.impl.facade.ToSwingWindowWrapper;
import consulo.desktop.awt.ui.impl.DesktopFontImpl;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Size2D;
import consulo.ui.Toast;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.BorderBuilder;
import consulo.ui.PaddingBuilder;
import consulo.ui.impl.BorderBuilderImpl;
import consulo.ui.impl.PaddingBuilderImpl;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.font.Font;
import consulo.ui.impl.UIDataObject;
import consulo.ui.internal.ToastOverlay;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public abstract class WindowOverAWTWindow implements Window, ToSwingWindowWrapper, ToastOverlay {
    protected final java.awt.Window myWindow;
    private final UIDataObject myUIDataObject = new UIDataObject();
    private @Nullable DesktopToastStack myToastStack;
    private @Nullable Consumer<Window> myCloseHandler;
    private int myDefaultCloseOperation = -1;

    public WindowOverAWTWindow(java.awt.Window window) {
        myWindow = window;
        myWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Consumer<Window> closeHandler = myCloseHandler;
                if (closeHandler != null) {
                    closeHandler.accept(WindowOverAWTWindow.this);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                myUIDataObject.dispose();
            }
        });
    }

    @RequiredUIAccess
    @Override
    public void setCloseHandler(@Nullable Consumer<Window> closeHandler) {
        myCloseHandler = closeHandler;

        if (myWindow instanceof JDialog dialog) {
            dialog.setDefaultCloseOperation(closeOperation(dialog.getDefaultCloseOperation(), closeHandler));
        }
        else if (myWindow instanceof JFrame frame) {
            frame.setDefaultCloseOperation(closeOperation(frame.getDefaultCloseOperation(), closeHandler));
        }
    }

    private int closeOperation(int current, @Nullable Consumer<Window> closeHandler) {
        if (closeHandler == null) {
            int restored = myDefaultCloseOperation < 0 ? current : myDefaultCloseOperation;
            myDefaultCloseOperation = -1;
            return restored;
        }

        if (myDefaultCloseOperation < 0) {
            myDefaultCloseOperation = current;
        }
        return WindowConstants.DO_NOTHING_ON_CLOSE;
    }

    @Override
    public void dispose() {
        myWindow.dispose();
    }

    @RequiredUIAccess
    @Override
    public void showToast(Toast toast) {
        DesktopToastStack stack = toastStack();
        if (stack != null) {
            stack.show(toast);
        }
    }

    @RequiredUIAccess
    @Override
    public void setToastArea(@Nullable Component area) {
        DesktopToastStack stack = toastStack();
        if (stack != null) {
            stack.setArea(area == null ? null : TargetAWT.to(area));
        }
    }

    private @Nullable DesktopToastStack toastStack() {
        DesktopToastStack stack = myToastStack;
        if (stack == null && toAWTWindow() instanceof RootPaneContainer container) {
            stack = myToastStack = new DesktopToastStack(container.getRootPane());
        }
        return stack;
    }

    @RequiredUIAccess
    @Override
    public void hideToast(Toast toast) {
        DesktopToastStack stack = myToastStack;
        if (stack != null) {
            stack.hide(toast);
        }
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
    public BorderBuilder borderBuilder() {
        return BorderBuilderImpl.NOOP;
    }

    @Override
    public PaddingBuilder paddingBuilder() {
        return PaddingBuilderImpl.NOOP;
    }


    @Override
    public boolean isVisible() {
        return myWindow.isVisible();
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

    @Override
    public @Nullable Window getParent() {
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

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        return myUIDataObject.getUserData(key);
    }
}
