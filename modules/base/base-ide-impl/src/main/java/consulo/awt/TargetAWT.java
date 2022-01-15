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
package consulo.awt;

import com.intellij.util.ui.JBUI;
import consulo.annotation.ReviewAfterMigrationToJRE;
import consulo.container.StartupError;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.ui.Component;
import consulo.ui.Window;
import consulo.ui.cursor.Cursor;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.Rectangle2D;
import consulo.ui.Size;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 25-Sep-17
 * <p>
 * This should moved to desktop module, after split desktop and platform code
 */
@SuppressWarnings("deprecation")
public class TargetAWT {
  private static final TargetAWTFacade ourFacade = findImplementation(TargetAWTFacade.class);

  @Nonnull
  @ReviewAfterMigrationToJRE(value = 9, description = "Use consulo.container.plugin.util.PlatformServiceLocator#findImplementation after migration")
  private static <T, S> T findImplementation(@Nonnull Class<T> interfaceClass) {
    for (T facade : ServiceLoader.load(interfaceClass, TargetAWT.class.getClassLoader())) {
      return facade;
    }

    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      if (PluginIds.isPlatformImplementationPlugin(descriptor.getPluginId())) {
        ServiceLoader<T> loader = ServiceLoader.load(interfaceClass, descriptor.getPluginClassLoader());

        Iterator<T> iterator = loader.iterator();
        if (iterator.hasNext()) {
          return iterator.next();
        }
      }
    }

    throw new StartupError("Can't find platform implementation: " + interfaceClass);
  }

  @Nonnull
  public static java.awt.Dimension to(@Nonnull Size size) {
    return ourFacade.to(size);
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

  public static java.awt.Image toImage(@Nonnull ImageKey key, JBUI.ScaleContext ctx) {
    return ourFacade.toImage(key, ctx);
  }
}
