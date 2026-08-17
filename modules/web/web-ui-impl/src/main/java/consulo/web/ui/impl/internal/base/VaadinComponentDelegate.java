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
package consulo.web.ui.impl.internal.base;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.Style;
import consulo.application.util.matcher.NameUtilCore;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.AttachEvent;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.DetachEvent;
import consulo.ui.event.KeyPressedEvent;
import consulo.ui.event.KeyReleasedEvent;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.impl.BorderInfo;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import consulo.ui.UIAccess;
import consulo.web.ui.impl.internal.WebColors;
import consulo.web.ui.impl.internal.WebFontImpl;
import consulo.web.ui.impl.internal.WebUIAccessImpl;
import consulo.web.ui.impl.internal.vaadin.AuraUtility;
import consulo.web.ui.impl.internal.vaadin.InitiableComponent;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public abstract class VaadinComponentDelegate<T extends com.vaadin.flow.component.Component & FromVaadinComponentWrapper>
    implements Component, consulo.ui.HasSize, DataObjectHolder, ToVaadinComponentWrapper {
    private T myVaadinComponent;

    private Cursor myCursor;

    private @Nullable ColorValue myBackgroundColor;

    /**
     * The web frontend has one ui per browser session, so the access is the one of the ui this component is
     * attached to - a component that is not attached has none, and whatever wanted to push into it has no ui
     * to push into yet.
     */
    @Override
    public @Nullable UIAccess getUIAccess() {
        return toVaadinComponent().getUI().map(VaadinComponentDelegate::getUIAccess).orElse(null);
    }

    /**
     * The access of a ui, created once and kept on the ui itself - the same instance
     * {@code UIInternal#_UIAccess_get} answers for the ui that is current.
     */
    public static UIAccess getUIAccess(UI ui) {
        WebUIAccessImpl uiAccess = ComponentUtil.getData(ui, WebUIAccessImpl.class);
        if (uiAccess == null) {
            uiAccess = new WebUIAccessImpl(ui);
            ComponentUtil.setData(ui, WebUIAccessImpl.class, uiAccess);
        }
        return uiAccess;
    }

    public VaadinComponentDelegate(boolean noBody) {
    }

    private String myClassNamePrefix;

    private boolean myKeyPressedInstalled;

    /**
     * A component which raises the click itself - a button does - sets this so the dom listener below is not put on
     * top of its own and the press does not arrive twice.
     */
    protected boolean myClickInstalled;
    private boolean myKeyReleasedInstalled;

    public VaadinComponentDelegate() {
        String[] impls = NameUtilCore.splitNameIntoWords(getClass().getSimpleName().replace("Impl", ""));
        myVaadinComponent = createVaadinComponent();

        myClassNamePrefix = String.join("-", impls).toLowerCase(Locale.ROOT);
        myVaadinComponent.addClassName(myClassNamePrefix);
        myVaadinComponent.setId(getClass().getSimpleName() + "." + hashCode());

        // every vaadin small variant - ButtonVariant.SMALL, MenuBarVariant.SMALL, TabsVariant.SMALL and the rest -
        // is this one theme name. the default sizing is meant for touch and is far too big for an ide, and setting
        // it here covers every component instead of every wrapper repeating addThemeVariants
        myVaadinComponent.getElement().getThemeList().add("small");

        if (myVaadinComponent instanceof InitiableComponent initiableComponent) {
            initiableComponent.init(myClassNamePrefix);
        }

        myVaadinComponent.addAttachListener(event -> getListenerDispatcher(AttachEvent.class).onEvent(new AttachEvent(this)));
        myVaadinComponent.addDetachListener(event -> getListenerDispatcher(DetachEvent.class).onEvent(new DetachEvent(this)));
    }

    public String getClassNamePrefix() {
        return myClassNamePrefix;
    }

    public abstract T createVaadinComponent();

    @Override
    public void setToolTipText(LocalizeValue value) {
        Tooltip.forComponent(toVaadinComponent()).setText(value.get());
    }

    protected T getVaadinComponent() {
        return myVaadinComponent;
    }

    @Override
    public T toVaadinComponent() {
        return myVaadinComponent;
    }

    @Override
    public UIDataObject dataObject() {
        UIDataObject data = ComponentUtil.getData(myVaadinComponent, UIDataObject.class);
        if (data == null) {
            ComponentUtil.setData(myVaadinComponent, UIDataObject.class, data = new UIDataObject());
        }
        return data;
    }

    @Override
    public @Nullable Component getParent() {
        Optional<com.vaadin.flow.component.Component> parent = myVaadinComponent.getParent();
        while (parent.isPresent()) {
            com.vaadin.flow.component.Component component = parent.get();
            if (component instanceof UI) {
                return null;
            }

            if (component instanceof FromVaadinComponentWrapper componentWrapper) {
                return componentWrapper.toUIComponent();
            }

            parent = component.getParent();
        }

        return null;
    }

    @RequiredUIAccess
    @Override
    public void setWidth(int widthInPixels) {
        ((HasSize) getVaadinComponent()).setWidth(widthInPixels, Unit.PIXELS);
    }

    @RequiredUIAccess
    @Override
    public void setHeight(int heightInPixels) {
        ((HasSize) getVaadinComponent()).setHeight(heightInPixels, Unit.PIXELS);
    }

    @RequiredUIAccess
    @Override
    public void setMinWidth(int widthInPixels) {
        ((HasSize) getVaadinComponent()).setMinWidth(widthInPixels, Unit.PIXELS);
    }

    @RequiredUIAccess
    @Override
    public void setMinHeight(int heightInPixels) {
        ((HasSize) getVaadinComponent()).setMinHeight(heightInPixels, Unit.PIXELS);
    }

    @Override
    public <T> void putUserData(Key<T> key, @Nullable T value) {
        dataObject().putUserData(key, value);

        // a component that publishes data is exactly the granularity the data context is built at, so it is also
        // the granularity the browser side focus is tracked at
        if (key == UiDataProvider.KEY && value != null) {
            WebFocusTracker.register(this);
        }
    }

    @Override
    public <T> @Nullable T getUserData(Key<T> key) {
        return dataObject().getUserData(key);
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> Disposable addListener(Class<? extends E> eventClass,
                                                                                     ComponentEventListener<C, E> listener) {
        if (eventClass == ClickEvent.class && !myClickInstalled) {
            myClickInstalled = true;

            WebInputDetails.addClickListener(
                toVaadinComponent().getElement(),
                details -> getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this, details))
            );
        }

        if (eventClass == KeyPressedEvent.class && !myKeyPressedInstalled) {
            myKeyPressedInstalled = true;

            keepKeysFromShortcuts();

            WebInputDetails.addKeyListener(
                toVaadinComponent().getElement(),
                "keydown",
                details -> getListenerDispatcher(KeyPressedEvent.class).onEvent(new KeyPressedEvent(this, details))
            );
        }

        if (eventClass == KeyReleasedEvent.class && !myKeyReleasedInstalled) {
            myKeyReleasedInstalled = true;

            keepKeysFromShortcuts();

            WebInputDetails.addKeyListener(
                toVaadinComponent().getElement(),
                "keyup",
                details -> getListenerDispatcher(KeyReleasedEvent.class).onEvent(new KeyReleasedEvent(this, details))
            );
        }

        return dataObject().addListener(eventClass, listener);
    }

    /**
     * {@code shortcuts.js} takes the keys the keymap owns from a document level capture listener, so a stroke
     * bound to an action never reaches the component it was typed in. The terminal marks itself the same way to
     * keep the keys its process reads.
     */
    private void keepKeysFromShortcuts() {
        toVaadinComponent().getElement().setAttribute("consulo-keyboard-capture", "");
    }

    @Override
    public <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(Class<E> eventClass) {
        return dataObject().getDispatcher(eventClass);
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

    @RequiredUIAccess
    @Override
    public void addBorders(BorderStyle borderStyle, @Nullable ColorValue colorKey, int width) {
        for (BorderPosition position : BorderPosition.values()) {
            dataObject().addBorder(position, borderStyle, colorKey, width);
        }

        bordersChanged();
    }

    @Override
    public boolean isVisible() {
        return myVaadinComponent.isVisible();
    }

    @RequiredUIAccess
    @Override
    public void setVisible(boolean value) {
        myVaadinComponent.setVisible(value);
    }

    @Override
    public boolean isEnabled() {
        return myVaadinComponent.getElement().isEnabled();
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        // HasEnabled is a mixin over the element's own state, and not every component carries it - a progress
        // bar has none, and asking one for it used to end the request rather than the call
        myVaadinComponent.getElement().setEnabled(value);
    }

    @Override
    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    @Override
    public void setBackgroundColor(@Nullable ColorValue background) {
        myBackgroundColor = background;

        Style style = myVaadinComponent.getStyle();
        String cssColor = WebColors.toCssColor(background);
        if (cssColor == null) {
            style.remove("background-color");
        }
        else {
            style.set("background-color", cssColor);
        }
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        myCursor = cursor;
        // TODO CursorConverter.setCursor(toVaadinComponent(), cursor);
    }

    @Override
    public @Nullable Cursor getCursor() {
        return myCursor;
    }

    public boolean hasFocus() {
        return true;
    }

    public void focus() {
        if (myVaadinComponent instanceof Focusable<?> focusable) {
            focusable.focus();
        }
        else {
            myVaadinComponent.getElement().callJsFunction("focus");
        }
    }

    public void setFocusable(boolean focusable) {

    }

    public boolean isFocusable() {
        return true;
    }

    public void bordersChanged() {
        Map<BorderPosition, BorderInfo> borders = dataObject().getBorders();

        if (!borders.isEmpty()) {
            myVaadinComponent.getStyle().set("box-sizing", "border-box");
        }

        if (!borders.isEmpty() && myVaadinComponent instanceof com.vaadin.flow.component.shared.InputField) {
            // swing answers the same borders by replacing the one the look and feel gave the component, and its
            // insets are counted inside the size. a vaadin field keeps its own border in the shadow dom, where a
            // class on the host cannot reach it, so it is turned off through the properties it reads
            Style style = myVaadinComponent.getStyle();
            style.set("--vaadin-input-field-border-width", "0");
            style.set("--vaadin-input-field-border-radius", "0");
            style.set("--vaadin-input-field-background", "transparent");

            // the shadow under the field is written into the theme rather than read from a property, so it takes
            // a rule reaching the part itself
            myVaadinComponent.getElement().getClassList().add("consulo-flat-input");
        }

        applyBorder(BorderPosition.TOP, borders);
        applyBorder(BorderPosition.BOTTOM, borders);
        applyBorder(BorderPosition.RIGHT, borders);
        applyBorder(BorderPosition.LEFT, borders);
    }

    private void applyBorder(BorderPosition pos, Map<BorderPosition, BorderInfo> borders) {
        BorderInfo info = borders.get(pos);
        if (info == null) {
            return;
        }

        ClassList classList = myVaadinComponent.getElement().getClassList();

        switch (info.getBorderStyle()) {
            case LINE_ROUNDED:
            case LINE: {
                classList.add(AuraUtility.BorderColor.CONTRAST_10); // TODO support color

                if (info.getBorderStyle() == BorderStyle.LINE_ROUNDED) {
                    myVaadinComponent.getStyle().set("border-radius", BorderStyle.DEFAULT_ARC + "px");
                }

                switch (info.getBorderPosition()) {
                    case TOP:
                        classList.add(AuraUtility.Border.TOP);
                        break;
                    case BOTTOM:
                        classList.add(AuraUtility.Border.BOTTOM);
                        break;
                    case LEFT:
                        classList.add(AuraUtility.Border.LEFT);
                        break;
                    case RIGHT:
                        classList.add(AuraUtility.Border.RIGHT);
                        break;
                }
                break;
            }
            case EMPTY: {
                String padding = info.getWidth() + "px";

                switch (info.getBorderPosition()) {
                    case TOP:
                        myVaadinComponent.getStyle().set("padding-top", padding);
                        break;
                    case BOTTOM:
                        myVaadinComponent.getStyle().set("padding-bottom", padding);
                        break;
                    case LEFT:
                        myVaadinComponent.getStyle().set("padding-left", padding);
                        break;
                    case RIGHT:
                        myVaadinComponent.getStyle().set("padding-right", padding);
                        break;
                }
                break;
            }
        }
    }
}
