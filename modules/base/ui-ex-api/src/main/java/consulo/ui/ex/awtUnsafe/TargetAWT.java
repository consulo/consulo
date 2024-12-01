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

import consulo.container.plugin.util.PlatformServiceLoader;
import consulo.ui.Component;
import consulo.ui.Window;
import consulo.ui.*;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.cursor.Cursor;
import consulo.ui.ex.awtUnsafe.internal.TargetAWTFacade;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import javax.swing.*;
import java.awt.*;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 25-Sep-17
 * <p>
 * This should moved to desktop module, after split desktop and platform code.
 * This class not provide any components just converting data
 */
@SuppressWarnings("deprecation")
public final class TargetAWT {
    private static final TargetAWTFacade ourFacade = PlatformServiceLoader.findImplementation(TargetAWTFacade.class, ServiceLoader::load);

    @Contract("null -> null")
    public static java.awt.Dimension to(@Nullable Size size) {
        return size == null ? null : new Dimension(size.getWidth(), size.getHeight());
    }

    @Contract("null -> null")
    public static java.awt.Point to(@Nullable Coordinate2D coordinate2D) {
        return coordinate2D == null ? null : new Point(coordinate2D.getX(), coordinate2D.getY());
    }

    @Nonnull
    public static java.awt.Color to(@Nonnull RGBColor color) {
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
    @Nonnull
    public static Component wrap(@Nonnull java.awt.Component component) {
        return ourFacade.wrap(component);
    }

    @Nonnull
    public static Image wrap(@Nonnull Icon icon) {
        return ourFacade.wrap(icon);
    }

    @Contract("null -> null")
    public static Component from(@Nullable java.awt.Component component) {
        return ourFacade.from(component);
    }

    @Contract("null -> null")
    public static Window from(@Nullable java.awt.Window component) {
        return ourFacade.from(component);
    }

    @Contract("null -> null")
    public static Rectangle2D from(@Nullable java.awt.Rectangle rectangle) {
        return ourFacade.from(rectangle);
    }

    @Contract("null -> null")
    public static Size from(@Nullable Dimension dimension) {
        return dimension == null ? null : new Size(dimension.width, dimension.height);
    }

    @Contract("null -> null")
    public static Coordinate2D from(@Nullable Point point) {
        return point == null ? null : new Coordinate2D(point.x, point.y);
    }

    @Contract("null -> null")
    public static ColorValue from(@Nullable java.awt.Color color) {
        return ourFacade.from(color);
    }

    @Contract("null -> null")
    public static java.awt.Cursor to(Cursor cursor) {
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

    @Nonnull
    public static java.awt.Font to(@Nonnull Font font) {
        return ourFacade.to(font);
    }

    public static java.awt.Image toAWTImage(@Nonnull Image image) {
        return ourFacade.toAWTImage(image);
    }
}
