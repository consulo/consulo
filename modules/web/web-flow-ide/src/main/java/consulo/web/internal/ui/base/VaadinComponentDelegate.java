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
package consulo.web.internal.ui.base;

import com.vaadin.flow.component.*;
import consulo.application.util.matcher.NameUtilCore;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.AttachEvent;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.DetachEvent;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;
import consulo.web.internal.ui.WebFontImpl;
import consulo.web.internal.ui.vaadin.InitiableComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public abstract class VaadinComponentDelegate<T extends com.vaadin.flow.component.Component & FromVaadinComponentWrapper> implements Component, DataObjectHolder, ToVaddinComponentWrapper {
    private T myVaadinComponent;

    private Font myFont = FontManager.get().createFont("?", 12);

    private Cursor myCursor;

    public VaadinComponentDelegate(boolean noBody) {
    }

    private String myClassNamePrefix;

    public VaadinComponentDelegate() {
        String[] impls = NameUtilCore.splitNameIntoWords(getClass().getSimpleName().replace("Impl", ""));
        myVaadinComponent = createVaadinComponent();

        myClassNamePrefix = String.join("-", impls).toLowerCase(Locale.ROOT);
        myVaadinComponent.addClassName(myClassNamePrefix);
        myVaadinComponent.setId(getClass().getSimpleName() + "." + hashCode());

        if (myVaadinComponent instanceof InitiableComponent initiableComponent) {
            initiableComponent.init(myClassNamePrefix);
        }

        myVaadinComponent.addAttachListener(event -> getListenerDispatcher(AttachEvent.class).onEvent(new AttachEvent(this)));
        myVaadinComponent.addDetachListener(event -> getListenerDispatcher(DetachEvent.class).onEvent(new DetachEvent(this)));
    }

    public String getClassNamePrefix() {
        return myClassNamePrefix;
    }

    @Nonnull
    public abstract T createVaadinComponent();

    @Override
    public void setFont(@Nonnull Font font) {
        if (!(font instanceof WebFontImpl)) {
            throw new IllegalArgumentException("not web font");
        }

        myFont = font;
    }

    @Nonnull
    @Override
    public Font getFont() {
        return myFont;
    }

    @Nonnull
    protected T getVaadinComponent() {
        return myVaadinComponent;
    }

    @Nonnull
    @Override
    public T toVaadinComponent() {
        return myVaadinComponent;
    }

    @Override
    @Nonnull
    public UIDataObject dataObject() {
        UIDataObject data = ComponentUtil.getData(myVaadinComponent, UIDataObject.class);
        if (data == null) {
            ComponentUtil.setData(myVaadinComponent, UIDataObject.class, data = new UIDataObject());
        }
        return data;
    }

    @Nullable
    @Override
    public Component getParent() {
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
    public void setSize(@Nonnull Size size) {
        HasSize vaadinComponent = (HasSize) getVaadinComponent();
        if (size.getHeight() == -1) {
            vaadinComponent.setHeight(null);
        }
        else {
            vaadinComponent.setHeight(size.getHeight(), Unit.PIXELS);
        }

        if (size.getWidth() == -1) {
            vaadinComponent.setWidth(null);
        }
        else {
            vaadinComponent.setWidth(size.getWidth(), Unit.PIXELS);
        }
        // TODO vaadinComponent.markAsDirty();
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

    @RequiredUIAccess
    @Override
    public void addBorder(@Nonnull BorderPosition borderPosition, BorderStyle borderStyle, ColorValue colorValue, int width) {
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
        return ((HasEnabled) myVaadinComponent).isEnabled();
    }

    @RequiredUIAccess
    @Override
    public void setEnabled(boolean value) {
        ((HasEnabled) myVaadinComponent).setEnabled(value);
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
        myCursor = cursor;
        // TODO CursorConverter.setCursor(toVaadinComponent(), cursor);
    }

    @Nullable
    @Override
    public Cursor getCursor() {
        return myCursor;
    }

    public boolean hasFocus() {
        return true;
    }

    public void focus() {

    }

    public void setFocusable(boolean focusable) {

    }

    public boolean isFocusable() {
        return true;
    }

    public void bordersChanged() {
    }
}
