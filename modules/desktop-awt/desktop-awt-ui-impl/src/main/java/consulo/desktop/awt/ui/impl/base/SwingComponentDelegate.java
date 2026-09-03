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

import consulo.desktop.awt.ui.impl.AWTUIAccessImpl;
import consulo.desktop.awt.ui.impl.DesktopFontImpl;
import consulo.desktop.awt.ui.impl.DesktopLength;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.ToSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.util.AWTFocusAdapterAsBlurListener;
import consulo.desktop.awt.ui.impl.util.AWTFocusAdapterAsFocusListener;
import consulo.desktop.awt.ui.impl.util.AWTKeyAdapterAsKeyPressedListener;
import consulo.desktop.awt.ui.impl.util.AWTKeyAdapterAsKeyReleasedListener;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.BorderBuilder;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.HasSize;
import consulo.ui.Length;
import consulo.ui.PaddingBuilder;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.*;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.font.Font;
import consulo.ui.impl.BorderBuilderImpl;
import consulo.ui.impl.PaddingBuilderImpl;
import consulo.ui.impl.UIDataObject;
import consulo.ui.internal.BorderPosition;
import consulo.ui.util.TextWithMnemonic;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
public abstract class SwingComponentDelegate<T extends java.awt.Component> implements Component, HasSize, ToSwingComponentWrapper {
    private T myInitializedComponent;

    private @Nullable ColorValue myBackgroundColor;

    private boolean myClickBridgeInstalled;
    private boolean myContextMenuInstalled;

    /** the desktop frontend draws into a single ui, so every component of it answers the same access */
    @Override
    public UIAccess getUIAccess() {
        return AWTUIAccessImpl.ourInstance;
    }

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
        if (!myClickBridgeInstalled) {
            myClickBridgeInstalled = true;

            new ClickListener() {
                @Override
                @RequiredUIAccess
                public boolean onClick(MouseEvent event, int clickCount) {
                    getListenerDispatcher(ClickEvent.class)
                        .onEvent(new ClickEvent(SwingComponentDelegate.this, DesktopAWTInputDetails.convert(event.getComponent(), event)));
                    return true;
                }
            }.installOn(toAWTComponent());
        }

        return dataObject().addListener(ClickEvent.class, clickListener);
    }

    @RequiredUIAccess
    public void setAccessibleName(LocalizeValue name) {
        AccessibleContext context = toAWTComponent().getAccessibleContext();
        if (context != null) {
            context.setAccessibleName(name.get());
        }
    }

    @RequiredUIAccess
    public void setAccessibleDescription(LocalizeValue description) {
        AccessibleContext context = toAWTComponent().getAccessibleContext();
        if (context != null) {
            context.setAccessibleDescription(description.get());
        }
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

    @Override
    public @Nullable Component getParent() {
        return TargetAWT.from(toAWTComponent().getParent());
    }

    @RequiredUIAccess
    @Override
    public void setWidth(Length width) {
        T component = toAWTComponent();
        Dimension size = component.getPreferredSize();
        component.setPreferredSize(new Dimension(DesktopLength.toPixels(component, width), size.height));
    }

    @RequiredUIAccess
    @Override
    public void setHeight(Length height) {
        T component = toAWTComponent();
        Dimension size = component.getPreferredSize();
        component.setPreferredSize(new Dimension(size.width, DesktopLength.toPixels(component, height)));
    }

    @RequiredUIAccess
    @Override
    public void setMinWidth(Length width) {
        T component = toAWTComponent();
        Dimension size = component.getMinimumSize();
        component.setMinimumSize(new Dimension(DesktopLength.toPixels(component, width), size.height));
    }

    @RequiredUIAccess
    @Override
    public void setMinHeight(Length height) {
        T component = toAWTComponent();
        Dimension size = component.getMinimumSize();
        component.setMinimumSize(new Dimension(size.width, DesktopLength.toPixels(component, height)));
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        dataObject().putUserData(key, value);
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        return dataObject().getUserData(key);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(Class<? extends E> eventClass,
                                                                                     ComponentEventListener<C, E> listener) {
        if (eventClass == ContextMenuEvent.class) {
            installContextMenuDispatch();
        }

        return dataObject().addListener(eventClass, listener);
    }

    private void installContextMenuDispatch() {
        if (myContextMenuInstalled) {
            return;
        }

        myContextMenuInstalled = true;

        toAWTComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                fireIfPopupTrigger(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                fireIfPopupTrigger(e);
            }

            private void fireIfPopupTrigger(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }

                e.consume();

                getListenerDispatcher(ContextMenuEvent.class).onEvent(
                    new ContextMenuEvent(SwingComponentDelegate.this, DesktopAWTInputDetails.convert(e.getComponent(), e))
                );
            }
        });
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        return dataObject().getDispatcher(eventClass);
    }

    @RequiredUIAccess
    @Override
    public BorderBuilder borderBuilder() {
        return new BorderBuilderImpl(dataObject(), this::bordersChanged);
    }

    @RequiredUIAccess
    @Override
    public PaddingBuilder paddingBuilder() {
        return new PaddingBuilderImpl(dataObject(), this::bordersChanged);
    }


    @Override
    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    @Override
    public void setBackgroundColor(@Nullable ColorValue background) {
        myBackgroundColor = background;

        T component = toAWTComponent();
        if (background == null) {
            component.setBackground(null);
        }
        else {
            component.setBackground(TargetAWT.to(background));
            if (component instanceof JComponent jComponent) {
                jComponent.setOpaque(true);
            }
        }
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        toAWTComponent().setCursor(TargetAWT.to(cursor));
    }

    @Override
    public @Nullable Cursor getCursor() {
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

        Map<BorderPosition, ColorValue> borders = dataObject().getBorders();
        Map<BorderPosition, Space> paddings = dataObject().getPaddings();
        if (borders.isEmpty() && paddings.isEmpty()) {
            return;
        }

        component.setBorder(new UIComponentBorder(borders, paddings));
    }

    protected UIDataObject dataObject() {
        JComponent component = (JComponent) toAWTComponent();
        UIDataObject dataObject = (UIDataObject) component.getClientProperty(UIDataObject.class);
        if (dataObject == null) {
            component.putClientProperty(UIDataObject.class, dataObject = new UIDataObject());
        }
        return dataObject;
    }

    public boolean hasListeners(Class<? extends ComponentEvent<?>> eventClass) {
        return dataObject().hasListeners(eventClass);
    }
}
