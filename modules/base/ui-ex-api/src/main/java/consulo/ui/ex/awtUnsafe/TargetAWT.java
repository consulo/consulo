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
package consulo.ui.ex.awtUnsafe;

import com.uber.nullaway.annotations.Contract;
import consulo.container.plugin.util.PlatformServiceLoader;
import consulo.ui.Component;
import consulo.ui.Window;
import consulo.ui.*;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2017-09-25
 * <p>
 * This should moved to desktop module, after split desktop and platform code.
 * This class not provide any components just converting data
 */
@SuppressWarnings("deprecation")
public final class TargetAWT {
    private static final TargetAWTFacade ourFacade = PlatformServiceLoader.findImplementation(TargetAWTFacade.class, ServiceLoader::load);

    @Contract("null -> null")
    public static java.awt.Dimension to(@Nullable Size2D size) {
        return size == null ? null : new Dimension(size.width(), size.height());
    }

    @Contract("null -> null")
    public static Point to(@Nullable Point2D point2D) {
        return point2D == null ? null : new Point(point2D.x(), point2D.y());
    }

    public static java.awt.Color to(RGBColor color) {
        return ourFacade.to(color);
    }

    @Contract("null -> null")
    public static java.awt.Color to(@Nullable ColorValue colorValue) {
        return ourFacade.to(colorValue);
    }

    @Contract("null -> null")
    public static java.awt.Rectangle to(@Nullable Rectangle2D rectangle2D) {
        return ourFacade.to(rectangle2D);
    }

    @Contract("null -> null")
    public static java.awt.Component to(@Nullable Component component) {
        return ourFacade.to(component);
    }

    @Contract("null -> null")
    public static java.awt.Window to(@Nullable Window component) {
        return ourFacade.to(component);
    }

    /**
     * Wrap AWT component to untyped UI component. Calling any methods from it except #from() not supported
     */
    public static Component wrap(java.awt.Component component) {
        return ourFacade.wrap(component);
    }

    public static Image wrap(Icon icon) {
        return ourFacade.wrap(icon);
    }

    public static ComponentEvent<?> from(java.awt.event.MouseEvent event) {
        return ourFacade.from(event);
    }

    @Contract("null -> null")
    public static Component from(java.awt.@Nullable Component component) {
        return ourFacade.from(component);
    }

    @Contract("null -> null")
    public static Window from(java.awt.@Nullable Window component) {
        return ourFacade.from(component);
    }

    @Contract("null -> null")
    public static Rectangle2D from(java.awt.@Nullable Rectangle rectangle) {
        return ourFacade.from(rectangle);
    }

    @Contract("null -> null")
    public static Size2D from(@Nullable Dimension dimension) {
        return dimension == null ? null : new Size2D(dimension.width, dimension.height);
    }

    @Contract("null -> null")
    public static Point2D from(@Nullable Point point) {
        return point == null ? null : new Point2D(point.x, point.y);
    }

    @Contract("null -> null")
    public static ColorValue from(java.awt.@Nullable Color color) {
        return ourFacade.from(color);
    }

    @Contract("null -> null")
    public static java.awt.@Nullable Cursor to(@Nullable Cursor cursor) {
        return ourFacade.to(cursor);
    }

    @Contract("null -> null")
    public static Cursor from(java.awt.Cursor cursor) {
        return ourFacade.from(cursor);
    }

    @Contract("null -> null")
    public static Icon to(@Nullable Image image) {
        return ourFacade.to(image);
    }

    @Contract("null -> null")
    public static Image from(@Nullable Icon icon) {
        return ourFacade.from(icon);
    }

    public static java.awt.Font to(Font font) {
        return ourFacade.to(font);
    }

    public static java.awt.Image toAWTImage(Image image) {
        return ourFacade.toAWTImage(image);
    }

