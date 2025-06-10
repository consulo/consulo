/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.*;
import consulo.ui.font.Font;
import consulo.ui.style.ComponentColors;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2016-01-09
 */
public interface Component extends UserDataHolder {
    Key<Component> KEY = Key.of(Component.class);

    @RequiredUIAccess
    default void addBorder(@Nonnull BorderPosition borderPosition) {
        addBorder(borderPosition, BorderStyle.LINE, ComponentColors.BORDER, 1);
    }

    @RequiredUIAccess
    default void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle) {
        addBorder(borderPosition, borderStyle, ComponentColors.BORDER, 1);
    }

    @RequiredUIAccess
    default void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, @Nullable ColorValue colorKey) {
        addBorder(borderPosition, borderStyle, colorKey, 1);
    }

    @RequiredUIAccess
    default void addBorders(@Nonnull BorderStyle borderStyle, @Nullable ColorValue colorKey, int width) {
        for (BorderPosition position : BorderPosition.values()) {
            addBorder(position, borderStyle, colorKey, width);
        }
    }

    @RequiredUIAccess
    default void addDefaultBorders() {
        for (BorderPosition position : BorderPosition.values()) {
            addBorder(position);
        }
    }

    @RequiredUIAccess
    default void addMirrorBorders(@Nonnull BorderStyle borderStyle, @Nullable ColorValue colorValue, int topBottom, int leftRight) {
        if (topBottom > 0) {
            addBorder(BorderPosition.TOP, borderStyle, colorValue, topBottom);
            addBorder(BorderPosition.BOTTOM, borderStyle, colorValue, topBottom);
        }
        if (leftRight > 0) {
            addBorder(BorderPosition.LEFT, borderStyle, colorValue, leftRight);
            addBorder(BorderPosition.RIGHT, borderStyle, colorValue, leftRight);
        }
    }

    @RequiredUIAccess
    default void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, int width) {
        addBorder(borderPosition, borderStyle, null, width);
    }

    @RequiredUIAccess
    void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, @Nullable ColorValue colorValue, int width);

    @RequiredUIAccess
    default void removeBorders() {
        for (BorderPosition position : BorderPosition.values()) {
            removeBorder(position);
        }
    }

    @RequiredUIAccess
    void removeBorder(@Nonnull BorderPosition borderPosition);

    boolean isVisible();

    @RequiredUIAccess
    void setVisible(boolean value);

    @RequiredUIAccess
    @Nonnull
    default Component withVisible(boolean visible) {
        setVisible(visible);
        return this;
    }

    boolean isEnabled();

    @RequiredUIAccess
    void setEnabled(boolean value);

    @RequiredUIAccess
    default void setEnabledRecursive(boolean value) {
        setEnabled(value);
    }

    @RequiredUIAccess
    default Component withEnabled(boolean enabled) {
        setEnabled(enabled);
        return this;
    }

    @Nullable
    Component getParent();

    @RequiredUIAccess
    void setSize(@Nonnull Size2D size);

    @Nonnull
    @RequiredUIAccess
    default Component withSize(@Nonnull Size2D size) {
        setSize(size);
        return this;
    }

    @Nonnull
    default <T> Disposable addUserDataProvider(@Nonnull Key<T> key, @Nonnull Supplier<T> supplier) {
        return addUserDataProvider(k -> k == key ? supplier.get() : null);
    }

    @Nonnull
    Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function);

    @Nonnull
    Font getFont();

    void setFont(@Nonnull Font font);

    @Nonnull
    @RequiredUIAccess
    default Component withFont(@Nonnull Font font) {
        setFont(font);
        return this;
    }

    @Nullable
    default ColorValue getForegroundColor() {
        throw new AbstractMethodError("not supported");
    }

    default void setForegroundColor(@Nullable ColorValue foreground) {
        throw new AbstractMethodError("not supported");
    }

    @Nonnull
    default Component withForegroundColor(@Nullable ColorValue foreground) {
        setForegroundColor(foreground);
        return this;
    }

    @Nullable
    default Cursor getCursor() {
        throw new AbstractMethodError("not supported");
    }

    default void setCursor(@Nullable Cursor cursor) {
        throw new AbstractMethodError("not supported");
    }

    @Nonnull
    default Component withCursor(@Nullable Cursor cursor) {
        setCursor(cursor);
        return this;
    }

    default void setToolTipText(@Nonnull LocalizeValue value) {
        throw new AbstractMethodError("not supported");
    }

    @Nonnull
    default LocalizeValue getToolTipText() {
        return LocalizeValue.empty();
    }

    @Nonnull
    default Component withToolTipText(@Nonnull LocalizeValue value) {
        setToolTipText(value);
        return this;
    }

    /**
     * @return runner for unregister listener
     */
    @Nonnull
    <C extends Component, E extends ComponentEvent<C>> Disposable addListener(
        @Nonnull Class<? extends E> eventClass,
        @Nonnull ComponentEventListener<C, E> listener
    );

    @Nonnull
    <C extends Component, E extends ComponentEvent<C>> ComponentEventListener<C, E> getListenerDispatcher(@Nonnull Class<E> eventClass);

    @Nonnull
    default Disposable addKeyPressedListener(@Nonnull ComponentEventListener<Component, KeyPressedEvent> keyListener) {
        return addListener(KeyPressedEvent.class, keyListener);
    }

    @Nonnull
    default Disposable addKeyReleasedListener(@Nonnull ComponentEventListener<Component, KeyReleasedEvent> keyListener) {
        return addListener(KeyReleasedEvent.class, keyListener);
    }

    @Nonnull
    default Disposable addAttachListener(@Nonnull ComponentEventListener<Component, AttachEvent> attachListener) {
        return addListener(AttachEvent.class, attachListener);
    }

    @Nonnull
    default Disposable addDetachListener(@Nonnull ComponentEventListener<Component, DetachEvent> detachListener) {
        return addListener(DetachEvent.class, detachListener);
    }

    @Nonnull
    default Disposable addClickListener(@Nonnull ComponentEventListener<Component, ClickEvent> clickListener) {
        return addListener(ClickEvent.class, clickListener);
    }

    default void forceRepaint() {
    }
}
