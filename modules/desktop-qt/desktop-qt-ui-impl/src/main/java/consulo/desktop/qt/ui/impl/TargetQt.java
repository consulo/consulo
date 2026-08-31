/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.ui.impl;

import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import consulo.ui.Window;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.gui.QColor;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class TargetQt {
    private static final Map<QWidget, Component> ourComponents = Collections.synchronizedMap(new WeakHashMap<>());

    public static void register(QWidget widget, Component component) {
        ourComponents.put(widget, component);
    }

    public static void unregister(QWidget widget) {
        ourComponents.remove(widget);
    }

    public static QWidget to(Window window) {
        return ((DesktopQtWindowImpl) window).toQtComponent();
    }

    public static @Nullable Component from(@Nullable QWidget widget) {
        return widget == null ? null : ourComponents.get(widget);
    }

    public static QColor to(ColorValue colorValue) {
        RGBColor rgb = colorValue.toRGB();
        return new QColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), rgb.getAlpha());
    }

    public static @Nullable QColor toNullable(@Nullable ColorValue colorValue) {
        return colorValue == null ? null : to(colorValue);
    }

    public static ColorValue from(QColor color) {
        return new RGBColor(color.red(), color.green(), color.blue(), color.alpha() / 255f);
    }

    public static QSize to(Size2D size) {
        return new QSize(size.width(), size.height());
    }

    public static Size2D from(QSize size) {
        return new Size2D(size.width(), size.height());
    }

    public static QRect to(Rectangle2D rectangle) {
        return new QRect(rectangle.minX(), rectangle.minY(), rectangle.width(), rectangle.height());
    }

    public static Rectangle2D from(QRect rectangle) {
        return new Rectangle2D(rectangle.x(), rectangle.y(), rectangle.width(), rectangle.height());
    }
}
