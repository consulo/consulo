/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.awt.ui.impl.base;

import consulo.annotation.DeprecationInfo;
import consulo.desktop.awt.facade.ToSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.DesktopFontImpl;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.util.AWTFocusAdapterAsBlurListener;
import consulo.desktop.awt.ui.impl.util.AWTFocusAdapterAsFocusListener;
import consulo.desktop.awt.ui.impl.util.AWTKeyAdapterAsKeyPressedListener;
import consulo.desktop.awt.ui.impl.util.AWTKeyAdapterAsKeyReleasedListener;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.FocusableComponent;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.*;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.font.Font;
import consulo.ui.impl.BorderInfo;
import consulo.ui.impl.UIDataObject;
import consulo.ui.util.MnemonicInfo;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
public class SwingComponentDelegate<T extends java.awt.Component> implements Component, ToSwingComponentWrapper {
    @Deprecated
    @DeprecationInfo("Use #initialize() method")
    protected T myComponent;

    @SuppressWarnings("deprecation")
    protected void initialize(T component) {
        myComponent = component;

        myComponent.addKeyListener(new AWTKeyAdapterAsKeyPressedListener(this, getListenerDispatcher(KeyPressedEvent.class)));
        myComponent.addKeyListener(new AWTKeyAdapterAsKeyReleasedListener(this, getListenerDispatcher(KeyReleasedEvent.class)));

        if (this instanceof FocusableComponent) {
            myComponent.addFocusListener(new AWTFocusAdapterAsFocusListener((FocusableComponent) this, getListenerDispatcher(FocusEvent.class)));
            
            myComponent.addFocusListener(new AWTFocusAdapterAsBlurListener((FocusableComponent) this, getListenerDispatcher(BlurEvent.class)));
        }
    }

    protected static void updateTextForButton(AbstractButton button, LocalizeValue textValue) {
        String text = textValue.getValue();

        MnemonicInfo mnemonicInfo = MnemonicInfo.parse(text);
        if (mnemonicInfo == null) {
            button.setText(text);

            button.setMnemonic(0);
            button.setDisplayedMnemonicIndex(-1);
        }
        else {
            button.setText(mnemonicInfo.getText());
            button.setMnemonic(mnemonicInfo.getKeyCode());
            button.setDisplayedMnemonicIndex(mnemonicInfo.getIndex());
        }
    }

    @Nonnull
    @Override
    public Disposable addClickListener(@Nonnull ComponentEventListener<Component, ClickEvent> clickListener) {
        ClickListener awtClickListener = new ClickListener() {
            @Override
            @RequiredUIAccess
            public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
                clickListener.onEvent(new ClickEvent(SwingComponentDelegate.this, DesktopAWTInputDetails.convert(event)));
                return true;
            }
        };
        awtClickListener.installOn(toAWTComponent());
        return () -> awtClickListener.uninstall(toAWTComponent());
    }

    public boolean hasFocus() {
        return toAWTComponent().hasFocus();
    }

    public boolean isFocusable() {
        return toAWTComponent().isFocusable();
    }

    public void setFocusable(boolean focusable) {
        toAWTComponent().setFocusable(focusable);
    }

    public void focus() {
        toAWTComponent().requestFocus();
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public T toAWTComponent() {
        return myComponent;
    }

    @Override
    public boolean isVisible() {
        return toAWTComponent().isVisible();
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {
        toAWTComponent().setVisible(value);
    }

    @Override
    public boolean isEnabled() {
        return toAWTComponent().isEnabled();
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        toAWTComponent().setEnabled(value);
    }

    @Nullable
    @Override
    public Component getParent() {
        return TargetAWT.from(toAWTComponent().getParent());
    }

    @RequiredUIAccess
    @Override
    public void setSize(@Nonnull Size size) {
        toAWTComponent().setPreferredSize(TargetAWT.to(size));
    }

    @Nonnull
    @Override
    public Font getFont() {
        return new DesktopFontImpl(toAWTComponent().getFont());
    }

    @Override
    public void setFont(@Nonnull Font font) {
        toAWTComponent().setFont(TargetAWT.to(font));
    }

    @Override
    public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
        dataObject().putUserData(key, value);
    }

    @Nullable
    @Override
    public <T> T getUserData(@Nonnull Key<T> key) {
        return dataObject().getUserData(key);
    }

    @Override
    @Nonnull
    public Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
        return dataObject().addUserDataProvider(function);
    }

    @Nonnull
    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(@Nonnull Class<? extends E> eventClass,
                                                                                     @Nonnull ComponentEventListener<C, E> listener) {
        return dataObject().addListener(eventClass, listener);
    }

    @Nonnull
    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(@Nonnull Class<E> eventClass) {
        return dataObject().getDispatcher(eventClass);
    }

    @Override
    @RequiredUIAccess
    public void addBorders(@Nonnull BorderStyle borderStyle, @Nullable ColorValue colorKey, int width) {
        for (BorderPosition position : BorderPosition.values()) {
            dataObject().addBorder(position, borderStyle, colorKey, width);
        }

        bordersChanged();
    }

    @RequiredUIAccess
    @Override
    public void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, ColorValue colorValue, int width) {
        dataObject().addBorder(borderPosition, borderStyle, colorValue, width);

        bordersChanged();
    }

    @RequiredUIAccess
    @Override
    public void removeBorder(@Nonnull BorderPosition borderPosition) {
        dataObject().removeBorder(borderPosition);

        bordersChanged();
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        toAWTComponent().setCursor(TargetAWT.to(cursor));
    }

    @Nullable
    @Override
    public Cursor getCursor() {
        return TargetAWT.from(toAWTComponent().getCursor());
    }

    @Override
    public void forceRepaint() {
        T component = toAWTComponent();
        component.invalidate();
        component.repaint();
    }

    private void bordersChanged() {
        JComponent component = (JComponent) toAWTComponent();

        component.setBorder(JBUI.Borders.empty());

        Map<BorderPosition, BorderInfo> borders = dataObject().getBorders();
        if (borders.isEmpty()) {
            return;
        }

        component.setBorder(new UIComponentBorder(borders));
    }

    @Nonnull
    protected UIDataObject dataObject() {
        javax.swing.JComponent component = (javax.swing.JComponent) toAWTComponent();
        UIDataObject dataObject = (UIDataObject) component.getClientProperty(UIDataObject.class);
        if (dataObject == null) {
            component.putClientProperty(UIDataObject.class, dataObject = new UIDataObject());
        }
        return dataObject;
    }
}
