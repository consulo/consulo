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
import consulo.ui.HasFocus;
import consulo.ui.Size2D;
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
import consulo.ui.util.TextWithMnemonic;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
public abstract class SwingComponentDelegate<T extends java.awt.Component> implements Component, ToSwingComponentWrapper {
    private T myInitializedComponent;

    protected abstract T createComponent();

    protected void init(T component) {
        component.addKeyListener(new AWTKeyAdapterAsKeyPressedListener(this, getListenerDispatcher(KeyPressedEvent.class)));
        component.addKeyListener(new AWTKeyAdapterAsKeyReleasedListener(this, getListenerDispatcher(KeyReleasedEvent.class)));

        if (this instanceof HasFocus) {
            component.addFocusListener(new AWTFocusAdapterAsFocusListener((HasFocus) this, getListenerDispatcher(FocusEvent.class)));

            component.addFocusListener(new AWTFocusAdapterAsBlurListener((HasFocus) this, getListenerDispatcher(BlurEvent.class)));
        }
    }

    protected static void updateTextForButton(AbstractButton button, LocalizeValue textValue) {
        String text = textValue.getValue();

        TextWithMnemonic mnemonicInfo = TextWithMnemonic.parse(text);
        if (!mnemonicInfo.hasMnemonic()) {
            button.setText(text);

            button.setMnemonic(0);
            button.setDisplayedMnemonicIndex(-1);
        }
        else {
            button.setText(mnemonicInfo.getText());
            button.setMnemonic(mnemonicInfo.getMnemonic());
            button.setDisplayedMnemonicIndex(mnemonicInfo.getMnemonicIndex());
        }
    }

    public boolean isInitialized() {
        return myInitializedComponent != null;
    }

    @Override
    public Disposable addClickListener(ComponentEventListener<Component, ClickEvent> clickListener) {
        ClickListener awtClickListener = new ClickListener() {
            @Override
            @RequiredUIAccess
            public boolean onClick(MouseEvent event, int clickCount) {
                clickListener.onEvent(new ClickEvent(SwingComponentDelegate.this, DesktopAWTInputDetails.convert(event.getComponent(), event)));
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

    @Override
    public LocalizeValue getToolTipText() {
        JComponent component = (JComponent) toAWTComponent();
        return LocalizeValue.ofNullable(component.getToolTipText());
    }

    @Override
    public void setToolTipText(LocalizeValue value) {
        JComponent component = (JComponent) toAWTComponent();
        component.setToolTipText(StringUtil.nullize(value.get()));
    }

    @Override
    public T toAWTComponent() {
        if (myInitializedComponent == null) {
            T component = createComponent();
            myInitializedComponent = component;
            init(component);
        }
        return myInitializedComponent;
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
    public void setSize(Size2D size) {
        toAWTComponent().setPreferredSize(TargetAWT.to(size));
    }

    @Override
    public Font getFont() {
        return new DesktopFontImpl(toAWTComponent().getFont());
    }

    @Override
    public void setFont(Font font) {
        toAWTComponent().setFont(TargetAWT.to(font));
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        dataObject().putUserData(key, value);
    }

    @Nullable
    @Override
    public <T> T getUserData(Key<T> key) {
        return dataObject().getUserData(key);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(Class<? extends E> eventClass,
                                                                                     ComponentEventListener<C, E> listener) {
        return dataObject().addListener(eventClass, listener);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        return dataObject().getDispatcher(eventClass);
    }

    @Override
    @RequiredUIAccess
    public void addBorders(BorderStyle borderStyle, @Nullable ColorValue colorKey, int width) {
        for (BorderPosition position : BorderPosition.values()) {
            dataObject().addBorder(position, borderStyle, colorKey, width);
        }

        bordersChanged();
    }

    @RequiredUIAccess
    @Override
    public void addBorder(BorderPosition borderPosition, BorderStyle borderStyle, ColorValue colorValue, int width) {
        dataObject().addBorder(borderPosition, borderStyle, colorValue, width);

        bordersChanged();
    }

    @RequiredUIAccess
    @Override
    public void removeBorder(BorderPosition borderPosition) {
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

    protected UIDataObject dataObject() {
        JComponent component = (JComponent) toAWTComponent();
        UIDataObject dataObject = (UIDataObject) component.getClientProperty(UIDataObject.class);
        if (dataObject == null) {
            component.putClientProperty(UIDataObject.class, dataObject = new UIDataObject());
        }
        return dataObject;
    }
}