    @Contract("null -> null")
    public static javax.swing.@Nullable KeyStroke to(consulo.ui.event.details.@Nullable KeyStroke keyStroke) {
        if (keyStroke == null) {
            return null;
        }
        int modifiers = toModifiers(keyStroke.getModifiers());
        if (keyStroke.isTyped()) {
            return javax.swing.KeyStroke.getKeyStroke(Character.valueOf(keyStroke.getKeyChar()), modifiers);
        }
        return javax.swing.KeyStroke.getKeyStroke(keyStroke.getKeyCode().key(), modifiers, keyStroke.isOnKeyRelease());
    }

    /**
     * @return the key stroke, {@code null} for one with neither a key nor a character
     */
    @Contract("null -> null")
    public static consulo.ui.event.details.@Nullable KeyStroke from(javax.swing.@Nullable KeyStroke keyStroke) {
        if (keyStroke == null) {
            return null;
        }
        Set<ModifiedInputDetails.Modifier> modifiers = fromModifiers(keyStroke.getModifiers());

        int keyCode = keyStroke.getKeyCode();
        if (keyCode != java.awt.event.KeyEvent.VK_UNDEFINED) {
            return consulo.ui.event.details.KeyStroke.of(KeyCode.of(keyCode), modifiers, keyStroke.isOnKeyRelease());
        }

        char keyChar = keyStroke.getKeyChar();
        if (keyChar != java.awt.event.KeyEvent.CHAR_UNDEFINED) {
            return consulo.ui.event.details.KeyStroke.typed(keyChar, modifiers);
        }
        return null;
    }

    /**
     * @return the {@code *_DOWN_MASK} modifiers of an input event
     */
    public static int toModifiers(Set<ModifiedInputDetails.Modifier> modifiers) {
        int mask = 0;
        for (ModifiedInputDetails.Modifier modifier : modifiers) {
            mask |= switch (modifier) {
                case SHIFT -> java.awt.event.InputEvent.SHIFT_DOWN_MASK;
                case CTRL -> java.awt.event.InputEvent.CTRL_DOWN_MASK;
                case META -> java.awt.event.InputEvent.META_DOWN_MASK;
                case ALT -> java.awt.event.InputEvent.ALT_DOWN_MASK;
                case ALT_GRAPH -> java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK;
            };
        }
        return mask;
    }

    /**
     * @param mask the modifiers of an input event, the old masks and the {@code *_DOWN_MASK} ones alike
     */
    public static Set<ModifiedInputDetails.Modifier> fromModifiers(int mask) {
        Set<ModifiedInputDetails.Modifier> modifiers = EnumSet.noneOf(ModifiedInputDetails.Modifier.class);
        addIf(modifiers, mask, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.SHIFT_MASK,
            ModifiedInputDetails.Modifier.SHIFT);
        addIf(modifiers, mask, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.CTRL_MASK,
            ModifiedInputDetails.Modifier.CTRL);
        addIf(modifiers, mask, java.awt.event.InputEvent.META_DOWN_MASK | java.awt.event.InputEvent.META_MASK,
            ModifiedInputDetails.Modifier.META);
        addIf(modifiers, mask, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.ALT_MASK,
            ModifiedInputDetails.Modifier.ALT);
        addIf(modifiers, mask, java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK | java.awt.event.InputEvent.ALT_GRAPH_MASK,
            ModifiedInputDetails.Modifier.ALT_GRAPH);
        return modifiers;
    }

    /**
     * @return the button of a {@link consulo.ui.ex.action.MouseShortcut} the event is a click of, a wheel turn included
     */
    public static int toMouseShortcutButton(java.awt.event.MouseEvent event) {
        if (event instanceof java.awt.event.MouseWheelEvent wheel) {
            return 0 < wheel.getWheelRotation()
                ? consulo.ui.ex.action.MouseShortcut.BUTTON_WHEEL_DOWN
                : consulo.ui.ex.action.MouseShortcut.BUTTON_WHEEL_UP;
        }
        return event.getButton();
    }

    private static void addIf(Set<ModifiedInputDetails.Modifier> set, int mask, int bits, ModifiedInputDetails.Modifier modifier) {
        if ((mask & bits) != 0) {
            set.add(modifier);
        }
    }
}
